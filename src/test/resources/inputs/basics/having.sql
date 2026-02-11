--#[having-00]
SELECT * FROM T GROUP BY T.b HAVING COUNT(T.b) > 1;

--#[having-01]
SELECT * FROM T WHERE T.b < 1 GROUP BY T.b HAVING COUNT(T.b) > 1;

--#[having-02]
SELECT a, b, COUNT(*) FROM T GROUP BY a, b HAVING COUNT(*) > 1 AND b > 5;

--#[having-03]
SELECT a, b FROM T GROUP BY a, b HAVING a = TRUE AND b < 20;

