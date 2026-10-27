-- --#[in-list-00] TODO(redshift)
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['b'] NOT IN (1, NULL);

-- --#[in-list-01] TODO(redshift)
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE "t_all_types"['col_int32'] IN (1, 2);

-- --#[in-list-02] TODO(redshift)
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE "t_all_types"['col_date'] IN (DATE '2020-01-01', DATE '2020-01-02');

-- --#[in-list-03] TODO(redshift)
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE TRIM(TRAILING FROM "t_all_types"['col_char']) IN ('a', 'b');

-- --#[in-list-04] TODO(redshift)
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE "t_all_types"['col_float64'] IN (CAST(0.1 AS DOUBLE PRECISION), 1);

-- --#[in-list-05] TODO(redshift)
[UNSUPPORTED{message="comparison of a number with a character string"}];

-- --#[in-list-06] TODO(redshift)
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE (CAST("t_all_types"['col_float32'] AS DOUBLE PRECISION) = CAST(0.1 AS DOUBLE PRECISION)) OR ("t_all_types"['col_float32'] = "t_all_types"['col_float32']);

-- --#[in-list-07] TODO(redshift)
[UNSUPPORTED{message="subquery as a value of an IN list"}];

-- --#[in-query-00] TODO(redshift)
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['b'] IN (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t");

-- --#[in-query-01] TODO(redshift)
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE TRIM(TRAILING FROM "t_all_types"['col_char']) NOT IN (SELECT "t_all_types"['col_string'] AS "col_string" FROM "default"."T_ALL_TYPES" AS "t_all_types");

-- --#[in-query-02] TODO(redshift)
[UNSUPPORTED{message="comparison of a number with a character string"}];

-- --#[in-query-03] TODO(redshift)
[UNSUPPORTED{message="IN subquery whose values Redshift converts (VARCHAR with CHAR)"}];

-- --#[quantified-00] TODO(redshift)
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['b'] IN (SELECT "s"['b'] AS "b" FROM "default"."T" AS "s" WHERE "s"['b'] > 1);

-- --#[quantified-01] TODO(redshift)
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['b'] IN (SELECT "t"['b'] AS "b" FROM "default"."T" AS "t");

-- --#[quantified-02] TODO(redshift)
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['b'] NOT IN (SELECT "s"['b'] AS "b" FROM "default"."T" AS "s");

-- --#[quantified-03] TODO(redshift)
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['b'] NOT IN (SELECT "s"['b'] AS "b" FROM "default"."T" AS "s");

-- --#[quantified-04] TODO(redshift)
[UNSUPPORTED{message="quantified comparison `> ANY`"}];

-- --#[quantified-05] TODO(redshift)
[UNSUPPORTED{message="quantified comparison `= ALL`"}];

-- --#[row-in-00] TODO(redshift)
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE (("t"['b'] = 1) AND ("t"['timestamp_1'] = TIMESTAMP '2020-01-01 00:00:00')) OR (("t"['b'] = 2) AND ("t"['timestamp_1'] = TIMESTAMP '2021-01-01 00:00:00'));

-- --#[row-in-01] TODO(redshift)
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE NOT(("t"['b'] = 1) AND ("t"['c'] = 'x'));

-- --#[row-in-02] TODO(redshift)
[INVALID_INPUT{message="row value with 3 values in IN"}];

--#[row-in-03]
[UNSUPPORTED{message="row value IN subquery"}];

-- --#[between-00] TODO(redshift)
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['b'] NOT BETWEEN -5 AND 10;

-- --#[between-01] TODO(redshift)
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE NOT(("t"['timestamp_1'] >= "t"['timestamp_2']) AND ("t"['timestamp_1'] <= TIMESTAMP '2030-01-01 00:00:00'));

-- --#[between-02] TODO(redshift)
SELECT ("t"['b'] >= NULL) AND ("t"['b'] <= 3) AS "v" FROM "default"."T" AS "t";

-- --#[between-03] TODO(redshift)
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE "t_all_types"['col_date'] BETWEEN DATE '2020-01-01' AND DATE '2020-12-31';

-- --#[between-04] TODO(redshift)
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE "t_all_types"['col_float64'] BETWEEN 0 AND CAST(0.1 AS DOUBLE PRECISION);

-- --#[between-05] TODO(redshift)
[UNSUPPORTED{message="ordering comparison of an integer with a character string '1'"}];

-- --#[between-06] TODO(redshift)
[UNSUPPORTED{message="BETWEEN SYMMETRIC"}];

-- --#[exists-00] TODO(redshift)
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE EXISTS(SELECT 1 AS "_1" FROM "default"."T" AS "s" WHERE "s"['b'] = "t"['b']);

-- --#[exists-01] TODO(redshift)
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE NOT (EXISTS(SELECT 1 AS "_1" FROM "default"."T" AS "s" WHERE "s"['b'] = "t"['b']));

-- --#[exists-02] TODO(redshift)
SELECT EXISTS(SELECT max("s"['b']) AS "max" FROM "default"."T" AS "s" WHERE "s"['b'] > 100) AS "e" FROM "default"."T" AS "t";

-- --#[is-true-00] TODO(redshift)
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" WHERE "t"['a'] IS TRUE AND NOT ("t"['a'] IS FALSE);

-- --#[is-true-01] TODO(redshift)
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" WHERE NOT ("t"['b'] > 1 IS TRUE);

-- --#[is-true-02] TODO(redshift)
[UNSUPPORTED{message="`IS TRUE` on a non-boolean value"}];

-- --#[is-true-03] TODO(redshift)
[UNSUPPORTED{message="`IS FALSE` on a non-boolean value"}];

-- --#[is-unknown-00] TODO(redshift)
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" WHERE "t"['a'] IS NULL OR "t"['b'] IS NOT NULL;

-- --#[is-distinct-00] TODO(redshift)
[UNSUPPORTED{message="IS DISTINCT FROM (not supported by Redshift)"}];

--#[condition-00]
[UNSUPPORTED{message="non-boolean condition"}];

--#[condition-01]
[UNSUPPORTED{message="non-boolean condition"}];

--#[condition-02]
[UNSUPPORTED{message="non-boolean condition"}];

--#[condition-03]
[UNSUPPORTED{message="non-boolean condition"}];

--#[condition-04]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE (NULL OR "t"['x']) OR "t"['a'];
