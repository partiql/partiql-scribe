--#[rel-aggregation-count-00]
SELECT COUNT(*) FROM T;

--#[rel-aggregation-count-01]
SELECT COUNT(1) FROM T;

--#[rel-aggregation-count-02]
SELECT COUNT(a) FROM T;

--#[rel-aggregation-count-03]
SELECT COUNT(*) FROM T GROUP BY a;

--#[rel-aggregation-max-00]
SELECT MAX(b) FROM T;

--#[rel-aggregation-max-01]
SELECT MAX(b) FROM T GROUP BY a;
