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
- Comparisons, `= ANY`, `= SOME`, and `<> ALL` with a subquery, `[NOT] BETWEEN`, `[NOT] IN` with a list or a
  subquery, a row value `(a, b) [NOT] IN ((1, 2), ...)`, `IS [NOT] NULL`, `IS [NOT] TRUE`, `FALSE`, and `UNKNOWN`,
  `EXISTS`, `AND`, `OR`, `NOT`
- `[NOT] LIKE` and `~~`; `ILIKE` and `~~*` with an ASCII literal pattern; `SIMILAR TO` and POSIX `~` and `!~` with a
  literal pattern without regular expression metacharacters
- Arithmetic, signs, `@` (absolute value), `&` (bitwise AND), `||`, simple and searched `CASE`
- `CAST(x AS t)`, `x::t`, and `CONVERT(t, x)` for numeric, character, boolean, `DATE`, `TIME`, `TIMETZ`, `TIMESTAMP`,
  and `TIMESTAMPTZ` types, with the Redshift type names and aliases (`INT4`, `FLOAT8`, `FLOAT(p)`, `BPCHAR(n)`,
  `NVARCHAR(n)`, `CHARACTER VARYING(n)`, `VARCHAR(MAX)`, ...)
- String literals with the backslash escapes `\\`, `\'`, `\b`, `\f`, `\n`, `\r`, and `\t`
- Typed literals: `DATE`, `TIME`, `TIMETZ`, `TIMESTAMP`, and `TIMESTAMPTZ`, with a numeric time zone offset
- SUPER navigation: `t.a.b`, `t.a[0]`, `t.a[i + 1]`
- Functions: `NVL`, `COALESCE`, `NULLIF`, `LEN`, `LENGTH`, `UPPER`, `LOWER`, `CHAR_LENGTH`, `OCTET_LENGTH`, `ABS`,
  `COUNT`, `SUM`, `AVG`, `MIN`, `MAX`, `DATEADD`, `DATEDIFF`, `GETDATE`, `SYSDATE`, `CURRENT_DATE`
- `EXTRACT(part FROM x)` for the year, month, day, hour, minute, and second dateparts and their abbreviations

## Semantic rules

- Identifiers: by default, quoted and unquoted identifiers fold to lower case, as with the Redshift default
  `enable_case_sensitive_identifier = false`. Use `RedshiftTranspiler(caseSensitiveIdentifiers = true)` for sources
  that set it to true.
- `ORDER BY` always writes the null order: Redshift puts NULL values last for `ASC` and first for `DESC`.
- `VARCHAR` and `TEXT` without a length are `VARCHAR(256)`; `DECIMAL` without parameters is `DECIMAL(18, 0)`.
  Redshift `VARCHAR(n)` holds n bytes and PartiQL `VARCHAR(n)` n characters, so a `CAST` to `VARCHAR(n)`, or to
  `VARCHAR` without a length, that can truncate a character string is unsupported.
- Trailing blanks (deviation): Redshift ignores trailing blanks when it compares `VARCHAR` values and SUPER strings;
  the translation does not trim them, so `c = 'a '` is false in PartiQL for the value `'a'`. `CHAR(n)` values are
  trimmed where Redshift ignores their padding: comparisons, `IN`, `||`, and conversions to text; `LIKE` keeps the
  padding, as in Redshift. A `CAST` to `CHAR(n)` is supported only as a top-level SELECT item.
- String literals: Redshift reads backslash escapes. The translation decodes `\\`, `\'`, `\b`, `\f`, `\n`, `\r`, and
  `\t`, and reports other escapes as unsupported.
- Implicit conversions: Redshift converts a character string literal to the type of the other operand of a
  comparison, `IN`, `BETWEEN`, or simple `CASE`. The translation converts the literal at translation time when both
  readings agree (`col_date = '2020-01-01'` becomes `col_date = DATE '2020-01-01'`), and reports the comparison as
  unsupported otherwise. A comparison of a number with a character string that is not a literal is unsupported.
- Redshift compares and computes `REAL` with another number, and `DOUBLE PRECISION` with `DECIMAL`, in `DOUBLE
  PRECISION`; the translation casts the operands. A comparison with a literal that the floating-point type holds
  exactly needs no cast. `REAL` and `DOUBLE PRECISION` values are assumed not to be NaN.
- `DECIMAL`: with the default `enable_numeric_rounding = off`, a Redshift `CAST` that reduces the scale truncates a
  value from a table and rounds a constant. The translation truncates (`x - x % 1`), which assumes that values have
  the declared scale of their type, and a constant that needs rounding is unsupported. A `DECIMAL` division is
  translated only when its quotient is exact in both languages, because the result scales differ; the translation
  adds a note. PartiQL types `-x` and `ABS(x)` of a `DECIMAL(p, s)` value as a wider type, so the translation casts
  them back.
- `LIKE`: the default escape character is the backslash. A literal pattern is decoded and written again for PartiQL,
  with `ESCAPE '\'` where a literal `%`, `_`, or backslash needs it. A pattern that is not a literal gets
  `ESCAPE '\'` and a note: PartiQL raises an error for a backslash before a character other than `%`, `_`, or a
  backslash, which Redshift matches as that character. `ILIKE` ignores the case of ASCII letters only; the
  translation matches the lower-case value with an ASCII pattern.
- `CASE`: the results are cast to the result type of the CASE when their numeric or datetime types differ, because
  PartiQL computes that type but does not convert the results. A `WHEN NULL` branch is removed.
- A `DATE` converted to text (`CAST`, `||`) is written as `yyyy-mm-dd`, which matches Redshift for the years 0001 to
  9999.
- The type of an operand with an aggregate call is the Redshift result type: `COUNT` is `BIGINT`, `MIN` and `MAX`
  have the type of the argument, and `SUM` and `AVG` are `BIGINT` for integers, `DECIMAL(38, s)` for `DECIMAL(p, s)`,
  and `DOUBLE PRECISION` for floating-point numbers. A rule that needs the type of another aggregate expression reports
  it as unsupported.
- SUPER navigation: Redshift matches an attribute name case-sensitively after it folds the identifier, so the
  translation writes each attribute step as a delimited name with the ASCII letters folded (`t.x.Y` becomes
  `"t"['x']['y']`). A bracket step with a character string key is unsupported.
- A numeric literal with an exponent (`1e3`) is unsupported: its Redshift type is not documented.
- `GETDATE()` and `SYSDATE` become `UTCNOW()`, which returns `TIMESTAMP WITH TIME ZONE` in UTC.
- `TIME`, `TIMETZ`, and `TIMESTAMPTZ` types get precision 6, which is the Redshift precision. PartiQL `TIME` without
  a precision is `TIME(0)`.
- `EXTRACT(SECOND ...)` returns an INTEGER in Redshift for `TIMESTAMP` and `TIME` values, and a DECIMAL with the
  fraction in PartiQL. The translation adds a note.
- A `TIMESTAMP` or `TIME` literal with an offset, and a literal with a time zone name, are unsupported. Redshift
  ignores the offset of a `TIMESTAMP` literal; PartiQL accepts only numeric offsets.
- `GROUP BY 1` and `ORDER BY 1` are unsupported, because PartiQL reads the number as a constant, not a position.

## PLK 1.6.1 runtime gaps

The PartiQL 1.6.1 planner and evaluator differ from their own types or from the PartiQL and SQL specifications in
these cases. The translation avoids them as noted, or reports the construct as unsupported.

- LIKE `%` and `_` do not match line terminators. The translation does not work around this.
- A `CAST` of a character string to `DATE`, `TIME`, `TIMETZ`, `TIMESTAMP`, or `TIMESTAMPTZ` evaluates to MISSING, and
  a `CAST` to `DECIMAL`, `REAL`, or `DOUBLE PRECISION` fails for strings such as `'0.5'`. String literals are
  converted at translation time; other strings are unsupported.
- A `CAST` of a character string to an integer type fails for some white space around the number (`'0 '`,
  `' 012'`): the translation trims it.
- A `VARCHAR(n)` or `CHAR(n)` value can only be cast to `STRING`: the translation casts it to `STRING` first.
- A `CAST` of `DOUBLE PRECISION` to `REAL` evaluates to MISSING for values that are not positive: unsupported.
- A `CAST` of `TIMESTAMP` or `TIMESTAMPTZ` to `TIME WITH TIME ZONE` evaluates to MISSING: a `TIMESTAMPTZ` value is
  cast to `TIME` first, and a `TIMESTAMP` value is unsupported.
- `CASE` does not convert its results to its result type: the translation casts them.
- An aggregate call in the argument of a function, such as `ABS(MAX(b))`, is not planned (`FUNCTION_NOT_FOUND`);
  operators, `CAST`, `CASE`, `COALESCE`, `TRIM`, and `POSITION` work. `@` and `ILIKE` on an aggregate expression are
  unsupported.
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
    file is also the list of gaps.

  Every Scribe fixture must be in exactly one set. After a change that moves fixtures between the sets, classify
  them again and review the change to both files:

  ```shell
  ./gradlew :partiql-scribe-redshift-frontend:test --tests '*RoundTripSuite*' -PupdateRoundTrip
  ```

```shell
./gradlew :partiql-scribe-redshift-frontend:test
```
