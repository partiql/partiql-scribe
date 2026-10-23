# PartiQL Scribe Redshift Frontend

Translates Amazon Redshift queries to PartiQL. Scribe targets translate PartiQL to other dialects; this module goes
the other way for Redshift, and reuses the PartiQL planner and the Scribe PartiQL target to do it.

```kotlin
val result = RedshiftTranspiler().transpile("SELECT TOP 5 c AS v FROM t", session)
result.status   // VALID
result.partiql  // SELECT "t"['c'] AS "v" FROM "default"."T" AS "t" LIMIT 5
result.problems // a TRANSLATION_INFO note for TOP, with line and column
```

`RedshiftTranspiler.tables(sql)` lists the tables a query reads, so that a caller can fetch their schemas and build
the session: each table needs a schema that is a bag of rows with the column types.

## Pipeline

1. Parse the Redshift text with `src/main/antlr/Redshift.g4`.
2. Build a PartiQL AST from the parse tree. Dialect differences are rewritten here: queries and clauses in
   `RedshiftAstBuilder`, expressions in `RedshiftExpressions`, and function calls in `RedshiftFunctions`. This part
   has the query structure, names, literals, SUPER paths, and window functions; the rules for comparisons and the
   other predicates, operators, casts, datetime constructs, and the function categories follow, and are reported as
   unsupported until then.
3. A rule whose Redshift semantics depend on the static types of its operands (for example `AVG` of an integer, or
   `LEN` of a `CHAR(n)` value) gets the types from a probe pass (`OperandTypes`). In the probe pass, such a rule
   translates to a call of the Redshift catalog: a hidden function whose arguments are the operands of the rule and
   whose result type is the type of the rule's translation for the argument types, which the catalog computes by
   running the rule with columns of those types and planning the translation. The planner plans the probe AST against
   the session and the Redshift catalog, so the probe plan is the Redshift query typed as Redshift types it (`AVG` of
   an integer is `BIGINT`, `LEN` is `INTEGER`, a comparison of a `DATE` with a string literal is `BOOLEAN`), and the
   rules read the operand types from it. Nested rules resolve in one pass; an aggregate in an operand of a rule needs
   a second pass, because the planner does not find an aggregate in a function argument.
4. Plan the AST with the PartiQL planner against a `Session`. This resolves names and types.
5. Print the plan as PartiQL with `PartiQLOutputTarget`, the Scribe PartiQL target with the print rules that the
   translation needs, then parse and plan the output to verify it.

The translation is deterministic. A construct without a rule gives `UNSUPPORTED` with the construct name and its
location; the translator does not guess. A function without a rule is unsupported, because a function with the same
name can have different semantics in the two languages. Every rewrite that changes the form of the query adds a
`TRANSLATION_INFO` note.

## Supported subset

Queries and clauses

- `[WITH ...] SELECT [TOP n] [DISTINCT] ... [EXCLUDE ...] [FROM ...] [WHERE] [GROUP BY] [HAVING] [QUALIFY]
  [ORDER BY] [LIMIT n | ALL] [OFFSET n]`, also without `FROM`, with any number of trailing semicolons
- Set operations `UNION [ALL]`, `INTERSECT`, `EXCEPT`, and `MINUS`, also of parenthesized queries with their own
  `ORDER BY` and `LIMIT`
- Inner, outer, cross, and comma joins, `JOIN ... USING`, parenthesized joins; subqueries in `FROM` (also with column
  aliases), `IN`, `EXISTS`, and as scalar expressions
- `GROUP BY` expressions, positions, select-list aliases, and `ALL`; `ORDER BY` output names, positions, and
  expressions; lateral column aliases in the select list
- Window functions `ROW_NUMBER`, `RANK`, `DENSE_RANK`, `LAG`, and `LEAD` with `PARTITION BY` columns and `ORDER BY`,
  and the running `COUNT(*)`
- SUPER arrays in `FROM`: `FROM t, t.arr AS e`, `LEFT JOIN t.arr AS e ON TRUE`, and `UNNEST(t.arr) AS u(e)`

Expressions: column references, SUPER navigation (`t.a.b`, `t.a[0]`), string, numeric, boolean, and NULL literals,
`IS [NOT] NULL`, `[NOT] EXISTS`, `AND`, `OR`, `NOT`, and `COUNT(*)`. Comparisons and the other predicates, operators,
casts, `CASE`, datetime literals and functions, and the function categories follow.

Known constructs without a translation (`PIVOT`, `GROUPING SETS`, window frames, `WITH RECURSIVE`, the system
information functions, ...) are unsupported with a reason and a location.

## Semantic rules

Names and output

- Identifiers: by default, quoted and unquoted identifiers fold to lower case, as with the Redshift default
  `enable_case_sensitive_identifier = false`. Use `RedshiftTranspiler(caseSensitiveIdentifiers = true)` for sources
  that set it to true: then only unquoted identifiers fold, and names match exactly. Only the ASCII letters A to Z
  fold, as in Redshift.
- Every column reference is qualified with the alias of its FROM item, by the Redshift scoping rules (the innermost
  query block that has a FROM item with the column, then the enclosing blocks), because PartiQL resolves an
  unqualified name as a FROM alias or a catalog table first. A table without an alias gets its name as the alias. An
  unknown or ambiguous column is invalid input; a column that more than one FROM item with unknown columns can have
  is unsupported. When a subquery refers to an outer FROM item whose alias one of its own FROM items repeats, the
  outer alias is renamed (`t$1`).
- Select-list aliases in the select list (lateral aliases) and in WHERE, GROUP BY, HAVING, and ORDER BY expressions
  are replaced by the item's expression. In the select list, a column of the same name wins, as in Redshift; in the
  other clauses, a name that is both a column and an alias is unsupported, because Redshift does not document which
  one it uses. A bare ORDER BY name is an output column first. GROUP BY and ORDER BY positions and GROUP BY ALL are
  replaced by the select-list expressions.
- Default output column names: an unaliased column or SUPER path is named after the column or the last attribute, a
  function call or window function after the function (`count`, `row_number`, as in Redshift).
  Other expressions keep the PartiQL name (`_1`, ...) with a note; Redshift names them `?column?`.
- The output is parsed and planned again; an error, or a planner warning that the input does not have, is an internal
  error.

Query structure

- `ORDER BY` always writes the null order: Redshift puts NULL values last for `ASC` and first for `DESC`, also in a
  window specification.
- GROUP BY keys: a reference to a key after the aggregation (HAVING, ORDER BY, window PARTITION BY) uses an alias of
  the key, `GROUP BY "t"['c'] AS "$k0" HAVING "$k0" > 1`, because PartiQL does not match these expressions with the
  keys.
- QUALIFY, `SELECT DISTINCT` with LIMIT or OFFSET, and a window function in ORDER BY move the query into a subquery
  that an outer query filters, sorts, and limits: PartiQL evaluates DISTINCT after LIMIT and OFFSET, and does not
  allow window functions in ORDER BY. A reference to the alias of a window function in QUALIFY is the column of the
  item. A FROM item directly before QUALIFY needs an alias, because Redshift reads QUALIFY as the alias.
- Set operations: the later queries name their items like the first query; when the names of the first query repeat,
  every query names its items `$c1`, `$c2`, ..., and an outer query restores the names. A set operation with ORDER BY
  moves into a subquery that an outer query sorts, because the PartiQL planner resolves an ORDER BY name of a set
  operation as a FROM alias or a table. A set operation as a scalar, `IN`, `ANY`, `ALL`, or `EXISTS` subquery becomes
  the FROM item of a query block, because PartiQL reads the rows of a set operation as structs, not values.
  `INTERSECT ALL` and `EXCEPT ALL` are invalid in Redshift.
- WITH: each common table expression is inlined as a subquery where the query refers to it; `WITH RECURSIVE` is
  unsupported.
- SELECT without FROM reads from a one-row bag.

## Documented deviations

The translation does not model these Redshift behaviors:

- Trailing blanks: Redshift ignores trailing blanks when it compares `VARCHAR` values and SUPER strings, also in
  GROUP BY, DISTINCT, set operations, and joins. The translation does not trim them (it would hide the columns from
  predicate push-down in the target engine), so `c = 'a '` is false in PartiQL for the value `'a'`.
- String comparisons and ordering assume the default case-sensitive collation; the order of characters at or above
  U+10000 can differ.
- ORDER BY and GROUP BY over SUPER objects and arrays pass through; Redshift does not document the order of these
  values.

## PLK 1.6.1 runtime gaps

The translation follows the PartiQL specification. The PartiQL Kotlin implementation 1.6.1 differs from its own types
or from the PartiQL and SQL specifications in these cases; the translation avoids them as noted, or reports the
construct as unsupported:

- The planner does not find an aggregate call in the arguments of a scalar function call: `ABS(SUM(x))` fails with
  `FUNCTION_NOT_FOUND`, and such a call is unsupported. Operators, `CAST`, `CASE`, `COALESCE`, `TRIM`, and `POSITION`
  are not function calls. The calls of the Redshift catalog in a probe pass are function calls too, so an operand with
  an aggregate is passed as a copy in which each aggregate is a NULL of its type, once the type is known.
- `EXCEPT` returns duplicate rows of the left query. The translation adds DISTINCT to a left query that is a simple
  query block, which has the same result.
- A set operation over SUPER values (the dynamic type) does not plan (`SET_OP_SCHEMA_INCOMPATIBLE`); the translation
  reports it as unsupported.
- The plan rewriter does not visit the expressions of ORDER BY; the probe passes visit them separately.
- A path index of the dynamic type gives the planner warning `PATH_INDEX_NEVER_SUCCEEDS`, and the types of the
  expressions in the index are lost; while it resolves operand types, the translator casts such an index to `INT`.

## Tests

- `RedshiftTranspilerSuite` runs the fixtures in `src/test/resources/redshift-frontend/inputs` against the expected
  output of the same name in `outputs`. An expected output is PartiQL text, or a status with a message for a failed
  translation. A case whose marker is prefixed `-- --#[name] TODO(redshift)` is the expected output of a rule that
  follows; it is enabled with the rule.

```shell
./gradlew :partiql-scribe-redshift-frontend:test
```
