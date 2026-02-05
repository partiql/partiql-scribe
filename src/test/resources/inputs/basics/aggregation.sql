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

--#[rel-aggregation-max-min-date]
SELECT MAX(col_date), MIN(col_date) FROM T_ALL_TYPES AS T;

--#[rel-aggregation-max-min-time]
SELECT MAX(col_time), MIN(col_time) FROM T_ALL_TYPES AS T;

--#[rel-aggregation-max-min-timez]
SELECT MAX(col_timez), MIN(col_timez) FROM T_ALL_TYPES AS T;

--#[rel-aggregation-max-min-timestamp]
SELECT MAX(col_timestamp), MIN(col_timestamp) FROM T_ALL_TYPES AS T;

--#[rel-aggregation-max-min-timestampz]
SELECT MAX(col_timestampz), MIN(col_timestampz) FROM T_ALL_TYPES AS T;

--#[rel-aggregation-max-min-string]
SELECT MAX(col_string), MIN(col_string) FROM T_ALL_TYPES AS T;

--#[rel-aggregation-distinct-00]
SELECT SUM(DISTINCT b) FROM T;

--#[rel-aggregation-null-01]
SELECT SUM(NULL) FROM T;

--#[rel-aggregation-multiple-00]
SELECT COUNT(*), SUM(b), AVG(b), MIN(b), MAX(b) FROM T;

--#[rel-aggregation-multiple-01]
SELECT a, COUNT(*), SUM(b), AVG(b) FROM T GROUP BY a;

--#[rel-aggregation-multiple-group-00]
SELECT a, c, COUNT(*) FROM T GROUP BY a, c;

--#[rel-aggregation-multiple-group-01]
SELECT a, c, SUM(b), AVG(b) FROM T GROUP BY a, c;

--#[rel-aggregation-multiple-group-02]
SELECT SUM(b), a, AVG(b), c FROM T GROUP BY a, c;

--#[rel-aggregation-where-group-00]
SELECT a, COUNT(*) FROM T WHERE b > 0 GROUP BY a;
