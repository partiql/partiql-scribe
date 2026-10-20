--#[qualify]
SELECT b FROM T QUALIFY b > 1;

--#[ilike]
SELECT a FROM T WHERE c ILIKE 'a%';

--#[unknown-function]
SELECT MD5(c) AS v FROM T;

--#[datepart-week]
SELECT DATEADD(week, 1, timestamp_1) AS v FROM T;

--#[group-by-position]
SELECT c, COUNT(*) AS n FROM T GROUP BY 1;

--#[order-by-position]
SELECT c FROM T ORDER BY 1;

--#[no-from]
SELECT 1;

--#[super-type]
SELECT x::super AS v FROM T;

--#[top-and-limit]
SELECT TOP 5 b FROM T LIMIT 3;
