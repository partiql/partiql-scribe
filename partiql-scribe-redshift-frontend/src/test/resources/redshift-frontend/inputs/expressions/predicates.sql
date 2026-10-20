-- IN values get the conversions of `=`.
--#[in-list-00]
SELECT a FROM T WHERE b NOT IN (1, NULL);

--#[in-list-01]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_int32 IN ('1', '2');

--#[in-list-02]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_date IN ('2020-01-01', DATE '2020-01-02');

--#[in-list-03]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_char IN ('a ', 'b');

--#[in-list-04]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_float64 IN (0.1, 1);

--#[in-list-05]
SELECT a FROM T WHERE c IN (1, 2);

-- The values need different conversions of the left side.
--#[in-list-06]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_float32 IN (1, col_float32);

-- The PartiQL output target writes a subquery value of an IN list without parentheses.
--#[in-list-07]
SELECT a FROM T WHERE b IN ((SELECT MAX(b) FROM T), 2);

--#[in-query-00]
SELECT a FROM T WHERE b IN (SELECT b FROM T);

--#[in-query-01]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_char NOT IN (SELECT col_string FROM T_ALL_TYPES);

-- Redshift converts the values of the subquery.
--#[in-query-02]
SELECT a FROM T WHERE b IN (SELECT c FROM T);

--#[in-query-03]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_string IN (SELECT col_char FROM T_ALL_TYPES);

-- = ANY and = SOME are IN; <> ALL is NOT IN.
--#[quantified-00]
SELECT a FROM T WHERE b = ANY (SELECT b FROM T AS s WHERE s.b > 1);

--#[quantified-01]
SELECT a FROM T WHERE b = SOME (SELECT b FROM T);

--#[quantified-02]
SELECT a FROM T WHERE b <> ALL (SELECT b FROM T AS s);

--#[quantified-03]
SELECT a FROM T WHERE b != ALL (SELECT b FROM T AS s);

--#[quantified-04]
SELECT a FROM T WHERE b > ANY (SELECT b FROM T);

--#[quantified-05]
SELECT a FROM T WHERE b = ALL (SELECT b FROM T);

--#[row-in-00]
SELECT a FROM T WHERE (b, timestamp_1) IN ((1, TIMESTAMP '2020-01-01 00:00:00'), (2, TIMESTAMP '2021-01-01 00:00:00'));

--#[row-in-01]
SELECT a FROM T WHERE (b, c) NOT IN ((1, 'x'));

--#[row-in-02]
SELECT a FROM T WHERE (b, c) IN ((1, 'x', 3));

--#[row-in-03]
SELECT a FROM T WHERE (b, c) IN (SELECT b, c FROM T);

-- BETWEEN with literal bounds is BETWEEN.
--#[between-00]
SELECT a FROM T WHERE b NOT BETWEEN -5 AND +10;

-- PartiQL BETWEEN is NULL when a bound is NULL; Redshift BETWEEN is x >= a AND x <= b.
--#[between-01]
SELECT a FROM T WHERE timestamp_1 NOT BETWEEN timestamp_2 AND TIMESTAMP '2030-01-01 00:00:00';

--#[between-02]
SELECT b BETWEEN NULL AND 3 AS v FROM T;

--#[between-03]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_date BETWEEN '2020-01-01' AND '2020-12-31';

--#[between-04]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_float64 BETWEEN 0 AND 0.1;

--#[between-05]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_int32 BETWEEN '1' AND '5';

--#[between-06]
SELECT a FROM T WHERE b BETWEEN SYMMETRIC 10 AND 1;

--#[exists-00]
SELECT a FROM T WHERE EXISTS (SELECT 1 FROM T AS s WHERE s.b = T.b);

--#[exists-01]
SELECT a FROM T WHERE NOT EXISTS (SELECT 1 FROM T AS s WHERE s.b = T.b);

-- An aggregate subquery has one row.
--#[exists-02]
SELECT EXISTS (SELECT MAX(s.b) FROM T AS s WHERE s.b > 100) AS e FROM T;

--#[is-true-00]
SELECT b FROM T WHERE a IS TRUE AND a IS NOT FALSE;

--#[is-true-01]
SELECT b FROM T WHERE (b > 1) IS NOT TRUE;

-- Redshift reads a number as a truth value (non-zero is TRUE).
--#[is-true-02]
SELECT b FROM T WHERE b IS TRUE;

--#[is-true-03]
SELECT b FROM T WHERE x IS FALSE;

--#[is-unknown-00]
SELECT b FROM T WHERE a IS UNKNOWN OR b IS NOT UNKNOWN;

-- IS DISTINCT FROM is not supported by Redshift.
--#[is-distinct-00]
SELECT a FROM T WHERE b IS DISTINCT FROM 1;

-- PartiQL treats a condition that is not BOOLEAN as never true.
--#[condition-00]
SELECT a FROM T WHERE b;

--#[condition-01]
SELECT a FROM T WHERE a AND NOT b;

--#[condition-02]
SELECT a FROM T t1 JOIN T t2 ON t1.b;

--#[condition-03]
SELECT c, COUNT(*) AS n FROM T GROUP BY c HAVING COUNT(*);

--#[condition-04]
SELECT a FROM T WHERE NULL OR x OR a;
