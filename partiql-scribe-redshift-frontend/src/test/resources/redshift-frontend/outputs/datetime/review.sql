-- --#[review-aggregate-00] TODO(redshift)
[UNSUPPORTED{message="`datetime +` of an aggregate function (the PartiQL planner does not accept an aggregate in a function argument)"}];

-- --#[review-aggregate-01] TODO(redshift)
[UNSUPPORTED{message="DATEADD with an argument of unknown type"}];

-- --#[review-subquery-00] TODO(redshift)
[UNSUPPORTED{message="DATEDIFF with an argument of unknown type"}];

-- --#[review-subquery-01] TODO(redshift)
[UNSUPPORTED{message="EXTRACT with an argument of unknown type"}];

--#[review-interval-times-00]
SELECT "t"['col_timestamp'] + ("t"['col_d2s'] * 2) AS "v" FROM "default"."T_INTERVALS" AS "t";

--#[review-interval-times-01]
[UNSUPPORTED{message="interval `*` with decimal operand"}];

--#[review-interval-times-02]
[UNSUPPORTED{message="interval `/` with integer operand"}];

--#[review-interval-negate-00]
SELECT "t"['col_timestamp'] + (INTERVAL '1' DAY (9) * -1) AS "v" FROM "default"."T_INTERVALS" AS "t";

--#[review-interval-negate-01]
SELECT "t"['col_timestamp'] + ("t"['col_d2s'] * -1) AS "v" FROM "default"."T_INTERVALS" AS "t";

--#[review-interval-negate-02]
SELECT "t"['col_timestamp'] + "t"['col_d2s'] AS "v" FROM "default"."T_INTERVALS" AS "t";

--#[review-interval-nested-00]
[UNSUPPORTED{message="INTERVAL literal '1 month' without a qualifier in interval arithmetic"}];

--#[review-interval-nested-01]
[UNSUPPORTED{message="INTERVAL literal '1 month' without a qualifier in interval arithmetic"}];

--#[review-timetz-case-00]
[UNSUPPORTED{message="comparison with a TIMETZ literal whose offset is not UTC"}];

--#[review-timetz-nullif-00]
[UNSUPPORTED{message="comparison with a TIMETZ literal whose offset is not UTC"}];

--#[review-order-by-00]
SELECT "t"['col_date'] AS "v" FROM "default"."T_ALL_TYPES" AS "t" ORDER BY DATE_ADD(DAY, 1, "t"['col_date']) ASC NULLS LAST;
