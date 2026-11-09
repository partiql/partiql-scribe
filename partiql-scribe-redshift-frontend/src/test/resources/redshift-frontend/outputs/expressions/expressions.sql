--#[arith]
SELECT "simple_t"['b'] + 1 AS "_1", "simple_t"['b'] - 1 AS "_2", "simple_t"['b'] * 2 AS "_3", "simple_t"['b'] / 2 AS "_4" FROM "default"."SIMPLE_T" AS "simple_t";

--#[arith-unary]
SELECT -"simple_t"['b'] AS "_1", +"simple_t"['b'] AS "_2", -("simple_t"['b'] + 1) AS "_3" FROM "default"."SIMPLE_T" AS "simple_t";

--#[arith-precedence]
SELECT ("simple_t"['b'] + (1 * 2)) - (3 / 4) AS "_1", ("simple_t"['b'] + 1) * 2 AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[arith-exponent]
SELECT "pow"(CAST("simple_t"['b'] AS DOUBLE PRECISION), CAST(2 AS DOUBLE PRECISION)) AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[bitwise-and]
SELECT "simple_t"['b'] & 1 AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[bitwise-and-precedence]
SELECT "simple_t"['b'] & ("simple_t"['b'] + 1) AS "_1", ("simple_t"['b'] & "simple_t"['b']) + 1 AS "_2", "simple_t"['b'] & ("simple_t"['b'] * 2) AS "_3" FROM "default"."SIMPLE_T" AS "simple_t";

--#[bitwise-and-chain]
SELECT ("simple_t"['b'] & 1) & 3 AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[bitwise-and-null]
SELECT "simple_t"['b'] & NULL AS "_1", NULL & 1 AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[bitwise-and-in-where]
SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t" WHERE ("simple_t"['b'] & 1) = 1;

--#[concat]
SELECT "simple_t"['c'] || 'x' AS "_1", ("simple_t"['c'] || "simple_t"['c']) || 'y' AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[concat-with-arith]
[PLANNER{message="FUNCTION_TYPE_MISMATCH"}];

--#[case-searched]
SELECT CASE WHEN "simple_t"['b'] > 0 THEN 'pos' WHEN "simple_t"['b'] < 0 THEN 'neg' ELSE 'zero' END AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[case-searched-no-else]
SELECT CASE WHEN "simple_t"['a'] THEN 1 ELSE NULL END AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[case-simple]
SELECT CASE WHEN "simple_t"['b'] = 1 THEN 'one' WHEN "simple_t"['b'] = 2 THEN 'two' ELSE 'many' END AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[case-nested]
SELECT CASE WHEN "simple_t"['a'] THEN CASE WHEN "simple_t"['b'] > 0 THEN 1 ELSE 2 END ELSE 3 END AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[cast]
SELECT CAST("simple_t"['b'] AS VARCHAR(10)) AS "b", CAST("simple_t"['c'] AS INT) AS "c", CAST("simple_t"['b'] AS DOUBLE PRECISION) AS "b" FROM "default"."SIMPLE_T" AS "simple_t";

--#[cast-shorthand]
SELECT CAST("simple_t"['b'] AS VARCHAR(256)) AS "b", CAST("simple_t"['c'] AS BIGINT) AS "c", CAST("simple_t"['b'] AS DECIMAL(10,2)) AS "b", CAST("simple_t"['b'] AS SMALLINT) AS "b" FROM "default"."SIMPLE_T" AS "simple_t";

--#[cast-shorthand-chain]
SELECT CAST(CAST("simple_t"['b'] AS INT) AS VARCHAR(256)) AS "b" FROM "default"."SIMPLE_T" AS "simple_t";

--#[cast-shorthand-precedence]
SELECT -CAST("simple_t"['b'] AS INT) AS "_1", "simple_t"['b'] + CAST(1 AS INT) AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[cast-types]
SELECT CAST("simple_t"['c'] AS VARCHAR(256)) AS "c", CAST("simple_t"['c'] AS STRING) AS "c", CAST("simple_t"['c'] AS CHAR(1)) AS "c", CAST("simple_t"['c'] AS CHAR(3)) AS "c", CAST("simple_t"['b'] AS DECIMAL(18,0)) AS "b", CAST("simple_t"['b'] AS REAL) AS "b", CAST("simple_t"['b'] AS DOUBLE PRECISION) AS "b", CAST("simple_t"['b'] AS BOOL) AS "b" FROM "default"."SIMPLE_T" AS "simple_t";

--#[cast-datetime]
SELECT CAST("simple_t"['c'] AS DATE) AS "c", CAST("simple_t"['c'] AS TIMESTAMP) AS "c" FROM "default"."SIMPLE_T" AS "simple_t";

--#[convert]
SELECT CAST("simple_t"['c'] AS INT) AS "c", CAST("simple_t"['b'] AS VARCHAR(10)) AS "b" FROM "default"."SIMPLE_T" AS "simple_t";

--#[literals]
SELECT 1 AS "_1", 1.5 AS "_2", 0.5 AS "_3", 'abc' AS "_4", 'it''s' AS "_5", true AS "_6", false AS "_7", NULL AS "_8" FROM "default"."SIMPLE_T" AS "simple_t";

--#[literal-string-backslash]
SELECT 'a''b' AS "_1", 'line
break' AS "_2", 'back\slash' AS "_3" FROM "default"."SIMPLE_T" AS "simple_t";

--#[literal-string-backslash-unknown]
[UNSUPPORTED_OPERATION{message="backslash escape `\q` in a string literal"}];

--#[literal-date]
SELECT DATE '2024-01-31' AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[literal-timestamp]
SELECT TIMESTAMP '2024-01-31 12:34:56' AS "_1", TIMESTAMP '2024-01-31 12:34:56.789' AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[literal-date-format]
[UNSUPPORTED_OPERATION{message="DATE literal format '01/31/2024'"}];

--#[literal-interval]
[UNSUPPORTED_OPERATION{message="INTERVAL literal"}];

--#[aggregates]
SELECT count(1) AS "_1", count("simple_t"['b']) AS "_2", count(DISTINCT "simple_t"['b']) AS "_3", sum("simple_t"['b']) AS "_4", avg("simple_t"['b']) AS "_5", max("simple_t"['c']) AS "_6", min("simple_t"['c']) AS "_7" FROM "default"."SIMPLE_T" AS "simple_t";

--#[aggregates-all]
SELECT sum("simple_t"['b']) AS "_1", count("simple_t"['c']) AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[aggregates-in-expressions]
SELECT sum("simple_t"['b']) / count(1) AS "_1", max("simple_t"['b']) - min("simple_t"['b']) AS "_2", CAST(avg("simple_t"['b']) AS INT) AS "_3" FROM "default"."SIMPLE_T" AS "simple_t";

--#[aggregates-case-insensitive]
SELECT count(1) AS "_1", sum("simple_t"['b']) AS "_2" FROM "default"."SIMPLE_T" AS "simple_t";

--#[names-nested-field]
SELECT "t"['d']['e'] AS "e" FROM "default"."T" AS "t";

--#[names-qualified-nested-field]
SELECT "t"['d']['e'] AS "e" FROM "default"."T" AS "t";

--#[parenthesized]
SELECT "simple_t"['b'] AS "b", "simple_t"['b'] + (1 * 2) AS "_1" FROM "default"."SIMPLE_T" AS "simple_t";

--#[cast-type-aliases]
SELECT CAST("simple_t"['b'] AS SMALLINT) AS "b", CAST("simple_t"['b'] AS INT) AS "b", CAST("simple_t"['b'] AS BIGINT) AS "b", CAST("simple_t"['b'] AS REAL) AS "b", CAST("simple_t"['b'] AS DOUBLE PRECISION) AS "b", CAST("simple_t"['c'] AS CHAR(256)) AS "c", CAST("simple_t"['c'] AS VARCHAR(5)) AS "c", CAST("simple_t"['c'] AS VARCHAR(6)) AS "c", CAST("simple_t"['c'] AS TIMESTAMP) AS "c" FROM "default"."SIMPLE_T" AS "simple_t";

--#[cast-type-bounds]
SELECT CAST("simple_t"['c'] AS VARCHAR(65535)) AS "c", CAST("simple_t"['c'] AS CHAR(4096)) AS "c", CAST("simple_t"['b'] AS DECIMAL(38,37)) AS "b" FROM "default"."SIMPLE_T" AS "simple_t";

--#[cast-varchar-too-long]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: VARCHAR length 65536 is out of range (1 to 65535)"}];

--#[cast-varchar-zero]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: VARCHAR length 0 is out of range (1 to 65535)"}];

--#[cast-decimal-scale-over-precision]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: DECIMAL scale 6 is out of range (0 to 5)"}];

--#[cast-length-not-an-int]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: VARCHAR length 99999999999 is out of range (1 to 65535)"}];

--#[case-sensitive-identifiers-off]
SELECT "simple_t"['c'] AS "mixed" FROM "default"."SIMPLE_T" AS "simple_t";

--#[numeric-literal-38-digits]
SELECT 1.0000000000000000000000000000000000001 AS "v" FROM "default"."SIMPLE_T" AS "simple_t";

--#[numeric-literal-39-digits]
[UNSUPPORTED_OPERATION{message="numeric literal with more than 38 digits"}];

--#[integer-literal-39-digits]
[UNSUPPORTED_OPERATION{message="numeric literal with more than 38 digits"}];
