--#[review-set-operation-in-00]
SELECT t.b FROM T AS t WHERE t.b IN (SELECT s.b FROM SIMPLE_T AS s UNION SELECT u.b FROM T AS u);

--#[review-set-operation-in-01]
SELECT t.b FROM T AS t WHERE t.b NOT IN (SELECT s.b FROM SIMPLE_T AS s EXCEPT SELECT u.b FROM T AS u);

--#[review-set-operation-scalar-00]
SELECT (SELECT MAX(s.b) AS m FROM SIMPLE_T AS s UNION SELECT MAX(u.b) FROM SIMPLE_T AS u) AS v FROM T AS t;

--#[review-set-operation-any-00]
SELECT t.b FROM T AS t WHERE t.b = ANY (SELECT s.b FROM SIMPLE_T AS s INTERSECT SELECT u.b FROM T AS u);

--#[review-set-operation-order-by-00]
SELECT t.b AS t FROM T AS t UNION SELECT s.b FROM SIMPLE_T AS s ORDER BY t DESC LIMIT 2;

--#[review-set-operation-order-by-01]
SELECT t.b AS x FROM T AS t UNION ALL SELECT s.b FROM SIMPLE_T AS s ORDER BY 1 DESC;

--#[review-default-name-00]
SELECT d.coalesce, d."case", d.pgdate_part FROM (SELECT NVL(t.b, 0), DECODE(t.b, 1, 'x', 'y'), DATE_PART(year, t.timestamp_1) FROM T AS t) AS d;

--#[review-default-name-01]
SELECT d."timestamp" IS NOT NULL AS v FROM (SELECT GETDATE()) AS d;
