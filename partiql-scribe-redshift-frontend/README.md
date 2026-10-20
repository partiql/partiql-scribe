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
- Functions:
  - Aggregates: `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`, `BOOL_AND`, `BOOL_OR`, `ANY_VALUE`
  - Conditional: `NVL`, `COALESCE`, `NVL2`, `NULLIF`, `DECODE`, `GREATEST`, `LEAST`
  - Strings: `LEN`, `LENGTH`, `TEXTLEN`, `CHAR_LENGTH`, `CHARACTER_LENGTH`, `OCTET_LENGTH`, `UPPER`, `LOWER`,
    `SUBSTRING`, `LEFT`, `RIGHT`, `TRIM`, `BTRIM`, `LTRIM`, `RTRIM`, `POSITION`, `STRPOS`, `CHARINDEX`, `OCTETINDEX`,
    `REPLACE`, `TRANSLATE`, `SPLIT_PART`, `CONCAT`, `LPAD`, `RPAD`, `REPEAT`, `REPLICATE`, `CHR`, `QUOTE_LITERAL`, and
    `REGEXP_COUNT`, `REGEXP_INSTR`, `REGEXP_REPLACE`, `REGEXP_SUBSTR` with a pattern without regular expression
    operators
  - Math: `ABS`, `MOD`, `ROUND`, `FLOOR`, `CEIL`, `CEILING`, `TRUNC` (also of timestamps), `SIGN`, `PI`, and `POWER`
    and `POW` of literals
  - SUPER: `ARRAY`, `OBJECT`, `GET_ARRAY_LENGTH`, `GET_NUMBER_ATTRIBUTES`, `ARRAY_CONTAINS`, `ARRAYS_OVERLAP`,
    `ARRAY_DISTINCT`, `ARRAY_SORT`, `ARRAY_UNION`, `ARRAY_INTERSECTION`, `IS_ARRAY`, `IS_OBJECT`, `IS_SCALAR`,
    `IS_BOOLEAN`, `IS_FLOAT`, `IS_DECIMAL`, `IS_SMALLINT`, `IS_INTEGER`, `IS_BIGINT`, `IS_VARCHAR`, `IS_CHAR`
  - Date and time: `DATEADD`, `DATEDIFF`, `GETDATE`, `SYSDATE`, `CURRENT_DATE`

  Each function rule documents its conditions; for example, `LEFT(s, n)` requires a literal `n`. Known functions without
  a translation (`MEDIAN`, `SQRT`, `JSON_PARSE`, the system information functions, ...) are unsupported with a reason.
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

### Functions

- The rules depend on the static types of the arguments, which the translator gets from the PartiQL planner. An
  argument of type SUPER is unsupported where Redshift returns NULL for a value of the wrong type and PartiQL returns
  MISSING (`ABS`, `FLOOR`, `SUM`, ...), or where the Redshift result for mixed types is not documented (`MIN`, `MAX`).
- `CHAR(n)` values: Redshift disregards the trailing blanks, and removes them when it converts the value to another
  string type. The translation trims them (`TRIM(TRAILING FROM x)`) in `LEN` and its synonyms, `SUBSTRING`, `LEFT`,
  `RIGHT`, `REPLACE`, `TRANSLATE`, `SPLIT_PART`, `LPAD`, `RPAD`, `REPEAT`, `CONCAT`, the trim functions, the comparisons
  of `NULLIF`, `DECODE`, `GREATEST`, and `LEAST`, and in `MIN` and `MAX`, which return the value without the padding.
  `OCTET_LENGTH`, `UPPER`, and `LOWER` keep the padding, as Redshift does. A `CHAR(n)` value that is unified with other
  character strings (`NVL`, `COALESCE`, `NVL2`, `DECODE`, `GREATEST`, `LEAST`) is trimmed. Where the Redshift behavior is
  not documented, a `CHAR(n)` argument is unsupported: `POSITION`, `STRPOS`, `CHARINDEX`, `OCTETINDEX`, `QUOTE_LITERAL`,
  `ARRAY`, `ARRAY_CONTAINS`, `CONCAT` of two `CHAR(n)` values, and `OCTET_LENGTH` or `POSITION` of a conditional
  expression that trims `CHAR(n)` values.
- Redshift converts a numeric or `DATE` argument of a string function to a character string. The translation adds
  `CAST(x AS STRING)` for `SMALLINT`, `INTEGER`, `BIGINT`, and `DATE` values, and for a `DECIMAL(p, s)` column, literal,
  or `CAST` with s at most 6. Other arguments are unsupported, because PartiQL writes their text differently: a
  `DECIMAL` value with a scale above 6 with an exponent (`0E-8`), a `TIMESTAMP` value with a `T`, a `DOUBLE PRECISION`
  value with an exponent. The scale of other `DECIMAL` expressions, for example a quotient, can differ.
- `AVG` of an integer argument returns `BIGINT` without the fraction, truncated toward zero; the translation is
  `SUM(x) / COUNT(x)`, or `CAST(AVG(x) - AVG(x) % 1 AS BIGINT)` for `BIGINT`. `AVG` of a `DECIMAL` argument is
  unsupported: Redshift returns the scale of the argument and does not document how it shortens the fraction.
- `SUM`, `MIN`, `MAX`, and `ABS` of a `DECIMAL(p, s)` argument are cast to the Redshift result type (`DECIMAL(38, s)`
  for `SUM`, `DECIMAL(p, s)` otherwise). `SUM` of a `DECIMAL` quotient is unsupported, because the scales differ.
- `ANY_VALUE(x)` becomes `MIN(x)`, which returns one of the values that Redshift can return.
- The values that a conditional function returns are unified to one type: a string literal next to values of another
  type converts to that type (`NVL(int_col, '0')`), and a value of another numeric or datetime type is cast to the common
  type (`NVL(d, 0)` of a `DECIMAL(5, 2)` value `d` is `COALESCE(d, CAST(0 AS DECIMAL(12, 2)))`). Values of different
  type families, a cast that cannot hold every value, and a string literal that Redshift may convert differently
  (`'-0'`, `'05'`) are unsupported.
- `ROUND` rounds half away from zero, as `CAST(x AS DECIMAL(38, n))` does. `ROUND` and `TRUNC` of a `BIGINT`, `ROUND`
  of a floating-point value, and `TRUNC` of a floating-point value with digits are unsupported: Redshift does not
  document the conversion of the argument, or how it rounds ties. `FLOOR`, `CEIL`, `ROUND`, and `TRUNC` of a `DECIMAL`
  return `DECIMAL(38, n)`, and `SIGN` returns `INTEGER`; the translation adds a note.
- `TRUNC` of a `TIMESTAMPTZ` value returns its date in UTC, the session time zone.
- `REAL` and `DOUBLE PRECISION` values are assumed not to be NaN (`SIGN`, `GREATEST`, `LEAST`, `MIN`, `MAX`).
- `POWER` of literals becomes its value when the exact value is a `DOUBLE PRECISION` value.
- SUPER: the elements of `ARRAY(...)` and `OBJECT(...)` that have the dynamic type are wrapped in `COALESCE(x)`, which
  is NULL for MISSING. `OBJECT` requires string literal keys that are all different, and values that cannot be NULL,
  because Redshift does not document whether an object keeps an attribute whose value is NULL. The type tests
  (`IS_ARRAY`, ...) are false for NULL. `ARRAY_CONTAINS` assumes `default_array_search_null_handling = TRUE`.
  `ARRAY_DISTINCT`, `ARRAY_UNION`, and `ARRAY_INTERSECTION` return the elements sorted; Redshift does not guarantee an
  order.
- `UPPER` and `LOWER` use the full Unicode case mapping of Java (`UPPER('ß')` is `'SS'`); Redshift does not document
  its mapping of such characters.
- A function whose PartiQL translation is a function call is unsupported when its argument contains an aggregate
  (`ABS(SUM(x))`); see the PLK 1.6.1 gaps.

### PLK 1.6.1 runtime gaps

The translation avoids these defects of PLK 1.6.1:

- `SUM` of a `BIGINT` argument fails at run time (`getBigDecimal is not valid for type BIGINT`); the translation is
  `CAST(SUM(CAST(x AS DECIMAL(38, 0))) AS BIGINT)`.
- `AVG` of a `SMALLINT` or `REAL` argument fails at run time; the translation uses `SUM(x) / COUNT(x)` and
  `AVG(CAST(x AS DOUBLE PRECISION))`.
- `SUM`, `AVG`, `MIN`, and `MAX` of a `DECIMAL` argument, `SUM` of a `BIGINT` argument, and `ABS` of a `DECIMAL`
  argument have the static type `DECIMAL(38, 19)` or `DECIMAL(39, 19)`, so arithmetic on a result with more digits fails;
  the translation casts the result. `MIN` and `MAX` of a non-numeric argument have the dynamic type.
- `CASE` and `COALESCE` have the common type of their values, but the values are not converted, so an expression that
  uses the result is MISSING for a value of another type; the translation casts the values.
- There is no run-time cast from `VARCHAR` or `CHAR` to `CHAR(n)` (`UNDEFINED_CAST`).
- The planner does not find an aggregate in the arguments of a scalar function call: `ABS(SUM(x))` fails with
  `FUNCTION_NOT_FOUND`. COALESCE, CASE, CAST, and the operators are not function calls. The probe calls of the operand
  types are function calls too, so the function rules probe a copy of an operand in which each aggregate is a NULL of
  its type.
- An index into `SPLIT(NULL, d)` fails with a `NullPointerException`; the translation of `SPLIT_PART` tests for NULL.
- `CAST(x AS STRING)` of a `DECIMAL` value with a scale above 6 uses an exponent.
- `x IS FLOAT` is false for negative values and 0; `IS_FLOAT` uses `x IS DOUBLE PRECISION`.
- The Scribe PartiQL target prints `x IS DECIMAL(p, s)` and `x IS INTEGER` as `p IS DECIMAL` and `x IS INT`, so
  `IS_SMALLINT`, `IS_INTEGER`, and `IS_BIGINT` of a SUPER value, which need these tests, are unsupported.

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
