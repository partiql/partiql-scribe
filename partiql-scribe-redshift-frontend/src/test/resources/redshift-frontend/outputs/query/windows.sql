--#[row-number]
SELECT "t"['c'] AS "c", ROW_NUMBER() OVER (PARTITION BY "t"."c" ORDER BY "t"['b'] ASC NULLS LAST) AS "rn" FROM "default"."T" AS "t";

--#[rank]
SELECT "t"['b'] AS "b", RANK() OVER (ORDER BY "t"['b'] DESC NULLS FIRST) AS "r" FROM "default"."T" AS "t";

--#[rank-without-order-by]
SELECT "t"['b'] AS "b", RANK() OVER () AS "r" FROM "default"."T" AS "t";

--#[dense-rank]
SELECT "t"['b'] AS "b", DENSE_RANK() OVER (PARTITION BY "t"."c" ORDER BY "t"['b'] ASC NULLS LAST) AS "r" FROM "default"."T" AS "t";

--#[lag-lead]
SELECT "t"['b'] AS "b", LAG("t"['b'], 1, NULL) RESPECT NULLS OVER (ORDER BY "t"['timestamp_1'] ASC NULLS LAST) AS "prev", LEAD("t"['b'], 2, NULL) RESPECT NULLS OVER (PARTITION BY "t"."c" ORDER BY "t"['timestamp_1'] DESC NULLS FIRST) AS "nxt" FROM "default"."T" AS "t";

--#[lag-offset-zero-respect-nulls]
SELECT "t"['b'] AS "b", LAG("t"['b'], 0, NULL) RESPECT NULLS OVER (ORDER BY "t"['b'] ASC NULLS LAST) AS "cur" FROM "default"."T" AS "t";

--#[lag-offset-expression]
[UNSUPPORTED{message="LAG offset that is not a non-negative integer literal"}];

--#[lag-default]
[UNSUPPORTED{message="LAG with a default value"}];

--#[lag-without-order-by]
[UNSUPPORTED{message="LAG without ORDER BY"}];

--#[ignore-nulls]
[UNSUPPORTED{message="IGNORE NULLS"}];

--#[partition-by-super-path]
SELECT ROW_NUMBER() OVER (PARTITION BY "t"."d"."e" ORDER BY "t"['b'] ASC NULLS LAST) AS "r" FROM "default"."T" AS "t";

--#[partition-by-columns]
SELECT "t"['b'] AS "b", LEAD("t"['b'], 1, NULL) RESPECT NULLS OVER (PARTITION BY "t"."c", "t"."z" ORDER BY "t"['timestamp_1'] ASC NULLS LAST, "t"['b'] DESC NULLS FIRST) AS "nxt" FROM "default"."T" AS "t";

-- --#[partition-by-expression] TODO(redshift)
[UNSUPPORTED{message="PARTITION BY expression"}];

--#[order-by-nulls]
SELECT "t"['b'] AS "b", ROW_NUMBER() OVER (ORDER BY "t"['b'] DESC NULLS LAST) AS "r1", RANK() OVER (ORDER BY "t"['c'] ASC NULLS LAST) AS "r2" FROM "default"."T" AS "t";

--#[order-by-constant]
[INVALID_INPUT{message="constant in a window ORDER BY"}];

--#[running-count]
SELECT "t"['c'] AS "c", ROW_NUMBER() OVER (PARTITION BY "t"."c" ORDER BY "t"['b'] ASC NULLS LAST) AS "running_n" FROM "default"."T" AS "t";

--#[running-sum-of-one]
SELECT "t"['c'] AS "c", ROW_NUMBER() OVER (PARTITION BY "t"."c" ORDER BY "t"['b'] ASC NULLS LAST) AS "running_n" FROM "default"."T" AS "t";

--#[running-sum]
[UNSUPPORTED{message="window frame"}];

--#[aggregate-window]
[UNSUPPORTED{message="aggregate window function `SUM`"}];

--#[count-star-window]
[UNSUPPORTED{message="aggregate window function `COUNT`"}];

--#[percent-rank]
[UNSUPPORTED{message="window function `PERCENT_RANK`"}];

--#[ntile]
[UNSUPPORTED{message="window function `NTILE`"}];

--#[first-value]
[UNSUPPORTED{message="window function `FIRST_VALUE`"}];

--#[ratio-to-report]
[UNSUPPORTED{message="window function `RATIO_TO_REPORT`"}];

--#[median-window]
[UNSUPPORTED{message="aggregate window function `MEDIAN`"}];

--#[order-by-window-function]
SELECT "$w"['b'] AS "b" FROM (SELECT "t"['b'] AS "b", ROW_NUMBER() OVER (ORDER BY "t"['b'] ASC NULLS LAST) AS "$o" FROM "default"."T" AS "t") AS "$w" ORDER BY "$w"['$o'] ASC NULLS LAST;

--#[order-by-window-alias]
SELECT "$w"['b'] AS "b", "$w"['rn'] AS "rn" FROM (SELECT "t"['b'] AS "b", ROW_NUMBER() OVER (ORDER BY "t"['b'] ASC NULLS LAST) AS "rn" FROM "default"."T" AS "t") AS "$w" ORDER BY "$w"['rn'] DESC NULLS FIRST;

--#[order-by-window-alias-limit]
SELECT "$w"['b'] AS "b", "$w"['rn'] AS "rn" FROM (SELECT "t"['b'] AS "b", ROW_NUMBER() OVER (ORDER BY "t"['b'] ASC NULLS LAST) AS "rn" FROM "default"."T" AS "t") AS "$w" ORDER BY "$w"['rn'] DESC NULLS FIRST LIMIT 2;

--#[order-by-window-alias-expression]
[UNSUPPORTED{message="ORDER BY expression with the alias `rn` of a window function"}];

--#[window-in-expression]
SELECT "t"['b'] AS "b", ROW_NUMBER() OVER (ORDER BY "t"['b'] ASC NULLS LAST) - CAST(1 AS BIGINT) AS "r0", CASE WHEN ROW_NUMBER() OVER (PARTITION BY "t"."c" ORDER BY "t"['b'] ASC NULLS LAST) = 1 THEN 'first' ELSE 'other' END AS "f" FROM "default"."T" AS "t";

--#[window-over-grouped-query]
SELECT "t"['c'] AS "c", count(1) AS "n", RANK() OVER (ORDER BY count(1) DESC NULLS FIRST) AS "r" FROM "default"."T" AS "t" GROUP BY "t"['c'];

--#[window-over-group-key]
SELECT "t"['c'] AS "c", RANK() OVER (ORDER BY "t"['c'] ASC NULLS LAST) AS "r" FROM "default"."T" AS "t" GROUP BY "t"['c'];

--#[window-lateral-alias]
[UNSUPPORTED{message="reference to the alias `rn` of a window function in the select list"}];

--#[window-in-where]
[INVALID_INPUT{message="window function in WHERE"}];

--#[named-window]
[INVALID_INPUT{message="mismatched input 'w' expecting {<EOF>, ';'}"}];
