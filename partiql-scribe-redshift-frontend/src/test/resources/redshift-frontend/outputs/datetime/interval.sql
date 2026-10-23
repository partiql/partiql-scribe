-- --#[interval-qualified-00] TODO(redshift)
SELECT INTERVAL '1 2:3:4.500000' DAY (9) TO SECOND (6) AS "v" FROM "default"."T" AS "t";

-- --#[interval-qualified-01] TODO(redshift)
SELECT INTERVAL '100' DAY (9) AS "v" FROM "default"."T" AS "t";

-- --#[interval-qualified-02] TODO(redshift)
SELECT INTERVAL '10.234' SECOND (9, 3) AS "v" FROM "default"."T" AS "t";

-- --#[interval-qualified-03] TODO(redshift)
SELECT INTERVAL '-10-3' YEAR (9) TO MONTH AS "v" FROM "default"."T" AS "t";

-- --#[interval-qualified-04] TODO(redshift)
SELECT INTERVAL '-10 3:4:5.000000' DAY (9) TO SECOND (6) AS "v" FROM "default"."T" AS "t";

-- --#[interval-qualified-05] TODO(redshift)
[UNSUPPORTED{message="INTERVAL literal '-10 3:4:5' DAY TO SECOND"}];

-- --#[interval-qualified-06] TODO(redshift)
SELECT INTERVAL '-30' MINUTE (9) AS "v" FROM "default"."T" AS "t";

-- --#[interval-qualified-07] TODO(redshift)
[UNSUPPORTED{message="negative INTERVAL literal '-0.5' shorter than one second"}];

-- --#[interval-qualified-08] TODO(redshift)
[UNSUPPORTED{message="INTERVAL literal '70:70.123' MINUTE TO SECOND"}];

-- --#[interval-qualified-09] TODO(redshift)
[UNSUPPORTED{message="INTERVAL literal '1.234567' SECOND(3)"}];

-- --#[interval-qualified-10] TODO(redshift)
[UNSUPPORTED{message="INTERVAL literal '5 HOURS' HOUR"}];

-- --#[interval-qualified-11] TODO(redshift)
[UNSUPPORTED{message="INTERVAL qualifier `DAY(3)`"}];

-- --#[interval-qualified-12] TODO(redshift)
[UNSUPPORTED{message="INTERVAL qualifier `YEAR TO DAY`"}];

-- --#[interval-qualified-13] TODO(redshift)
[UNSUPPORTED{message="INTERVAL qualifier `SECOND(7)`"}];

-- --#[interval-qualified-14] TODO(redshift)
SELECT INTERVAL '25:0' HOUR (9) TO MINUTE AS "v1", INTERVAL '5' DAY (9) AS "v2" FROM "default"."T" AS "t";

-- --#[interval-unqualified-00] TODO(redshift)
SELECT INTERVAL '7' DAY (9) AS "v" FROM "default"."T" AS "t";

-- --#[interval-unqualified-01] TODO(redshift)
SELECT "t"['timestamp_1'] + INTERVAL '7 1:1:1.000000' DAY (9) TO SECOND (6) AS "v" FROM "default"."T" AS "t";

-- --#[interval-unqualified-02] TODO(redshift)
SELECT "t"['timestamp_1'] - INTERVAL '4' MONTH (9) AS "v" FROM "default"."T" AS "t";

-- --#[interval-unqualified-03] TODO(redshift)
SELECT INTERVAL '2' YEAR (9) AS "v", INTERVAL '14' MONTH (9) AS "w" FROM "default"."T" AS "t";

-- --#[interval-unqualified-04] TODO(redshift)
SELECT "t"['timestamp_1'] + INTERVAL '-30' MINUTE (9) AS "v" FROM "default"."T" AS "t";

-- --#[interval-unqualified-05] TODO(redshift)
SELECT CAST("t"['col_date'] AS TIMESTAMP) + INTERVAL '12' HOUR (9) AS "v" FROM "default"."T_ALL_TYPES" AS "t";

-- --#[interval-unqualified-06] TODO(redshift)
SELECT "t"['timestamp_1'] + INTERVAL '1 12' DAY (9) TO HOUR AS "v" FROM "default"."T" AS "t";

-- --#[interval-unqualified-07] TODO(redshift)
[UNSUPPORTED{message="INTERVAL literal '36 hours' outside of datetime arithmetic (Redshift does not normalize its fields)"}];

-- --#[interval-unqualified-08] TODO(redshift)
[UNSUPPORTED{message="INTERVAL literal '1 month 2 days'"}];

-- --#[interval-type-00] TODO(redshift)
[UNSUPPORTED{message="data type `interval day to second`"}];

-- --#[interval-type-01] TODO(redshift)
[UNSUPPORTED{message="data type `INTERVAL`"}];
