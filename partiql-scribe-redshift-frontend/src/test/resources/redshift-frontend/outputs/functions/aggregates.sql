--#[agg-count-00]
SELECT "t"['c'] AS "c", count(1) AS "n", count("t"['z']) AS "nz", count(DISTINCT "t"['z']) AS "dz" FROM "default"."T" AS "t" GROUP BY "t"['c'];

--#[agg-sum-00]
SELECT sum("t_all_types"['col_int16']) AS "s16", sum("t_all_types"['col_int32']) AS "s32", CAST(sum(CAST("t_all_types"['col_int64'] AS DECIMAL(38,0))) AS BIGINT) AS "s64", sum(CAST("t_all_types"['col_float32'] AS DOUBLE PRECISION)) AS "sf32", sum("t_all_types"['col_float64']) AS "sf64" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[agg-sum-01]
SELECT CAST(sum("t_decimals"['de']) AS DECIMAL(38,0)) AS "s" FROM "default"."T_DECIMALS" AS "t_decimals";

--#[agg-sum-02]
SELECT sum(DISTINCT "t"['b']) AS "s" FROM "default"."T" AS "t";

--#[agg-sum-03]
SELECT CAST(sum("t"['foo']['keep'] * CAST(2 AS DECIMAL(10,0))) AS DECIMAL(38,2)) AS "s" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

--#[agg-sum-04]
[UNSUPPORTED{message="SUM of a DECIMAL quotient (the scales differ)"}];

--#[agg-sum-05]
[UNSUPPORTED{message="SUM of an argument of type SUPER"}];

--#[agg-avg-00]
SELECT sum("t"['b']) / count("t"['b']) AS "v" FROM "default"."T" AS "t";

--#[agg-avg-01]
SELECT "t"['c'] AS "c", sum(DISTINCT "t"['b']) / count(DISTINCT "t"['b']) AS "v" FROM "default"."T" AS "t" GROUP BY "t"['c'];

--#[agg-avg-02]
SELECT sum("t_all_types"['col_int16']) / count("t_all_types"['col_int16']) AS "v16", CAST(avg("t_all_types"['col_int64']) - (avg("t_all_types"['col_int64']) % CAST(1 AS DECIMAL(10,0))) AS BIGINT) AS "v64" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[agg-avg-03]
SELECT avg(CAST("t_all_types"['col_float32'] AS DOUBLE PRECISION)) AS "v1", avg("t_all_types"['col_float64']) AS "v2" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[agg-avg-04]
[UNSUPPORTED{message="AVG of a DECIMAL argument (Redshift returns the scale of the argument)"}];

--#[agg-avg-05]
[UNSUPPORTED{message="AVG of an argument of type SUPER"}];

--#[agg-min-max-00]
SELECT min("t"['b']) AS "lo", max("t"['timestamp_1']) AS "hi", max(DISTINCT "t"['c']) AS "mc" FROM "default"."T" AS "t";

--#[agg-min-max-01]
SELECT CAST(min("t"['foo']['keep']) AS DECIMAL(5,2)) AS "lo", CAST(max("t"['foo']['keep']) AS DECIMAL(5,2)) AS "hi" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

--#[agg-min-max-02]
SELECT max(TRIM(TRAILING FROM "t_all_types"['col_char'])) AS "m" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[agg-min-max-03]
[UNSUPPORTED{message="MAX of a BOOLEAN argument"}];

--#[agg-min-max-04]
[UNSUPPORTED{message="MIN of an argument of type SUPER (the order of SUPER values is not documented)"}];

--#[agg-any-value-00]
SELECT "t"['c'] AS "c", min("t"['z']) AS "z" FROM "default"."T" AS "t" GROUP BY "t"['c'];

--#[agg-bool-00]
SELECT "t"['c'] AS "c", every("t"['a']) AS "all_a", some("t"['a']) AS "any_a" FROM "default"."T" AS "t" GROUP BY "t"['c'];

--#[agg-bool-01]
SELECT "t"['c'] AS "c", some("t"['b'] > 5) AS "big" FROM "default"."T" AS "t" GROUP BY "t"['c'];

--#[agg-bool-02]
[UNSUPPORTED{message="BOOL_AND of an argument of type INTEGER; the argument must be BOOLEAN"}];

--#[agg-having-00]
SELECT "t"['c'] AS "c" FROM "default"."T" AS "t" GROUP BY "t"['c'] HAVING sum("t"['b']) > CAST(10 AS BIGINT);

--#[agg-nested-02]
[UNSUPPORTED{message="`ABS` of an aggregate function (the PartiQL planner does not accept an aggregate in a function argument)"}];

--#[agg-nested-03]
SELECT "t"['c'] AS "c", sum("t"['b']) + CAST(1 AS BIGINT) AS "v1", (sum("t"['b']) / count("t"['b'])) - CAST(1 AS BIGINT) AS "v2" FROM "default"."T" AS "t" GROUP BY "t"['c'];

--#[agg-nested-04]
SELECT max("t"['b']) - min("t"['b']) AS "v" FROM "default"."T" AS "t";

--#[agg-unsupported-00]
[UNSUPPORTED{message="function `LISTAGG`; PartiQL has no string aggregation"}];

--#[agg-unsupported-01]
[UNSUPPORTED{message="function `MEDIAN`; PartiQL has no inverse distribution aggregation"}];

--#[agg-unsupported-02]
[UNSUPPORTED{message="function `STDDEV_SAMP`; PartiQL has no square root or statistical aggregation"}];

--#[agg-unsupported-03]
[UNSUPPORTED{message="function `VAR_POP`; PartiQL has no square root or statistical aggregation"}];

--#[agg-unsupported-04]
[UNSUPPORTED{message="function `BIT_AND`; PartiQL has no bit-wise aggregation"}];

--#[agg-unsupported-05]
[UNSUPPORTED{message="function `HLL`; PartiQL has no HyperLogLog sketches"}];
