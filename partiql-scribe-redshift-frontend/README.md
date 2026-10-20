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
2. Build a PartiQL AST from the parse tree. Dialect differences are rewritten here: queries and clauses in
   `RedshiftAstBuilder`, expressions in `RedshiftExpressions` (with `RedshiftConditions`, `RedshiftPatterns`,
   `RedshiftOperators`, and `RedshiftCasts`), datetime constructs in `RedshiftDatetime`, and function calls in
   `RedshiftFunctions` and its category classes.
3. A rule whose Redshift semantics depend on the static types of its operands (for example `AVG` of an integer, or
   `LEN` of a `CHAR(n)` value) gets the types from probe passes, which plan the AST with probe calls before the final
   translation (`OperandTypes`).
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

Expressions

- Comparisons, `= ANY`, `= SOME`, and `<> ALL` with a subquery, `[NOT] BETWEEN`, `[NOT] IN` with a list or a subquery,
  row values `(a, b) [NOT] IN ((1, 2), ...)`, `IS [NOT] NULL`, `IS [NOT] TRUE`, `FALSE`, and `UNKNOWN`, `[NOT]
  EXISTS`, `AND`, `OR`, `NOT`
- `[NOT] LIKE` and `~~`; `ILIKE` and `~~*` with an ASCII literal pattern; `SIMILAR TO` and POSIX `~` and `!~` with a
  literal pattern without regular expression metacharacters
- Arithmetic, signs, `@` (absolute value), `&` (bitwise AND), `||`, simple and searched `CASE`
- `CAST(x AS t)`, `x::t`, and `CONVERT(t, x)` with the Redshift type names and aliases (`INT4`, `FLOAT8`, `FLOAT(p)`,
  `BPCHAR(n)`, `NVARCHAR(n)`, `CHARACTER VARYING(n)`, `VARCHAR(MAX)`, ...)
- Literals: string literals with the backslash escapes `\\`, `\'`, `\b`, `\f`, `\n`, `\r`, and `\t`; `DATE`, `TIME`,
  `TIMETZ`, `TIMESTAMP`, and `TIMESTAMPTZ` literals in the ISO format and the Redshift formats that do not depend on
  the DateStyle setting (`20080215`, `Feb 15, 2008`, `04:05 PM`), with a numeric offset, `Z`, `UTC`, or `GMT`;
  `INTERVAL` literals with a qualifier (`INTERVAL '1 2:03:04' DAY TO SECOND`) and without (`INTERVAL '3 hours, 35
  minutes'`)
- Datetime arithmetic: `date +- integer`, `date - date`, `date + time`, and datetime and interval `+` and `-`
- SUPER navigation: `t.a.b`, `t.a[0]`, `t.a[i + 1]`

Functions

- Aggregates: `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`, `BOOL_AND`, `BOOL_OR`, `ANY_VALUE`
- Conditional: `NVL`, `COALESCE`, `NVL2`, `NULLIF`, `DECODE`, `GREATEST`, `LEAST`
- Strings: `LEN`, `LENGTH`, `TEXTLEN`, `CHAR_LENGTH`, `CHARACTER_LENGTH`, `OCTET_LENGTH`, `UPPER`, `LOWER`,
  `SUBSTRING`, `LEFT`, `RIGHT`, `TRIM`, `BTRIM`, `LTRIM`, `RTRIM`, `POSITION`, `STRPOS`, `CHARINDEX`, `OCTETINDEX`,
  `REPLACE`, `TRANSLATE`, `SPLIT_PART`, `CONCAT`, `LPAD`, `RPAD`, `REPEAT`, `REPLICATE`, `CHR`, `QUOTE_LITERAL`, and
  `REGEXP_COUNT`, `REGEXP_INSTR`, `REGEXP_REPLACE`, `REGEXP_SUBSTR` with a pattern without regular expression
  operators
- Math: `ABS`, `MOD`, `ROUND`, `FLOOR`, `CEIL`, `CEILING`, `TRUNC` (also of timestamps), `SIGN`, `PI`, and `POWER`
  and `POW` of literals
- Date and time: `DATEADD`, `DATEDIFF`, `DATE_PART`, `PGDATE_PART`, `DATE_PART_YEAR`, `EXTRACT`, `DATE_TRUNC`,
  `LAST_DAY`, `ADD_MONTHS`, `NEXT_DAY`, `DATE_CMP` and the other comparison functions, `GETDATE`, `SYSDATE`,
  `CURRENT_DATE`, and `CONVERT_TIMEZONE`, `TIMEZONE`, and `AT TIME ZONE` for the UTC time zone. `EXTRACT` and
  `DATE_PART` support the year, quarter, month, week, day, day of week, day of year, hour, minute, second,
  millisecond, decade, century, and millennium dateparts and their abbreviations.
- SUPER: `ARRAY`, `OBJECT`, `GET_ARRAY_LENGTH`, `GET_NUMBER_ATTRIBUTES`, `ARRAY_CONTAINS`, `ARRAYS_OVERLAP`,
  `ARRAY_DISTINCT`, `ARRAY_SORT`, `ARRAY_UNION`, `ARRAY_INTERSECTION`, `IS_ARRAY`, `IS_OBJECT`, `IS_SCALAR`,
  `IS_BOOLEAN`, `IS_FLOAT`, `IS_DECIMAL`, `IS_SMALLINT`, `IS_INTEGER`, `IS_BIGINT`, `IS_VARCHAR`, `IS_CHAR`

Each rule documents its conditions; for example, `LEFT(s, n)` requires a literal `n`. Known constructs without a
translation (`MEDIAN`, `LISTAGG`, `SQRT`, `JSON_PARSE`, `OBJECT_TRANSFORM`, `PIVOT`, `GROUPING SETS`, window frames,
`TRY_CAST`, the system information functions, ...) are unsupported with a reason and a location.

## Semantic rules

Names and output

- Identifiers: by default, quoted and unquoted identifiers fold to lower case, as with the Redshift default
  `enable_case_sensitive_identifier = false`. Use `RedshiftTranspiler(caseSensitiveIdentifiers = true)` for sources
  that set it to true. Only the ASCII letters A to Z fold, as in Redshift.
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
  function call or window function after the function (`count`, `row_number`), and a CAST after its operand. Other
  expressions keep the PartiQL name (`_1`, ...) with a note; Redshift names them `?column?`.
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
  every query names its items `$c1`, `$c2`, ..., and an outer query restores the names. `INTERSECT ALL` and `EXCEPT
  ALL` are invalid in Redshift.
- WITH: each common table expression is inlined as a subquery where the query refers to it; `WITH RECURSIVE` is
  unsupported.
- SELECT without FROM reads from a one-row bag.

Comparisons, conversions, and literals

- Implicit conversions: Redshift converts a character string literal to the type of the other operand of a
  comparison, `IN`, `BETWEEN`, or simple `CASE`. The translation converts the literal at translation time when both
  readings agree (`col_date = '2020-01-01'` becomes `col_date = DATE '2020-01-01'`), and reports the comparison as
  unsupported otherwise. A comparison of a number with a character string that is not a literal, and other
  comparisons across type families, are unsupported.
- Redshift compares and computes `REAL` with another number, and `DOUBLE PRECISION` with `DECIMAL`, in `DOUBLE
  PRECISION`; the translation casts the operands. A comparison with a literal that the floating-point type holds
  exactly needs no cast.
- `BETWEEN` with a bound that is not a literal, or is NULL, becomes `x >= a AND x <= b`, because PartiQL `BETWEEN`
  is NULL when a bound is NULL.
- `CASE`, `NVL`, `COALESCE`, and the other conditional expressions cast their results to the common result type when
  their numeric or datetime types differ, because PartiQL computes that type but does not convert the results. A
  `WHEN NULL` branch is removed. Results of different type families are unsupported.
- Casts: Redshift `VARCHAR(n)` holds n bytes and PartiQL `VARCHAR(n)` n characters, so a `CAST` to `VARCHAR(n)`, or
  to `VARCHAR` without a length (256 bytes), that can truncate a character string is unsupported; otherwise the cast
  is to `STRING`. A `CAST` to `CHAR(n)` is supported only as a top-level SELECT item. `DECIMAL` without parameters is
  `DECIMAL(18, 0)`.
- `DECIMAL`: with the default `enable_numeric_rounding = off`, a Redshift `CAST` that reduces the scale truncates a
  value from a table and rounds a constant. The translation truncates (`x - x % 1`), which assumes that values have
  the declared scale of their type; a constant that needs rounding is unsupported. A `DECIMAL` division is
  translated only when its quotient is exact in both languages, because the result scales differ; the translation
  adds a note. PartiQL types `-x` and `ABS(x)` of a `DECIMAL(p, s)` value as a wider type, so the translation casts
  them back.
- String literals: Redshift reads backslash escapes. The translation decodes `\\`, `\'`, `\b`, `\f`, `\n`, `\r`, and
  `\t`, and reports other escapes as unsupported.
- A numeric literal with an exponent (`1e3`) is unsupported: its Redshift type is not documented.

Strings

- `CHAR(n)` values: Redshift disregards the trailing blanks, and removes them when it converts the value to another
  string type. The translation trims them (`TRIM(TRAILING FROM x)`) in comparisons, `IN`, `||`, `LEN` and its
  synonyms, `SUBSTRING`, `LEFT`, `RIGHT`, `REPLACE`, `TRANSLATE`, `SPLIT_PART`, `LPAD`, `RPAD`, `REPEAT`, `CONCAT`,
  the trim functions, the comparisons of `NULLIF`, `DECODE`, `GREATEST`, and `LEAST`, and in `MIN` and `MAX`.
  `OCTET_LENGTH`, `UPPER`, `LOWER`, and `LIKE` keep the padding, as Redshift does. Where the Redshift behavior is not
  documented, a `CHAR(n)` argument is unsupported (`POSITION`, `STRPOS`, `CHARINDEX`, `OCTETINDEX`, `QUOTE_LITERAL`,
  `ARRAY`, `ARRAY_CONTAINS`, `CONCAT` of two `CHAR(n)` values).
- Redshift converts a numeric or `DATE` argument of a string function or `||` to a character string. The translation
  adds `CAST(x AS STRING)` for `SMALLINT`, `INTEGER`, `BIGINT`, and `DATE` values, and for a `DECIMAL(p, s)` column,
  literal, or `CAST` with s at most 6. Other arguments are unsupported, because PartiQL writes their text differently.
  A `DATE` is written as `yyyy-mm-dd`, which matches Redshift for the years 0001 to 9999.
- `LIKE`: the default escape character is the backslash. A literal pattern is decoded and written again for PartiQL,
  with `ESCAPE '\'` where a literal `%`, `_`, or backslash needs it. A pattern that is not a literal gets `ESCAPE '\'`
  and a note: PartiQL raises an error for a backslash before a character other than `%`, `_`, or a backslash, which
  Redshift matches as that character. `ILIKE` ignores the case of ASCII letters only; the translation matches the
  lower-case value with an ASCII pattern.
- `UPPER` and `LOWER` use the full Unicode case mapping of Java (`UPPER('ß')` is `'SS'`); Redshift does not document
  its mapping of such characters.

Numbers and aggregates

- `AVG` of an integer argument returns `BIGINT` without the fraction, truncated toward zero; the translation is
  `SUM(x) / COUNT(x)`, or `CAST(AVG(x) - AVG(x) % 1 AS BIGINT)` for `BIGINT`. `AVG` of a `DECIMAL` argument is
  unsupported: Redshift returns the scale of the argument and does not document how it shortens the fraction.
- `SUM`, `MIN`, `MAX`, and `ABS` of a `DECIMAL(p, s)` argument are cast to the Redshift result type (`DECIMAL(38, s)`
  for `SUM`, `DECIMAL(p, s)` otherwise). The type of an operand with an aggregate call is the Redshift result type.
- `ANY_VALUE(x)` becomes `MIN(x)`, which returns one of the values that Redshift can return.
- `ROUND` rounds half away from zero, as `CAST(x AS DECIMAL(38, n))` does. `ROUND` and `TRUNC` of a `BIGINT`, `ROUND`
  of a floating-point value, and `TRUNC` of a floating-point value with digits are unsupported: Redshift does not
  document the conversion of the argument, or how it rounds ties. `FLOOR`, `CEIL`, `ROUND`, and `TRUNC` of a `DECIMAL`
  return `DECIMAL(38, n)`, and `SIGN` returns `INTEGER`; the translation adds a note.
- `POWER` of literals becomes its value when the exact value is a `DOUBLE PRECISION` value.
- `RANK` returns `INTEGER` in Redshift and `BIGINT` in PartiQL; the translation adds a note.

Date and time

- `DATEDIFF` counts datepart boundaries, PartiQL `DATE_DIFF` counts whole units: the translation counts the whole
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
  datetime `+` and `-`. `DATE` plus such a literal returns `TIMESTAMP`, so the literal is unsupported inside interval
  arithmetic (`INTERVAL '1 month' * 2`).
- An interval can be multiplied by an integer. Redshift carries fractions of months into days when it multiplies or
  divides an interval by another number, and PartiQL does not: such operations are unsupported.
- A `TIMETZ` literal whose offset is not UTC is unsupported in comparisons, including simple `CASE` and `NULLIF`.
- `TIME`, `TIMETZ`, and `TIMESTAMPTZ` types get precision 6, which is the Redshift precision. PartiQL `TIME` without a
  precision is `TIME(0)`.
- Week and quarter boundaries in `DATEDIFF`, quarters, decades, centuries, and millennia in `DATEADD`, `TIMESTAMPTZ`
  arguments of `DATEADD`, `DATEDIFF`, `DATE_TRUNC`, and `ADD_MONTHS`, `EXTRACT(microsecond)`, `EXTRACT(epoch)`, the
  `DOUBLE PRECISION` seconds of `DATE_PART`, `INTERVAL` data types, and time zones other than UTC are unsupported:
  the Redshift documentation does not specify their semantics, or PartiQL has no equivalent.
- `TRUNC` of a `TIMESTAMPTZ` value returns its date in UTC, the session time zone.

SUPER values

- Navigation: Redshift matches an attribute name case-sensitively after it folds the identifier, so the translation
  writes each attribute step as a delimited name with the ASCII letters folded (`t.x.Y` becomes `"t"['x']['y']`). A
  bracket step with a character string key is unsupported.
- A top-level select item whose static type is dynamic (a SUPER value), or that has an array subscript, is wrapped in
  `COALESCE(e)`, so that a navigation to a missing attribute returns NULL, as in Redshift, instead of MISSING. The
  elements of `ARRAY(...)` and `OBJECT(...)` that have the dynamic type are wrapped the same way.
- SUPER arrays in FROM: Redshift unnests only arrays, so the FROM item iterates `CASE WHEN x IS LIST THEN x ELSE []
  END`, and the root of the source is a variable reference (`@"t"['x']`), so that a catalog table of the same name
  does not capture it. The evaluator must run in PERMISSIVE mode, as for SUPER navigation.
- An argument of type SUPER is unsupported where Redshift returns NULL for a value of the wrong type and PartiQL
  returns MISSING (`ABS`, `FLOOR`, `SUM`, arithmetic, ...), or where the Redshift result for mixed types is not
  documented (`MIN`, `MAX`, set operations).
- `OBJECT` requires string literal keys that are all different, and values that cannot be NULL, because Redshift does
  not document whether an object keeps an attribute whose value is NULL. The type tests (`IS_ARRAY`, ...) are false
  for NULL. `ARRAY_CONTAINS` assumes `default_array_search_null_handling = TRUE`. `ARRAY_DISTINCT`, `ARRAY_UNION`, and
  `ARRAY_INTERSECTION` return the elements sorted; Redshift does not guarantee an order.

## Documented deviations

The translation does not model these Redshift behaviors:

- Trailing blanks: Redshift ignores trailing blanks when it compares `VARCHAR` values and SUPER strings, also in
  GROUP BY, DISTINCT, set operations, and joins. The translation does not trim them (it would hide the columns from
  predicate push-down in the target engine), so `c = 'a '` is false in PartiQL for the value `'a'`.
- Session time zone: the Redshift session time zone is assumed to be UTC, the Redshift default. `GETDATE()`,
  `SYSDATE`, and `CURRENT_DATE` become `UTCNOW()` cast to `TIMESTAMP` (truncated to the second for `GETDATE()`) or
  `DATE`. PartiQL evaluates `UTCNOW()` for each call and row; Redshift returns one value per statement or transaction.
  The translation adds a note. `TIMESTAMPTZ` values in the source data are assumed to be UTC instants; `TIMESTAMPTZ`
  literals are converted to UTC, as Redshift stores them, and a `TIMESTAMPTZ` or `TIMETZ` literal without a time zone
  is in UTC.
- `REAL` and `DOUBLE PRECISION` values are assumed not to be NaN.
- String comparisons and ordering assume the default case-sensitive collation; the order of characters at or above
  U+10000 can differ.
- ORDER BY and GROUP BY over SUPER objects and arrays pass through; Redshift does not document the order of these
  values.

## PLK 1.6.1 runtime gaps

The translation follows the PartiQL specification. The PartiQL Kotlin implementation 1.6.1 differs from its own types
or from the PartiQL and SQL specifications in these cases; the translation avoids them as noted, or reports the
construct as unsupported:

- LIKE `%` and `_` do not match line terminators. The translation does not work around this.
- A `CAST` of a character string to `DATE`, `TIME`, `TIMETZ`, `TIMESTAMP`, or `TIMESTAMPTZ` evaluates to MISSING, and
  a `CAST` to `DECIMAL`, `REAL`, or `DOUBLE PRECISION` fails for strings such as `'0.5'`. String literals are converted
  at translation time; other strings are unsupported.
- A `CAST` of a character string to an integer type fails for some white space around the number (`'0 '`, `' 012'`):
  the translation trims it. A `VARCHAR(n)` or `CHAR(n)` value can only be cast to `STRING`: the translation casts it
  to `STRING` first. There is no run-time cast from `VARCHAR` or `CHAR` to `CHAR(n)`.
- A `CAST` of `DOUBLE PRECISION` to `REAL` evaluates to MISSING for values that are not positive: unsupported.
- A `CAST` of `TIMESTAMP` or `TIMESTAMPTZ` to `TIME WITH TIME ZONE` evaluates to MISSING: a `TIMESTAMPTZ` value is
  cast to `TIME` first, and a `TIMESTAMP` value is unsupported.
- `CAST(x AS STRING)` of a `DECIMAL` value with a scale above 6 uses an exponent.
- `CASE` and `COALESCE` have the common type of their values, but the values are not converted: the translation casts
  them.
- `SUM` of a `BIGINT` argument fails at run time; the translation is `CAST(SUM(CAST(x AS DECIMAL(38, 0))) AS BIGINT)`.
  `AVG` of a `SMALLINT` or `REAL` argument fails at run time; the translation uses `SUM(x) / COUNT(x)` and
  `AVG(CAST(x AS DOUBLE PRECISION))`.
- `SUM`, `AVG`, `MIN`, and `MAX` of a `DECIMAL` argument, `SUM` of a `BIGINT` argument, and `ABS` of a `DECIMAL`
  argument have the static type `DECIMAL(38, 19)` or `DECIMAL(39, 19)`, so arithmetic on a result with more digits
  fails: the translation casts the result.
- The planner does not find an aggregate call in the arguments of a scalar function call: `ABS(SUM(x))` fails with
  `FUNCTION_NOT_FOUND`, and such a call is unsupported. Operators, `CAST`, `CASE`, `COALESCE`, `TRIM`, and `POSITION`
  are not function calls. The probe calls of the operand types are function calls too, so the probe passes probe a
  copy of an operand in which each aggregate is a NULL of its type. Date arithmetic translates to `DATE_ADD` and
  `DATE_DIFF` calls, so `MIN(d) + 1` is unsupported too, and so is a subquery as an operand of date arithmetic or an
  argument of `DATEADD`, `DATEDIFF`, or `EXTRACT`.
- `EXCEPT` returns duplicate rows of the left query. The translation adds DISTINCT to a left query that is a simple
  query block, which has the same result.
- A set operation over SUPER values (the dynamic type) does not plan (`SET_OP_SCHEMA_INCOMPATIBLE`); the translation
  reports it as unsupported. A `CAST` of a `VARCHAR(n)` value to `VARCHAR(m)` returns MISSING, so a set operation
  whose queries have character columns of different lengths can return MISSING values.
- `INTERVAL + INTERVAL` raises an error when a field carries (`INTERVAL '12' HOUR + INTERVAL '13' HOUR`), and
  `TIMESTAMP - TIMESTAMP` keeps only milliseconds for differences of about 146 years or more.
- An index into `SPLIT(NULL, d)` fails with a `NullPointerException`; the translation of `SPLIT_PART` tests for NULL.
- `x IS FLOAT` is false for negative values and 0; `IS_FLOAT` uses `x IS DOUBLE PRECISION`.
- The Scribe PartiQL target prints `x IS DECIMAL(p, s)` and `x IS INTEGER` as `x IS DECIMAL` and `x IS INT`, so
  `IS_SMALLINT`, `IS_INTEGER`, and `IS_BIGINT` of a SUPER value are unsupported. It drops the sign of a negative
  interval shorter than one second, so such interval literals are unsupported.
- There are no sign operators for intervals: the translation of `-i` is `i * -1`. `CURRENT_DATE` is not implemented:
  the translation is `CAST(UTCNOW() AS DATE)`.
- The plan rewriter does not visit the expressions of ORDER BY; the probe passes visit them separately.
- A path index of the dynamic type gives the planner warning `PATH_INDEX_NEVER_SUCCEEDS`, and the types of the
  expressions in the index are lost; while it resolves operand types, the translator casts such an index to `INT`.

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
    file is also the list of gaps. A `MISMATCH` is often a fixture where the Scribe Redshift target does not keep the
    PartiQL semantics (for example PartiQL `DATE_DIFF` printed as Redshift `DATEDIFF`), so the translation back, which
    keeps the Redshift semantics, differs.

  Every Scribe fixture must be in exactly one set. After a change that moves fixtures between the sets, classify
  them again and review the change to both files:

  ```shell
  ./gradlew :partiql-scribe-redshift-frontend:test --tests '*RoundTripSuite*' -PupdateRoundTrip
  ```

```shell
./gradlew :partiql-scribe-redshift-frontend:test
```
