--#[in-00]
SELECT * FROM T WHERE b IN (1, 2);

--#[in-01]
SELECT * FROM T WHERE b IN [1, 2];

--#[in-02]
SELECT * FROM T WHERE b IN << 1, 2 >>;

--#[in-03]
SELECT * FROM T WHERE b NOT IN (1, 2);

--#[in-04]
SELECT * FROM T WHERE b NOT IN [1, 2];

--#[in-05]
SELECT * FROM T WHERE b NOT IN << 1, 2 >>;

--#[in-06]
SELECT * FROM T WHERE (b, c) IN ((1, 'hello'), (2, 'world'));

--#[in-07]
SELECT * FROM T WHERE (b, c) NOT IN ((1, 'hello'), (2, 'world'));

-- Array-column membership: RHS is a runtime array value (a column), not a literal list or subquery.
-- SQL `IN` only accepts a value list or subquery, so targets rewrite this to their array-membership function.
--#[in-08]
SELECT * FROM T WHERE b IN T."array";

-- Negated array-column membership. (PartiQL's grammar only accepts `NOT IN` before a value list or subquery, so the
-- negation of array membership is written as `NOT (<value> IN <array>)`.)
--#[in-09]
SELECT * FROM T WHERE NOT (b IN T."array");

-- Subquery membership: RHS is a subquery. Native `IN (<subquery>)` / `NOT IN (<subquery>)` is preserved.
--#[in-10]
SELECT * FROM T WHERE b IN (SELECT T.b FROM T);

--#[in-11]
SELECT * FROM T WHERE b NOT IN (SELECT T.b FROM T);
