-- QUALIFY filters the rows of a subquery that computes the condition.
--#[qualify-window]
SELECT b, c FROM T t QUALIFY ROW_NUMBER() OVER (PARTITION BY c ORDER BY b DESC) = 1;

--#[qualify-star]
SELECT * FROM SIMPLE_T t QUALIFY ROW_NUMBER() OVER (PARTITION BY c ORDER BY b DESC) = 1;

-- A select-list alias in QUALIFY is the column of the item, so the filter and the item have the same value.
--#[qualify-alias]
SELECT c, b, ROW_NUMBER() OVER (PARTITION BY c ORDER BY b) AS rn FROM T t QUALIFY rn <= 2;

--#[qualify-alias-and-column]
SELECT c, ROW_NUMBER() OVER (PARTITION BY c ORDER BY b) AS rn FROM T t QUALIFY rn = 1 AND b > 0;

--#[qualify-grouped]
SELECT c, COUNT(*) AS n FROM T GROUP BY c QUALIFY RANK() OVER (ORDER BY COUNT(*) DESC) <= 2;

--#[qualify-where]
SELECT b, c FROM T WHERE b > 0 QUALIFY RANK() OVER (PARTITION BY c ORDER BY b) = 1;

--#[qualify-order-by-limit]
SELECT b, c FROM T t QUALIFY ROW_NUMBER() OVER (PARTITION BY c ORDER BY b) = 1 ORDER BY c DESC LIMIT 3;

--#[qualify-order-by-not-selected]
SELECT b FROM T t QUALIFY ROW_NUMBER() OVER (PARTITION BY c ORDER BY b) = 1 ORDER BY c;

--#[qualify-distinct-limit]
SELECT DISTINCT c FROM T t QUALIFY ROW_NUMBER() OVER (PARTITION BY c ORDER BY b) = 1 ORDER BY c LIMIT 3;

--#[qualify-repeated-names]
SELECT t1.b, t2.b FROM T t1 JOIN SIMPLE_T t2 ON t1.c = t2.c QUALIFY ROW_NUMBER() OVER (ORDER BY t1.b) = 1;

-- QUALIFY directly after an unaliased table: Redshift reads QUALIFY as the alias of the table.
--#[qualify-without-alias]
SELECT b FROM T QUALIFY ROW_NUMBER() OVER (ORDER BY b) = 1;

-- QUALIFY is not a reserved word.
--#[qualify-as-name]
SELECT qualify.b FROM T qualify;
