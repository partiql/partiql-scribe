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
- Typed literals: `DATE`, `TIME`, `TIMETZ`, `TIMESTAMP`, and `TIMESTAMPTZ` in the ISO format and the Redshift formats
  that do not depend on the DateStyle setting (`20080215`, `Feb 15, 2008`, `04:05 PM`), with a numeric time zone
  offset, `Z`, `UTC`, or `GMT`
- `INTERVAL` literals with a qualifier (`INTERVAL '1 2:03:04' DAY TO SECOND`) and without (`INTERVAL '3 hours,
  35 minutes'`)
- Datetime arithmetic: `date +- integer`, `date - date`, `date + time`, datetime and interval `+` and `-`
- SUPER navigation: `t.a.b`, `t.a[0]`
- Functions: `NVL`, `COALESCE`, `NULLIF`, `LEN`, `LENGTH`, `UPPER`, `LOWER`, `CHAR_LENGTH`, `OCTET_LENGTH`, `ABS`,
  `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`
- Datetime functions: `DATEADD`, `DATEDIFF`, `DATE_PART`, `PGDATE_PART`, `DATE_PART_YEAR`, `DATE_TRUNC`, `LAST_DAY`,
  `ADD_MONTHS`, `NEXT_DAY`, `DATE_CMP` and the other comparison functions, `GETDATE`, `SYSDATE`, `CURRENT_DATE`, and
  `CONVERT_TIMEZONE`, `TIMEZONE`, and `AT TIME ZONE` for the UTC time zone
- `EXTRACT(part FROM x)` for the year, quarter, month, week, day, day of week, day of year, hour, minute, second,
  millisecond, decade, century, and millennium dateparts and their abbreviations

## Semantic rules

- Identifiers: by default, quoted and unquoted identifiers fold to lower case, as with the Redshift default
  `enable_case_sensitive_identifier = false`. Use `RedshiftTranspiler(caseSensitiveIdentifiers = true)` for sources
  that set it to true.
- `ORDER BY` always writes the null order: Redshift puts NULL values last for `ASC` and first for `DESC`.
- `VARCHAR` and `TEXT` without a length are `VARCHAR(256)`; `DECIMAL` without parameters is `DECIMAL(18, 0)`.
- The Redshift session time zone is assumed to be UTC, the Redshift default: `GETDATE()`, `SYSDATE`, and
  `CURRENT_DATE` become `UTCNOW()` cast to `TIMESTAMP` (truncated to the second for `GETDATE()`) or `DATE`, and the
  fields of a `TIMESTAMPTZ` value are its UTC fields. PartiQL evaluates `UTCNOW()` for each call and row; Redshift
  returns one value per statement or transaction. The translation adds a note.
- `TIMESTAMPTZ` values in the source data are assumed to be UTC instants. `TIMESTAMPTZ` literals are converted to UTC,
  as Redshift stores them; a `TIMESTAMPTZ` or `TIMETZ` literal without a time zone is in UTC.
- `TIME`, `TIMETZ`, and `TIMESTAMPTZ` types get precision 6, which is the Redshift precision. PartiQL `TIME` without
  a precision is `TIME(0)`.
- `DATEDIFF` counts date part boundaries, PartiQL `DATE_DIFF` counts whole units: the translation counts the whole
  units from `TIMESTAMP '0001-01-01 00:00:00'` (from midnight for `TIME`). The same reference is used by `DATE_TRUNC`,
  `LAST_DAY`, `NEXT_DAY`, and the day of week, day of year, and week dateparts, so values before the year 1 are not
  supported.
- `DATEADD` returns `TIMESTAMP` for a `DATE` value, and moves February 29 to March 1 when it adds years. `EXTRACT`
  returns whole seconds, and `DOUBLE PRECISION` for a `TIMESTAMPTZ` value; `DATE_PART` returns `DOUBLE PRECISION`.
- A character string literal argument of a datetime function becomes a datetime literal, as Redshift converts it.
  Formats that depend on the DateStyle setting, special values (`'today'`), and `24:00:00` are unsupported. A
  `TIMESTAMP` literal ignores its time zone, as in Redshift.
- An interval literal without a qualifier is rewritten with a qualifier, with the leading field precision 9. Redshift
  keeps a time of 24 hours or more (`'36 hours'`) unnormalized, so such a literal is supported only as an operand of
  datetime `+` and `-`.
- Week and quarter boundaries in `DATEDIFF`, quarters, decades, centuries, and millennia in `DATEADD`, `TIMESTAMPTZ`
  arguments of `DATEADD`, `DATEDIFF`, `DATE_TRUNC`, and `ADD_MONTHS`, `EXTRACT(microsecond)`, `EXTRACT(epoch)`, the
  `DOUBLE PRECISION` seconds of `DATE_PART`, `INTERVAL` data types, and time zones other than UTC are unsupported:
  the Redshift documentation does not specify their semantics, or PartiQL has no equivalent.
- `GROUP BY 1` and `ORDER BY 1` are unsupported, because PartiQL reads the number as a constant, not a position.

## PLK 1.6.1 runtime gaps

The translation follows the PartiQL specification. The PartiQL Kotlin implementation 1.6.1 differs in these cases:

- `INTERVAL + INTERVAL` raises an error when a field carries (`INTERVAL '12' HOUR + INTERVAL '13' HOUR`).
- `TIMESTAMP - TIMESTAMP` keeps only milliseconds for differences of about 146 years or more.
- The Scribe PartiQL printer drops the sign of a negative interval shorter than one second, so such interval literals
  are unsupported.

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
