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

- `[WITH ...] SELECT [TOP n] [DISTINCT] ... [EXCLUDE ...] [FROM ...] [WHERE] [GROUP BY] [HAVING] [QUALIFY]
  [ORDER BY] [LIMIT] [OFFSET]`, also without `FROM`, and the set operations `UNION [ALL]`, `INTERSECT`, `EXCEPT`,
  and `MINUS`, also of parenthesized queries
- Inner, outer, cross, and comma joins, `JOIN ... USING`, and parenthesized joins; subqueries in `FROM` (also with
  column aliases), `IN`, and as scalar expressions
- `GROUP BY` expressions, positions, and `ALL`; `ORDER BY` output names and positions; select-list aliases in the
  select list, `WHERE`, `GROUP BY`, `HAVING`, `QUALIFY`, and `ORDER BY`
- Window functions `ROW_NUMBER`, `RANK`, `DENSE_RANK`, `LAG`, and `LEAD` with `PARTITION BY` columns and `ORDER BY`,
  and the running `COUNT(*)`
- SUPER arrays in `FROM`: `FROM t, t.arr AS e`, `LEFT JOIN t.arr AS e ON TRUE`, and `UNNEST(t.arr) AS u(e)`
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
  that set it to true. Only the ASCII letters A to Z fold, as in Redshift.
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
- A character string literal argument of a datetime function (not `EXTRACT`) becomes a datetime literal, as Redshift
  converts it. Formats that depend on the DateStyle setting, special values (`'today'`), and `24:00:00` are
  unsupported. A `TIMESTAMP` literal ignores its time zone, as in Redshift.
- An interval literal without a qualifier is rewritten with a qualifier, with the leading field precision 9. Redshift
  keeps a time of 24 hours or more (`'36 hours'`) unnormalized, so such a literal is supported only as an operand of
  datetime `+` and `-`.
- Week and quarter boundaries in `DATEDIFF`, quarters, decades, centuries, and millennia in `DATEADD`, `TIMESTAMPTZ`
  arguments of `DATEADD`, `DATEDIFF`, `DATE_TRUNC`, and `ADD_MONTHS`, `EXTRACT(microsecond)`, `EXTRACT(epoch)`, the
  `DOUBLE PRECISION` seconds of `DATE_PART`, `INTERVAL` data types, and time zones other than UTC are unsupported:
  the Redshift documentation does not specify their semantics, or PartiQL has no equivalent.
- Names: every column reference is qualified with the alias of its FROM item, by the Redshift scoping rules (the
  innermost query block that has a FROM item with the column, then the enclosing blocks), because PartiQL resolves
  an unqualified name as a FROM alias or a catalog table first. A table without an alias gets its name as the
  alias. An unknown or ambiguous column is invalid input; a column that more than one FROM item with unknown
  columns can have is unsupported. When a subquery refers to an outer FROM item whose alias one of its own FROM
  items repeats, the outer alias is renamed (`t$1`).
- Select-list aliases in the select list (lateral aliases) and in WHERE, GROUP BY, HAVING, and ORDER BY expressions
  are replaced by the item's expression. In the select list, a column of the same name wins, as in Redshift; in
  the other clauses, a name that is both a column and an alias is unsupported, because Redshift does not document
  which one it uses. A bare ORDER BY name is an output column first. GROUP BY and ORDER BY positions and GROUP BY
  ALL are replaced by the select-list expressions.
- GROUP BY keys: a reference to a key after the aggregation (HAVING, ORDER BY, window PARTITION BY) uses an alias of
  the key, `GROUP BY "t"['c'] AS "$k0" HAVING "$k0" > 1`, because PartiQL does not match these expressions with
  the keys.
- QUALIFY, `SELECT DISTINCT` with LIMIT or OFFSET, and a window function in ORDER BY move the query into a subquery
  that an outer query filters, sorts, and limits: PartiQL evaluates DISTINCT after LIMIT and OFFSET, and does not
  allow window functions in ORDER BY. A reference to the alias of a window function in QUALIFY is the column of the
  item, so that both have the same value. A FROM item directly before QUALIFY needs an alias, because Redshift
  reads QUALIFY as the alias.
- Set operations: the later queries name their items like the first query; when the names of the first query
  repeat, every query names its items `$c1`, `$c2`, ..., and an outer query restores the names. `INTERSECT ALL` and
  `EXCEPT ALL` are invalid in Redshift.
- WITH: each common table expression is inlined as a subquery where the query refers to it; `WITH RECURSIVE` is
  unsupported.
- SUPER arrays in FROM: Redshift unnests only arrays, so the FROM item iterates `CASE WHEN x IS LIST THEN x ELSE []
  END`, and the root of the source is a variable reference (`@"t"['x']`), so that a catalog table of the same name
  does not capture it. The evaluator must run in PERMISSIVE mode, as for SUPER navigation.
- A top-level select item whose static type is dynamic (a SUPER value) is wrapped in `COALESCE(e)`, so that a
  navigation to a missing attribute returns NULL, as in Redshift, instead of MISSING.
- Default output column names: an unaliased column or SUPER path is named after the column or the last attribute, a
  function call or window function after the function (`count`, `row_number`), and a CAST after its operand. Other
  expressions keep the PartiQL name (`_1`, ...) with a note; Redshift names them `?column?`.
- Character values in GROUP BY, DISTINCT, set operations, and join conditions keep the PartiQL semantics: trailing
  blanks are significant, while Redshift ignores them. This is a documented deviation.
- ORDER BY and GROUP BY over SUPER objects and arrays pass through; Redshift does not document the order of these
  values.
- The output is parsed and planned again; an error, or a planner warning that the input does not have, is an
  internal error.

## PLK 1.6.1 runtime gaps

The translation follows the PartiQL specification. The PartiQL Kotlin implementation 1.6.1 differs in these cases:

- `EXCEPT` returns duplicate rows of the left query. The translation adds DISTINCT to a left query that is a
  simple query block, which has the same result.
- A set operation over SUPER values (the dynamic type) does not plan (SET_OP_SCHEMA_INCOMPATIBLE); the translation
  reports it as unsupported.
- `CAST` of a `VARCHAR(n)` value to `VARCHAR(m)` returns MISSING, so a set operation whose queries have character
  columns of different lengths can return MISSING values.
- The planner does not find an aggregate in the argument of a function call after GROUP BY (`ABS(SUM(b))`).
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
