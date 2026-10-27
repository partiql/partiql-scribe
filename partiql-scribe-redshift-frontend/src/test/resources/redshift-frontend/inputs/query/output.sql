-- Default output column names: the function name for a function call, the operand for a CAST, the last attribute
-- for a path. Other expressions keep the PartiQL name.
--#[default-name-aggregates]
SELECT COUNT(*), SUM(b), MAX(c) FROM T;

--#[default-name-cast-of-aggregate]
SELECT COUNT(*)::INT FROM T;

--#[default-name-function-and-path]
SELECT UPPER(c), t.d.e, b::bigint FROM T t;

--#[default-name-window]
SELECT ROW_NUMBER() OVER (ORDER BY b) FROM T;

--#[default-name-expression]
SELECT b + 1 FROM T;

--#[default-name-set-operation]
SELECT COUNT(*) FROM T UNION ALL SELECT COUNT(*) FROM SIMPLE_T;

-- A select item of a SUPER navigation is wrapped in COALESCE, so that a missing attribute is NULL as in Redshift.
--#[super-navigation-coalesce]
SELECT t.x.y AS v, t.x AS w FROM T t;

--#[super-navigation-coalesce-grouped]
SELECT t.x.y AS v, COUNT(*) AS n FROM T t GROUP BY t.x.y;

-- A key reference in a subquery of HAVING refers to the key of the outer query.
--#[group-key-correlated]
SELECT c, COUNT(*) AS n FROM T GROUP BY c HAVING c IN (SELECT u.col_string FROM T_ALL_TYPES u WHERE u.col_string <> c);
