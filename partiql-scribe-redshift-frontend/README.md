# PartiQL Scribe Redshift front end

A front end that reads Amazon Redshift SQL for translation to PartiQL. This module currently contains the Redshift
grammar and its tests; the translation follows in later changes.

## Grammar

`src/main/antlr/Redshift.g4` is an ANTLR 4 grammar of the subset of the Redshift *query* language that the front end
translates: the constructs with a PartiQL counterpart that the target views accept, directly or by an exact rewrite.
What parses is what translates; everything else is a syntax error with a line and column. It is case-insensitive and
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
  `DATEDIFF`, `GETDATE()`, `SYSDATE`, `EXTRACT(EPOCH FROM ...)`; `POWER`; `CONCAT`, `LOWER`, `UPPER`. Any other
  function call is a syntax error at its opening parenthesis.

The grammar's header comment lists what is deliberately not parsed. The generated parser is in the package
`org.partiql.scribe.frontend.redshift.antlr`; it is an implementation detail and not part of the module's API.

## Tests

- `src/test/resources/grammar/accept/*.sql`: statements the grammar parses, one file per area of the grammar, each
  statement under a `--#[name]` marker.
- `src/test/resources/grammar/reject/*.sql`: statements the grammar refuses, each marker carrying the expected
  `line:column` of the first syntax error: `--#[name] 1:17`. The files group them by reason: other statements,
  malformed queries, other dialects, constructs the target views do not support yet, constructs with no counterpart,
  constructs that cannot be in a view query, and the refused parts of accepted constructs.
- `RedshiftParseTreeTest`: the shape of the parse tree where the grammar has to choose (operator precedence, `UNION`
  and join associativity, keywords that are also identifiers, literal prefixes).

```
./gradlew :partiql-scribe-redshift-frontend:test
```

The build needs JDK 17.
