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
`ScribeException`. It is an `UNSUPPORTED_OPERATION` whose `RedshiftFrontend.KIND` property says which:
`KIND_SYNTAX` (the text is not in the grammar), `KIND_UNSUPPORTED` (a construct that parses but has no conversion yet),
or `KIND_INVALID` (a form Redshift rejects, such as an ordinal out of range); the message starts with the kind in words. A rewrite that changes the shape of the query (a
`DISTINCT` query with `LIMIT` becomes a subquery) is reported as `TRANSLATION_INFO`. An unknown name, or an operation
PartiQL does not define on the operand types, is found by the planner, not by the front end.

`tables(sql)` lists the tables the query reads, from the same conversion as `toAst`, so a common table expression counts
as a table exactly where the AST reads it as one.

A query that nests deeper than `RedshiftFrontend.MAX_NESTING` (200) is refused: parentheses or `CASE` expressions open
at once, prefix operators (`NOT`, `-`) in a row, and levels of the AST, where each subquery, `CASE`, join, `UNION`, and
each operator of a chain is a level (`a AND b AND c` is two deep). The bound on the text keeps the parser's recursion
bounded, and the bound on the AST keeps the planner's and the targets' recursion bounded. The parser runs with SLL
prediction first and full LL only when SLL finds an error, which keeps its decision cache small.

## Grammar

`src/main/antlr/Redshift.g4` is an ANTLR 4 grammar of the subset of the Redshift *query* language that the front end
translates: the constructs with a PartiQL counterpart that Scribe's targets accept, directly or by an exact rewrite.
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
| `DISTINCT` with `LIMIT` or `OFFSET` | a `DISTINCT` subquery with `ORDER BY`, `LIMIT`, and `OFFSET` outside, because PartiQL evaluates `DISTINCT` after `LIMIT`; items that share an output name get their own names inside (`$1`) and the shared name back outside |
| `GROUP BY 1`, `ORDER BY 1` | the select-list expression; an ordinal that points at `*` is refused, as the columns behind `*` are known only after planning |
| `ORDER BY name` | the select item of that output name (its alias, or the column it selects), when the key is only the name (an output name wins in `ORDER BY`); a name inside an `ORDER BY` expression stays an input column; a name that several items have is ambiguous and refused unless they are the same expression (`x.b` and `b` over one FROM item are) |
| `ORDER BY` under `DISTINCT` with `LIMIT` | the key is matched to the select item it names or repeats, and the outer query sorts by that item's column; under `*` it must be a column of the relation behind the `*` |
| `* EXCLUDE (c)` | `EXCLUDE t.c`, qualified with the single FROM item or the relation the query names; a qualifier the query writes must be a FROM item, and the relation behind a `t.*` |
| `x::t`, `CONVERT(t, x)` | `CAST(x AS t)`; `VARCHAR` and `TEXT` are `VARCHAR(256)`, `CHAR` is `CHAR(1)`, `BPCHAR` is `CHAR(256)`, `DECIMAL` is `DECIMAL(18, 0)`, `VARCHAR(MAX)` is `STRING`, `CHAR(MAX)` is `CHAR(4096)`; a length or precision outside Redshift's range is refused |
| `LIKE` | a literal pattern is rewritten to the form PartiQL takes: Redshift escapes with `\` by default, takes an escaped character of any kind literally, and takes `ESCAPE ''` as no escape; PartiQL takes an escape only before `%`, `_`, and the escape character, so `'a\\b'` is `'ab'` and `'%start\\_%'` is `'%start\_%' ESCAPE '\'`. A pattern that is not a literal keeps Redshift's escape character |
| `ILIKE` | `LOWER(a) LIKE b` with a literal pattern lowered in that form, which cannot turn a letter into the escape character; `LOWER(a) LIKE LOWER(b)` for another pattern, whose escape character must not be a letter |
| `ISNULL`, `NOTNULL`, `!=` | `IS NULL`, `IS NOT NULL`, `<>` |
| `ORDER BY x` | `ORDER BY x ASC NULLS LAST` (Redshift's default order, written explicitly) |
| `t1, t2` | `t1 CROSS JOIN t2` |
| `WITH` | kept as `WITH`; a common table expression is referenced as a local variable, so a table of the same name does not capture it |

The conditional functions, each rewritten to the PartiQL expression with the same value; the planner types the result
as it types any expression:

| Redshift | PartiQL |
|---|---|
| `NVL(e1, ..., en)`, `COALESCE(e1, ..., en)` | `COALESCE(e1, ..., en)` |
| `NULLIF(a, b)` | `NULLIF(a, b)` |
| `NVL2(e, a, b)` | `CASE WHEN e IS NOT NULL THEN a ELSE b END` |
| `DECODE(e, s1, r1, ..., sk, rk [, d])` | `CASE WHEN m1 THEN r1 ... WHEN mk THEN rk ELSE d END`, where the match `mi` is `e IS NULL` when `si` is the NULL literal (`si IS NULL` when `e` is), `e = si` when either is another literal, and `e = si OR (e IS NULL AND si IS NULL)` when both may be NULL, since a NULL search value matches a NULL `e` |
| `GREATEST(v1, ..., vn)`, `LEAST(v1, ..., vn)` | NULL literals left out, then `CASE WHEN c1 THEN v1 ... WHEN c(n-1) THEN v(n-1) ELSE vn END`, where `ci` is the conjunction over `j > i` of `vi >= COALESCE(vj, vi)` (`<=` for `LEAST`): a NULL `vi` fails its branch, a NULL `vj` does not stop `vi`; one argument left is `COALESCE(v)` (so the item keeps its own name), all NULL is NULL |

A wrong number of arguments (`NULLIF(a)`, `NVL2(e, a)`, `DECODE(e, s)`) is refused as invalid.

`DECODE`, `GREATEST`, and `LEAST` repeat their arguments in the rewrite, and a call nested in a repeated position repeats
the repetition, so the AST of a statement is bounded in size: at most 100,000 nodes as the planner walks them, a shared
node counted at each use and a string literal weighted by its length (one node per 16 characters). The bound is checked
on the running total of the repeating rewrites as they are built, so that no oversized tree is built, and on the
finished statement, which also covers an alias that `GROUP BY` or `ORDER BY` repeats. Measured edges: a `GREATEST` of
129 columns or 182 literals converts and one more is refused; `DECODE` nested 11 deep in the expression with column
search values (200 with literal ones), `GREATEST` nested 8 deep; two `GREATEST`s of 120 columns in one statement are
refused together. A statement without repeating rewrites is bounded only by its own size (20,000 select items).

Not converted yet, and refused with `UNSUPPORTED_OPERATION` at their position: the string, math, and datetime functions
(`UPPER`, `CONCAT`, `POWER`, `DATEADD`, ...), the operators `^` and `&`, the `INTERVAL` literal and datetime arithmetic, a `DATE` or
`TIMESTAMP` literal in a form other than `YYYY-MM-DD` and `YYYY-MM-DD HH:MM:SS[.ffffff]`, a backslash escape in a string
literal other than `\\`, `\'`, `\b`, `\f`, `\n`, `\r`, `\t`, a numeric literal of more than 38 digits, a column list
on a derived table or a common table expression, `*` with other select items, `EXCLUDE` with select items other than `*`
or of an unqualified column with several FROM items, a subquery inside an `IN` list or as a function argument (the
PartiQL printer writes it there without its parentheses), `ORDER BY`, `LIMIT`, or `OFFSET` on
a parenthesized query that has its own, an `ORDER BY` or `GROUP BY` ordinal that points at `*` or follows a set
operation, `ILIKE` with an escape character that is not a literal, or a letter when the pattern is not a literal, an
`ORDER BY` name that several select items have when they may be different columns (`b` and `y.b` over several FROM
items), and under `DISTINCT` with `LIMIT` an `ORDER BY` key that is not a select item, `*` over several FROM items,
`t.*` next to other items (a column name may repeat there, which the outer query cannot tell apart without the schema),
and a qualified key of another relation than the `*`. A select-list alias used in `WHERE`, `GROUP BY`, `HAVING`, or a
later select item (a lateral column alias) is left as a name: Redshift reads it as the alias only when no FROM column
has the name, which needs the schema and is a later change; for now the planner reports it as unknown.

Known differences that are not refused: the aggregates `AVG`, `COUNT`, `MAX`, `MIN`, and `SUM` are translated directly,
and `AVG` of an integer, which Redshift truncates, is not yet rewritten; `NULLIF`, `DECODE`, `GREATEST`, and `LEAST`
compare `CHAR(n)` values with their padding, which Redshift disregards, and compare values of different types as
PartiQL does, where Redshift converts one to the type of the other: `DECODE(caldate, '2008-06-01', ...)` from Redshift's
own example converts the string to a `DATE` in Redshift and is never equal in PartiQL (refusing it needs the column's
type, which the planner has and this front end does not); `NVL2`, `DECODE`, `GREATEST`, and `LEAST` return a value of
either branch's type where Redshift converts to a common type (`NVL2(x, '2345', 1234)` is a `VARCHAR` in Redshift,
`GREATEST(ts, DATE '2020-01-01')` a `TIMESTAMP`); an unnamed select item is
named `_n` by the planner where Redshift names it after the function (`coalesce`, `case`, `greatest`), so
`SELECT NVL(b, 0) FROM t ORDER BY coalesce` does not resolve; `ILIKE` folds the case of every letter through
`LOWER`, where Redshift folds only single-byte characters; a `CHAR` or `VARCHAR` length is a number of bytes in Redshift
and of characters in PartiQL, and the number is kept; a `LIKE` pattern that is not a literal keeps Redshift's escape
character, and PartiQL rejects at run time an escape before a character other than `%`, `_`, or the escape itself, which
Redshift takes literally.

## Tests

- `src/test/resources/grammar/accept/*.sql` and `reject/*.sql`: the grammar fixtures, statements that must parse and
  statements that must fail with the expected `line:column`, grouped by reason. `RedshiftParseTreeTest` checks the
  shape of the parse tree where the grammar has to choose.
- `src/test/resources/redshift-frontend/inputs/<group>/*.sql` and `outputs/<group>/*.sql`: the conformance suite, one
  Redshift input and one expected result per case: the input is converted, planned against the Scribe test catalogs
  (`src/test/resources/catalogs` of the root project), and compiled with the PartiQL target. An expected result is
  PartiQL text, compared up to whitespace, the front end's problem code with a message fragment
  (`[UNSUPPORTED_OPERATION{message="function UPPER"}];`), or the planner's (`[PLANNER{message="VAR_REF_NOT_FOUND"}];`).
  `RedshiftTranspilerSuite` runs them, parses and plans every valid output again as a check, parses every input with
  the grammar, and checks that `tables` names the tables the AST reads.
- `RedshiftTranspilerTest`: the public contract: problem codes and kinds, positions, notes, determinism, the nesting
  limit, and the AST planned and compiled by `RedshiftTarget`.

```
./gradlew :partiql-scribe-redshift-frontend:test
```

The build needs JDK 17.
