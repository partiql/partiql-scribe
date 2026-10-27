-- --#[math-abs-00] TODO(redshift)
SELECT CAST(ABS("t"['foo']['keep']) AS DECIMAL(5,2)) AS "v" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

-- --#[math-abs-01] TODO(redshift)
SELECT ABS("t"['b']) AS "v" FROM "default"."T" AS "t";

-- --#[math-abs-02] TODO(redshift)
[UNSUPPORTED{message="`ABS` of a value of type SUPER"}];

-- --#[math-mod-00] TODO(redshift)
SELECT "t"['b'] % 3 AS "v" FROM "default"."T" AS "t";

-- --#[math-mod-01] TODO(redshift)
SELECT "t"['foo']['keep'] % CAST(2 AS DECIMAL(10,0)) AS "v" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

-- --#[math-mod-02] TODO(redshift)
[UNSUPPORTED{message="`MOD` with a floating-point argument"}];

-- --#[math-round-00] TODO(redshift)
SELECT CAST("t"['foo']['keep'] AS DECIMAL(38,1)) AS "v1", CAST("t"['foo']['keep'] AS DECIMAL(38,0)) AS "v2", "t"['foo']['keep'] AS "v3" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

-- --#[math-round-01] TODO(redshift)
SELECT CASE WHEN ("t"['foo']['keep'] % CAST(10 AS DECIMAL(10,0))) >= 5 THEN CAST("t"['foo']['keep'] - ("t"['foo']['keep'] % CAST(10 AS DECIMAL(10,0))) AS DECIMAL(38,0)) + CAST(10 AS DECIMAL(10,0)) WHEN ("t"['foo']['keep'] % CAST(10 AS DECIMAL(10,0))) <= -5 THEN CAST("t"['foo']['keep'] - ("t"['foo']['keep'] % CAST(10 AS DECIMAL(10,0))) AS DECIMAL(38,0)) - CAST(10 AS DECIMAL(10,0)) ELSE CAST("t"['foo']['keep'] - ("t"['foo']['keep'] % CAST(10 AS DECIMAL(10,0))) AS DECIMAL(38,0)) END AS "v" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

-- --#[math-round-02] TODO(redshift)
SELECT "t"['b'] AS "v" FROM "default"."T" AS "t";

-- --#[math-round-03] TODO(redshift)
[UNSUPPORTED{message="`ROUND` of a floating-point value (Redshift does not document how it rounds ties)"}];

-- --#[math-round-04] TODO(redshift)
[UNSUPPORTED{message="`ROUND` of a BIGINT value (Redshift does not document the conversion)"}];

-- --#[math-round-05] TODO(redshift)
[UNSUPPORTED{message="`ROUND` with a non-literal number of digits"}];

-- --#[math-floor-00] TODO(redshift)
SELECT CASE WHEN ("t"['foo']['keep'] % CAST(1 AS DECIMAL(10,0))) < 0 THEN CAST("t"['foo']['keep'] - ("t"['foo']['keep'] % CAST(1 AS DECIMAL(10,0))) AS DECIMAL(38,0)) - CAST(1 AS DECIMAL(10,0)) ELSE CAST("t"['foo']['keep'] - ("t"['foo']['keep'] % CAST(1 AS DECIMAL(10,0))) AS DECIMAL(38,0)) END AS "v" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

-- --#[math-floor-01] TODO(redshift)
SELECT CASE WHEN ("t"['col_float64'] - "t"['col_float64']) = 0 THEN CASE WHEN ("t"['col_float64'] % CAST(1 AS DOUBLE PRECISION)) < CAST(0 AS DOUBLE PRECISION) THEN ("t"['col_float64'] - ("t"['col_float64'] % CAST(1 AS DOUBLE PRECISION))) - CAST(1 AS DOUBLE PRECISION) ELSE "t"['col_float64'] - ("t"['col_float64'] % CAST(1 AS DOUBLE PRECISION)) END ELSE "t"['col_float64'] END AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[math-floor-02] TODO(redshift)
SELECT "t"['b'] AS "v" FROM "default"."T" AS "t";

-- --#[math-floor-03] TODO(redshift)
[UNSUPPORTED{message="`FLOOR` of a value of type SUPER"}];

-- --#[math-ceil-00] TODO(redshift)
SELECT CASE WHEN ("t"['foo']['keep'] % CAST(1 AS DECIMAL(10,0))) > 0 THEN CAST("t"['foo']['keep'] - ("t"['foo']['keep'] % CAST(1 AS DECIMAL(10,0))) AS DECIMAL(38,0)) + CAST(1 AS DECIMAL(10,0)) ELSE CAST("t"['foo']['keep'] - ("t"['foo']['keep'] % CAST(1 AS DECIMAL(10,0))) AS DECIMAL(38,0)) END AS "v" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

-- --#[math-ceil-01] TODO(redshift)
SELECT CASE WHEN ("t"['col_float64'] - "t"['col_float64']) = 0 THEN CASE WHEN ("t"['col_float64'] % CAST(1 AS DOUBLE PRECISION)) > CAST(0 AS DOUBLE PRECISION) THEN ("t"['col_float64'] - ("t"['col_float64'] % CAST(1 AS DOUBLE PRECISION))) + CAST(1 AS DOUBLE PRECISION) ELSE "t"['col_float64'] - ("t"['col_float64'] % CAST(1 AS DOUBLE PRECISION)) END ELSE "t"['col_float64'] END AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[math-trunc-00] TODO(redshift)
SELECT CAST("t"['foo']['keep'] - ("t"['foo']['keep'] % 0.1) AS DECIMAL(38,1)) AS "v1", CAST("t"['foo']['keep'] - ("t"['foo']['keep'] % CAST(1 AS DECIMAL(10,0))) AS DECIMAL(38,0)) AS "v2", CAST("t"['foo']['keep'] - ("t"['foo']['keep'] % CAST(10 AS DECIMAL(10,0))) AS DECIMAL(38,0)) AS "v3" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

-- --#[math-trunc-01] TODO(redshift)
SELECT CASE WHEN ("t"['col_float64'] - "t"['col_float64']) = 0 THEN "t"['col_float64'] - ("t"['col_float64'] % CAST(1 AS DOUBLE PRECISION)) ELSE "t"['col_float64'] END AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[math-trunc-02] TODO(redshift)
SELECT "t"['b'] AS "v1", "t"['b'] - ("t"['b'] % 10) AS "v2" FROM "default"."T" AS "t";

-- --#[math-trunc-03] TODO(redshift)
[UNSUPPORTED{message="`TRUNC` of a BIGINT value (Redshift does not document the conversion)"}];

-- --#[math-trunc-04] TODO(redshift)
[UNSUPPORTED{message="`TRUNC` of a floating-point value with digits"}];

-- --#[math-trunc-05] TODO(redshift)
SELECT CAST("t"['timestamp_1'] AS DATE) AS "v" FROM "default"."T" AS "t";

-- --#[math-trunc-06] TODO(redshift)
SELECT DATE_ADD(DAY, DATE_DIFF(DAY, CAST(TIMESTAMP '0001-01-01 00:00:00' AS TIMESTAMP WITH TIME ZONE), "t"['col_timestampz']), DATE '0001-01-01') AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[math-sign-00] TODO(redshift)
SELECT CASE WHEN "r"['b'] > 0 THEN 1 WHEN "r"['b'] < 0 THEN -1 WHEN "r"['b'] = 0 THEN 0 ELSE NULL END AS "v1", CASE WHEN "t"['foo']['keep'] > 0 THEN 1 WHEN "t"['foo']['keep'] < 0 THEN -1 WHEN "t"['foo']['keep'] = 0 THEN 0 ELSE NULL END AS "v2" FROM "default"."T" AS "r" INNER JOIN "default"."datatypes"."T_DECIMAL_5_2" AS "t" ON true;

-- --#[math-sign-01] TODO(redshift)
SELECT CASE WHEN "t"['col_float64'] > CAST(0 AS DOUBLE PRECISION) THEN 1 WHEN "t"['col_float64'] < CAST(0 AS DOUBLE PRECISION) THEN -1 WHEN "t"['col_float64'] = 0 THEN 0 ELSE NULL END AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[math-pi-00] TODO(redshift)
SELECT CAST(3.141592653589793 AS DOUBLE PRECISION) AS "v" FROM "default"."T" AS "t";

-- --#[math-power-00] TODO(redshift)
SELECT CAST(1024 AS DOUBLE PRECISION) AS "v1", CAST(0.5 AS DOUBLE PRECISION) AS "v2", CAST(6.25 AS DOUBLE PRECISION) AS "v3" FROM "default"."T" AS "t";

-- --#[math-power-01] TODO(redshift)
[UNSUPPORTED{message="`POWER` with a non-literal argument"}];

-- --#[math-power-02] TODO(redshift)
[UNSUPPORTED{message="`POWER` whose result is not an exact DOUBLE PRECISION value"}];

-- --#[math-power-03] TODO(redshift)
[UNSUPPORTED{message="`POWER` whose result is not an exact DOUBLE PRECISION value"}];

-- --#[math-unsupported-00] TODO(redshift)
[UNSUPPORTED{message="function `SQRT`; PartiQL has no equivalent function"}];

-- --#[math-unsupported-01] TODO(redshift)
[UNSUPPORTED{message="function `EXP`; PartiQL has no equivalent function"}];

-- --#[math-unsupported-02] TODO(redshift)
[UNSUPPORTED{message="function `LN`; PartiQL has no equivalent function"}];

-- --#[math-unsupported-03] TODO(redshift)
[UNSUPPORTED{message="function `LOG`; PartiQL has no equivalent function"}];

-- --#[math-unsupported-04] TODO(redshift)
[UNSUPPORTED{message="function `ATAN2`; PartiQL has no equivalent function"}];

-- --#[math-unsupported-05] TODO(redshift)
[UNSUPPORTED{message="function `RANDOM`; PartiQL has no random numbers"}];

-- --#[math-unsupported-06] TODO(redshift)
[UNSUPPORTED{message="function `DEGREES`; the Redshift formula is not documented"}];
