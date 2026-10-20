# PartiQL Scribe Redshift Frontend

Translates Amazon Redshift queries to PartiQL. Scribe targets translate PartiQL to other dialects; this module goes
the other way for Redshift, and reuses the PartiQL planner and the Scribe PartiQL target to do it.

```kotlin
val result = RedshiftTranspiler().transpile("SELECT TOP 5 NVL(c, 'none') AS v FROM t", session)
result.status   // VALID
result.partiql  // SELECT COALESCE("t"['c'], 'none') AS "v" FROM "default"."T" AS "t" LIMIT 5
result.problems // TRANSLATION_INFO notes for TOP and NVL, with line and column
```

## Pipeline

1. Parse the Redshift text with `src/main/antlr/Redshift.g4`.
2. Build a PartiQL AST from the parse tree (`RedshiftAstBuilder`). Dialect differences are rewritten here; function
   rules are in `RedshiftFunctions`.
3. Plan the AST with the PartiQL planner against a `Session`. This resolves names and types.
4. Print the plan with the Scribe `PartiQLTarget`, then parse and plan the output to verify it.

The translation is deterministic. A construct without a rule gives `UNSUPPORTED` with the construct name and its
location; the translator does not guess. A function that is not in `RedshiftFunctions` is unsupported, because a
function with the same name can have different semantics in the two languages.

## Supported subset

- `SELECT [TOP n] [DISTINCT] ... FROM ... [WHERE] [GROUP BY] [HAVING] [ORDER BY] [LIMIT] [OFFSET]`
- Inner, outer, and cross joins; subqueries in `FROM`, `IN`, and as scalar expressions
- Comparison, `BETWEEN`, `IN`, `LIKE`, `IS [NOT] NULL`, `AND`, `OR`, `NOT`, arithmetic, `||`, `CASE`
- `CAST(x AS t)` and `x::t` for numeric, character, boolean, `DATE`, `TIME`, `TIMETZ`, `TIMESTAMP`, and
  `TIMESTAMPTZ` types
- Typed literals: `DATE`, `TIME`, `TIMETZ`, `TIMESTAMP`, and `TIMESTAMPTZ`, with a numeric time zone offset
- SUPER navigation: `t.a.b`, `t.a[0]`
- Functions: `NVL`, `COALESCE`, `NULLIF`, `LEN`, `LENGTH`, `UPPER`, `LOWER`, `CHAR_LENGTH`, `OCTET_LENGTH`, `ABS`,
  `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`, `DATEADD`, `DATEDIFF`, `GETDATE`, `SYSDATE`, `CURRENT_DATE`
- `EXTRACT(part FROM x)` for the year, month, day, hour, minute, and second dateparts and their abbreviations

## Semantic rules

- Identifiers: by default, quoted and unquoted identifiers fold to lower case, as with the Redshift default
  `enable_case_sensitive_identifier = false`. Use `RedshiftTranspiler(caseSensitiveIdentifiers = true)` for sources
  that set it to true.
- `ORDER BY` always writes the null order: Redshift puts NULL values last for `ASC` and first for `DESC`.
- `VARCHAR` and `TEXT` without a length are `VARCHAR(256)`; `DECIMAL` without parameters is `DECIMAL(18, 0)`.
- `GETDATE()` and `SYSDATE` become `UTCNOW()`, which returns `TIMESTAMP WITH TIME ZONE` in UTC.
- `TIME`, `TIMETZ`, and `TIMESTAMPTZ` types get precision 6, which is the Redshift precision. PartiQL `TIME` without
  a precision is `TIME(0)`.
- `EXTRACT(SECOND ...)` returns an INTEGER in Redshift for `TIMESTAMP` and `TIME` values, and a DECIMAL with the
  fraction in PartiQL. The translation adds a note.
- A `TIMESTAMP` or `TIME` literal with an offset, and a literal with a time zone name, are unsupported. Redshift
  ignores the offset of a `TIMESTAMP` literal; PartiQL accepts only numeric offsets.
- `GROUP BY 1` and `ORDER BY 1` are unsupported, because PartiQL reads the number as a constant, not a position.

## Tests

- `RedshiftTranspilerSuite` runs the fixtures in `src/test/resources/redshift-frontend/inputs` against the expected
  output of the same name in `outputs`. An expected output is PartiQL text, or a status with a message for a failed
  translation.
- `RoundTripSuite` translates the Scribe Redshift fixtures (`src/test/resources/outputs/redshift` in the root
  project) back to PartiQL. Each fixture is in one of two sets in `src/test/resources/redshift-frontend/roundtrip`:
  - `happy.txt`: the translation is `VALID` and passes its oracle. `PARTIQL` means the output is equal to the Scribe
    PartiQL fixture. `REDSHIFT` means Scribe translates the output to the same Redshift text again; this oracle
    applies when the Scribe Redshift target adds casts, so the PartiQL text cannot be equal.
  - `unhappy.txt`: the translation does not round-trip. Each line gives the expected status and the reason, so the
    file is also the list of gaps.

  Every Scribe fixture must be in exactly one set. After a change that moves fixtures between the sets, classify
  them again and review the change to both files:

  ```shell
  ./gradlew :partiql-scribe-redshift-frontend:test --tests '*RoundTripSuite*' -PupdateRoundTrip
  ```

```shell
./gradlew :partiql-scribe-redshift-frontend:test
```
