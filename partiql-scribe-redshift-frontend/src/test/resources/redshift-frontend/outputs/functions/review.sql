-- --#[review-cte-column-00] TODO(redshift)
SELECT CHAR_LENGTH(CAST("x"['n'] AS STRING)) AS "l" FROM (SELECT "t"['b'] * 2 AS "n" FROM "default"."T" AS "t") AS "x";

-- --#[review-cte-column-01] TODO(redshift)
SELECT SUBSTRING(CAST("s"['d'] AS STRING) FROM 1 FOR 7) AS "ym" FROM (SELECT CAST("t"['timestamp_1'] AS DATE) AS "d" FROM "default"."T" AS "t") AS "s";

-- --#[review-cte-column-02] TODO(redshift)
SELECT CAST("s"['y'] AS STRING) || '-Q' AS "l" FROM (SELECT EXTRACT(YEAR FROM "t"['timestamp_1']) AS "y" FROM "default"."T" AS "t") AS "s";

-- --#[review-array-subscript-00] TODO(redshift)
SELECT [COALESCE("t"['col_list'][5]), COALESCE("t"['col_list'][0])] AS "a" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[review-array-subscript-01] TODO(redshift)
SELECT COALESCE("t"['col_list'][5] + 1) AS "a" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[review-real-aggregate-00] TODO(redshift)
[UNSUPPORTED{message="operator on an expression with an aggregate of a REAL value"}];

-- --#[review-real-aggregate-01] TODO(redshift)
[UNSUPPORTED{message="operator on an expression with an aggregate of a REAL value"}];
