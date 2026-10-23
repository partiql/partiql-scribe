--#[union-all]
(SELECT "t"['b'] AS "x" FROM "default"."T" AS "t") UNION ALL (SELECT "t_all_types"['col_int32'] AS "x" FROM "default"."T_ALL_TYPES" AS "t_all_types");

--#[union-all-names]
(SELECT "t"['c'] AS "c" FROM "default"."T" AS "t") UNION ALL (SELECT "t_all_types"['col_string'] AS "c" FROM "default"."T_ALL_TYPES" AS "t_all_types");

--#[union]
(SELECT "t"['a'] AS "a" FROM "default"."T" AS "t") UNION DISTINCT (SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t");

--#[union-distinct]
(SELECT "t"['a'] AS "a" FROM "default"."T" AS "t") UNION DISTINCT (SELECT "simple_t"['a'] AS "a" FROM "default"."SIMPLE_T" AS "simple_t");

--#[intersect-precedence]
(SELECT "t"['b'] AS "b" FROM "default"."T" AS "t") UNION DISTINCT ((SELECT "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t") INTERSECT DISTINCT (SELECT "t_all_types"['col_int32'] AS "b" FROM "default"."T_ALL_TYPES" AS "t_all_types"));

--#[union-except-left-associative]
((SELECT "t"['b'] AS "b" FROM "default"."T" AS "t") UNION DISTINCT (SELECT "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t")) EXCEPT DISTINCT (SELECT "t_all_types"['col_int32'] AS "b" FROM "default"."T_ALL_TYPES" AS "t_all_types");

--#[minus]
(SELECT DISTINCT "t"['b'] AS "b" FROM "default"."T" AS "t") EXCEPT DISTINCT (SELECT "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t");

--#[except]
(SELECT DISTINCT "t"['b'] AS "b" FROM "default"."T" AS "t") EXCEPT DISTINCT (SELECT "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t");

--#[intersect-all]
[INVALID_INPUT{message="INTERSECT ALL is not supported in Redshift"}];

--#[except-all]
[INVALID_INPUT{message="EXCEPT ALL is not supported in Redshift"}];

--#[set-operation-column-count]
[INVALID_INPUT{message="each UNION query must have the same number of columns"}];

--#[set-operation-order-by-position]
SELECT "$s"['b'] AS "b" FROM ((SELECT "t"['b'] AS "b" FROM "default"."T" AS "t") UNION ALL (SELECT "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t")) AS "$s" ORDER BY "$s"['b'] ASC NULLS LAST LIMIT 5;

--#[set-operation-order-by-name]
SELECT "$s"['x'] AS "x" FROM ((SELECT "t"['b'] AS "x" FROM "default"."T" AS "t") UNION DISTINCT (SELECT "simple_t"['b'] AS "x" FROM "default"."SIMPLE_T" AS "simple_t")) AS "$s" ORDER BY "$s"['x'] DESC NULLS FIRST LIMIT 3 OFFSET 1;

--#[set-operation-order-by-expression]
[INVALID_INPUT{message="ORDER BY of a set operation can use only output column names or positions"}];

--#[set-operation-order-by-unknown]
[INVALID_INPUT{message="ORDER BY `c` is not an output column of the set operation"}];

--#[set-operation-parenthesized]
(SELECT "t"['b'] AS "x" FROM "default"."T" AS "t" ORDER BY "t"['b'] DESC NULLS FIRST LIMIT 1) UNION ALL (SELECT "simple_t"['b'] AS "x" FROM "default"."SIMPLE_T" AS "simple_t" ORDER BY "simple_t"['b'] ASC NULLS LAST LIMIT 1);

--#[set-operation-nested]
(SELECT "t"['b'] AS "b" FROM "default"."T" AS "t") UNION ALL ((SELECT "simple_t"['b'] AS "b" FROM "default"."SIMPLE_T" AS "simple_t") UNION DISTINCT (SELECT "t_all_types"['col_int32'] AS "b" FROM "default"."T_ALL_TYPES" AS "t_all_types"));

--#[set-operation-top]
[UNSUPPORTED{message="TOP in a set operation operand"}];

-- --#[set-operation-repeated-names] TODO(redshift)
SELECT "$s"['$c1'] AS "b", "$s"['$c2'] AS "b" FROM ((SELECT "t"['b'] AS "$c1", "s"['b'] AS "$c2" FROM "default"."T" AS "t" INNER JOIN "default"."SIMPLE_T" AS "s" ON "t"['c'] = "s"['c']) UNION DISTINCT (SELECT "t"['b'] AS "$c1", "t"['b'] AS "$c2" FROM "default"."T" AS "t")) AS "$s";

-- --#[set-operation-repeated-names-order-by] TODO(redshift)
SELECT "$s"['$c1'] AS "b", "$s"['$c2'] AS "b" FROM ((SELECT "t"['b'] AS "$c1", "s"['b'] AS "$c2" FROM "default"."T" AS "t" INNER JOIN "default"."SIMPLE_T" AS "s" ON "t"['c'] = "s"['c']) UNION ALL (SELECT "t"['b'] AS "$c1", "t"['b'] AS "$c2" FROM "default"."T" AS "t")) AS "$s" ORDER BY "$s"['$c2'] DESC NULLS FIRST;

--#[set-operation-star]
(SELECT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b", "simple_t"['c'] AS "c" FROM "default"."SIMPLE_T" AS "simple_t") UNION DISTINCT (SELECT "simple_t"['a'] AS "a", "simple_t"['b'] AS "b", "simple_t"['c'] AS "c" FROM "default"."SIMPLE_T" AS "simple_t");

--#[set-operation-super]
[UNSUPPORTED{message="set operation over SUPER values or over columns of incompatible types"}];
