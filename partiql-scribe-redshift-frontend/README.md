# PartiQL Scribe Redshift front end

Converts an Amazon Redshift query to a PartiQL AST. The caller plans the AST with the PartiQL planner against its own
session and compiles the plan with `Scribe.compile` and any target (`PartiQLTarget`, `RedshiftTarget`, `SparkTarget`,
`TrinoTarget`, `DuckDBTarget`). The front end needs no session and no planner, and nothing Redshift-specific reaches the
targets.

```kotlin
val context = ScribeContext.standard()
val frontend = RedshiftFrontend(context)
val tables = frontend.tables(sql)                                   // the tables the query reads, to build the Session
val ast = frontend.toAst(sql)                                       // the PartiQL AST
val plan = PartiQLPlanner.standard().plan(ast, session).plan
val output = Scribe(context).compile(plan, session, PartiQLTarget).output.value
```

A problem found while converting is reported to the context's problem listener as a `ScribeProblem` with the `LINE`
and `COLUMN` of its position in the Redshift text, and the one that stops the conversion is thrown as
`ScribeException`. It is an `UNSUPPORTED_OPERATION` whose `KIND` property says which: `SYNTAX` (the text is not in the
grammar), `UNSUPPORTED` (a construct that parses but has no conversion yet), or `INVALID` (a form Redshift rejects, such
as an ordinal out of range); the message starts with the kind in words. A rewrite that changes the shape of the query (a
`DISTINCT` query with `LIMIT` becomes a subquery) is reported as `TRANSLATION_INFO`. An unknown name, or an operation
PartiQL does not define on the operand types, is found by the planner, not by the front end.

`tables(sql)` lists the tables the query reads, from the same conversion as `toAst`, so a common table expression counts
as a table exactly where the AST reads it as one. A query nested more than `RedshiftFrontend.MAX_NESTING` (200)
parentheses deep is refused before parsing.

## Grammar

`src/main/antlr/Redshift.g4` is an ANTLR 4 grammar of the subset of the Redshift *query* language that the front end
translates: the constructs with a PartiQL counterpart that the target views accept, directly or by an exact rewrite.
What parses either translates or is refused at its position; everything else is a syntax error with a line and column. It is case-insensitive and
follows PostgreSQL operator precedence, from which Redshift derives.

What parses:

- One `SELECT` query, with an optional trailing `;`. `DISTINCT`, `ALL`, `TOP n`, `* EXCLUDE (...)`, `WITH`,
  `FROM` (required) with tables, subqueries, and `[INNER | LEFT | RIGHT | FULL [OUTER]] JOIN ... ON`, `CROSS JOIN`
  and comma joins, `WHERE`, `GROUP BY` (expressions and ordinals), `HAVING`, `UNION [ALL | DISTINCT]`,
  `ORDER BY ... [ASC | DESC] [NULLS FIRST | LAST]`, `LIMIT n | ALL`, `OFFSET n`, and subqueries in every clause.
- Expressions: `+ - * /`, unary sign, `^`, `&`, `||`, datetime arithmetic (`d + 1`, `d1 - d2`,
  `ts + INTERVAL '1 day'`), `CASE`, `CAST`, `::`, `CONVERT`, scalar subqueries, and names of up to three parts.
- Conditions: `= <> != < > <= >=`, `AND OR NOT`, `IS [NOT] TRUE | FALSE | UNKNOWN`, `[NOT] LIKE | ILIKE ... [ESCAPE]`,
  `[NOT] BETWEEN`, `IS [NOT] NULL`, `ISNULL`, `NOTNULL`, `[NOT] IN (list | subquery)`.
- Literals: integer, decimal, string (with `''` and backslash escapes), `DATE '...'`, `TIMESTAMP '...'`,
  `INTERVAL '...'` (no qualifier), `TRUE`, `FALSE`, `NULL`.
- Types: `SMALLINT`, `INTEGER`, `BIGINT`, `DECIMAL(p, s)`, `REAL`, `DOUBLE PRECISION`, `CHAR(n)`, `VARCHAR(n)`,
  `DATE`, `TIMESTAMP`, `BOOLEAN`, each with its Redshift aliases.
- Functions, each with its own argument form: `AVG`, `COUNT`, `COUNT(*)`, `MAX`, `MIN`, `SUM` (with `DISTINCT`);
  `DECODE`, `GREATEST`, `LEAST`, `NVL`, `COALESCE`, `NVL2`, `NULLIF`; `CAST`, `CONVERT`; `CURRENT_DATE`, `DATEADD`,
  `DATEDIFF`, `GETDATE()`, `SYSDATE`, `EXTRACT(EPOCH FROM ...)`; `POWER`, `POW`; `CONCAT`, `LOWER`, `UPPER`. Any other
  function call is a syntax error at its opening parenthesis.

The grammar's header comment lists what is deliberately not parsed. The generated parser is in the package
`org.partiql.scribe.frontend.redshift.antlr`; it is an implementation detail and not part of the module's API.

## Translation

Each rule rewrites a form of the Redshift query into a form of PartiQL; the planner then resolves names and types. A
query the planner cannot type (an operation Redshift defines on types PartiQL does not, for example `'a' || 1`) fails
at the planner with its reason. The rewrites:

| Redshift | PartiQL |
|---|---|
| `TOP n` | `LIMIT n` |
| `LIMIT ALL` | no limit |
| `DISTINCT` with `LIMIT` or `OFFSET` | a `DISTINCT` subquery with `ORDER BY`, `LIMIT`, and `OFFSET` outside, because PartiQL evaluates `DISTINCT` after `LIMIT` |
| `GROUP BY 1`, `ORDER BY 1` | the select-list expression; an ordinal that points at `*` is refused, as the columns behind `*` are known only after planning |
| `ORDER BY alias` | the aliased expression, when the key is only the alias (an output name wins in `ORDER BY`); an alias inside an `ORDER BY` expression stays an input column; an alias given to two items is ambiguous and refused |
| `ORDER BY` under `DISTINCT` with `LIMIT` | the key is matched to the select item it names or repeats, and the outer query sorts by that item's output name |
| `* EXCLUDE (c)` | `EXCLUDE t.c`, qualified with the single FROM item or the relation the query names |
| `x::t`, `CONVERT(t, x)` | `CAST(x AS t)`; `VARCHAR` and `TEXT` are `VARCHAR(256)`, `CHAR` is `CHAR(1)`, `BPCHAR` is `CHAR(256)`, `DECIMAL` is `DECIMAL(18, 0)`, `VARCHAR(MAX)` is `STRING`, `CHAR(MAX)` is `CHAR(4096)`; a length or precision outside Redshift's range is refused |
| `LIKE` without `ESCAPE` | `LIKE ... ESCAPE '\'`, Redshift's default escape character, unless the pattern is a literal without a backslash |
| `ILIKE` | `LOWER(a) LIKE LOWER(b)`, with a literal escape character lowered too |
| `ISNULL`, `NOTNULL`, `!=` | `IS NULL`, `IS NOT NULL`, `<>` |
| `ORDER BY x` | `ORDER BY x ASC NULLS LAST` (Redshift's default order, written explicitly) |
| `t1, t2` | `t1 CROSS JOIN t2` |
| `WITH` | kept as `WITH`; a common table expression is referenced as a local variable, so a table of the same name does not capture it |

Not converted yet, and refused with `UNSUPPORTED_OPERATION` at their position: every scalar and datetime function (`NVL`,
`DATEADD`, `UPPER`, ...), the operators `^` and `&`, the `INTERVAL` literal and datetime arithmetic, a column list on a
derived table or a common table expression, `*` with other select items, `EXCLUDE` with select items other than `*` or
with an unqualified column and several FROM items, a subquery inside an `IN` list, an `ORDER BY` or `GROUP BY` ordinal
that points at `*` or follows a set operation or a parenthesized query, and under `DISTINCT` with `LIMIT` an `ORDER BY`
key that is not a select item. A select-list alias used in `WHERE`, `GROUP BY`, `HAVING`, or a
later select item (a lateral column alias) is left as a name: Redshift reads it as the alias only when no FROM column
has the name, which needs the schema and is a later change; for now the planner reports it as unknown. The aggregates `AVG`, `COUNT`, `MAX`, `MIN`, and `SUM` are
translated directly; `AVG` of an integer, which Redshift truncates, is not yet rewritten.

Known differences that are not refused: `ILIKE` folds the case of every letter through `LOWER`, where Redshift folds
only single-byte characters; a `CHAR` or `VARCHAR` length is a number of bytes in Redshift and of characters in PartiQL,
and the number is kept.

## Tests

- `src/test/resources/grammar/accept/*.sql` and `reject/*.sql`: the grammar fixtures, statements that must parse and
  statements that must fail with the expected `line:column`, grouped by reason. `RedshiftParseTreeTest` checks the
  shape of the parse tree where the grammar has to choose.
- `src/test/resources/redshift-frontend/inputs/<group>/*.sql` and `outputs/<group>/*.sql`: the conformance suite, one
  Redshift input and one expected result per case: the input is converted, planned against the Scribe test catalogs
  (`src/test/resources/catalogs` of the root project), and compiled with the PartiQL target. An expected result is
  PartiQL text, compared up to whitespace, the front end's problem code with a message fragment
  (`[UNSUPPORTED_OPERATION{message="function NVL"}];`), or the planner's (`[PLANNER{message="VAR_REF_NOT_FOUND"}];`).
  `RedshiftTranspilerSuite` runs them, parses and plans every valid output again as a check, parses every input with
  the grammar, and checks that `tables` names the tables the AST reads.
- `RedshiftTranspilerTest`: the public contract: problem codes, positions, notes, determinism, and the AST planned and
  compiled by `RedshiftTarget`.

```
./gradlew :partiql-scribe-redshift-frontend:test
```

The build needs JDK 17.
