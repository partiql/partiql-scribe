--#[datetime-00]
CURRENT_DATE;

--#[datetime-01]
DATE_ADD(SECOND, 5, TIME '12:34:56');

--#[datetime-02]
DATE_ADD(MINUTE, 5, TIME '12:34:56');

--#[datetime-03]
DATE_ADD(HOUR, 5, TIME '12:34:56');

--#[datetime-04]
DATE_ADD(DAY, 5, CURRENT_DATE);

--#[datetime-05]
DATE_ADD(MONTH, 5, CURRENT_DATE);

--#[datetime-06]
DATE_ADD(YEAR, 5, CURRENT_DATE);

--#[datetime-07]
DATE_DIFF(DAY, CURRENT_DATE, CURRENT_DATE);

--#[datetime-08]
SELECT CURRENT_DATE FROM T;

--#[datetime-09]
SELECT DATE_ADD(SECOND, 5, T.timestamp_1) FROM T;

--#[datetime-10]
SELECT DATE_ADD(MINUTE, 5, T.timestamp_1) FROM T;

--#[datetime-11]
SELECT DATE_ADD(HOUR, 5, T.timestamp_1) FROM T;

--#[datetime-12]
SELECT DATE_ADD(DAY, 5, CURRENT_DATE) FROM T;

--#[datetime-13]
SELECT DATE_ADD(MONTH, 5, CURRENT_DATE) FROM T;

--#[datetime-14]
SELECT DATE_ADD(YEAR, 5, CURRENT_DATE) FROM T;

--#[datetime-15]
SELECT DATE_DIFF(YEAR, timestamp_1, timestamp_2) FROM T;

--#[datetime-16]
SELECT DATE_DIFF(MONTH, timestamp_1, timestamp_2) FROM T;

--#[datetime-17]
SELECT DATE_DIFF(DAY, timestamp_1, timestamp_2) FROM T;

--#[datetime-18]
SELECT DATE_DIFF(HOUR, timestamp_1, timestamp_2) FROM T;

--#[datetime-19]
SELECT DATE_DIFF(MINUTE, timestamp_1, timestamp_2) FROM T;

--#[datetime-20]
SELECT DATE_DIFF(SECOND, timestamp_1, timestamp_2) FROM T;

--#[datetime-21]
SELECT DATE_ADD(SECOND, 1, TIMESTAMP '2017-01-02 03:04:05.006') FROM T;

--#[datetime-22]
SELECT DATE_DIFF(SECOND, TIMESTAMP '2017-01-02 03:04:05.006', TIMESTAMP '2017-01-02 03:04:20.006') FROM T;

-- Check UTCNOW()
--#[datetime-23]
SELECT UTCNOW() FROM T;

-- Explicit casts for comparison/equality with a datetime path
--#[datetime-24]
SELECT * FROM datatypes.T_TIMESTAMP AS t WHERE t.foo.keep > TIMESTAMP '2023-10-19 12:34:56';

--#[datetime-25]
SELECT * FROM datatypes.T_DATE AS t WHERE DATE '2023-10-19' < t.foo.keep;

--#[datetime-26]
SELECT * FROM datatypes.T_TIME AS t WHERE t.foo.keep <= TIME '12:34:56';

--#[datetime-27]
SELECT * FROM datatypes.T_TIMESTAMP AS t WHERE t.foo.keep = TIMESTAMP '2023-10-19 12:34:56';

--#[datetime-28]
SELECT * FROM datatypes.T_TIMESTAMP AS t WHERE t.foo.keep <> TIMESTAMP '2023-10-19 12:34:56';

-- Explicit casts for EXTRACT with a datetime path
--#[datetime-29]
SELECT EXTRACT(YEAR FROM t.foo.keep) FROM datatypes.T_TIMESTAMP AS t;

--#[datetime-30]
SELECT EXTRACT(YEAR FROM t.foo.keep) FROM datatypes.T_DATE AS t;

--#[datetime-31]
SELECT EXTRACT(HOUR FROM t.foo.keep) FROM datatypes.T_TIME AS t;

-- Explicit casts for BETWEEN with a datetime path
--#[datetime-32]
SELECT * FROM datatypes.T_TIMESTAMP AS t WHERE t.foo.keep BETWEEN TIMESTAMP '2023-10-19 12:34:56' AND TIMESTAMP '2024-10-19 12:34:56';

--#[datetime-33]
SELECT * FROM datatypes.T_TIMESTAMP AS t WHERE TIMESTAMP '2023-10-19 12:34:56' BETWEEN t.foo.keep AND TIMESTAMP '2024-10-19 12:34:56';

--#[datetime-34]
SELECT * FROM datatypes.T_TIMESTAMP AS t WHERE TIMESTAMP '2023-12-19 12:34:56' BETWEEN TIMESTAMP '2023-10-19 12:34:56' AND t.foo.keep;

-- Explicit casts for NULLIF with a datetime path
--#[datetime-35]
SELECT NULLIF(t.foo.keep, TIMESTAMP '2023-12-19 12:34:56') FROM datatypes.T_TIMESTAMP AS t;

--#[datetime-36]
SELECT NULLIF(TIMESTAMP '2023-12-19 12:34:56', t.foo.keep) FROM datatypes.T_TIMESTAMP AS t;

-- Explicit casts for CASE-WHEN with a datetime path
--#[datetime-37]
SELECT CASE t.foo.keep WHEN TIMESTAMP '2023-12-19 12:34:56' THEN t.foo.keep ELSE UTCNOW() END AS result FROM datatypes.T_TIMESTAMP AS t;

--#[datetime-38]
SELECT CASE t.foo.keep WHEN TIMESTAMP '2023-12-19 12:34:56' THEN TIMESTAMP '2023-12-19 12:34:56' ELSE t.foo.keep END AS result FROM datatypes.T_TIMESTAMP AS t;

--#[datetime-39]
SELECT CASE WHEN TIMESTAMP '2023-12-19 12:34:56' = t.foo.keep THEN TIMESTAMP '2023-12-19 12:34:56' ELSE t.foo.keep END AS result FROM datatypes.T_TIMESTAMP AS t;

-- Explicit cast for top-level timestamp in comparison
--#[datetime-40]
SELECT timestamp_2, timestamp_1 < timestamp_2 AS result FROM T WHERE timestamp_1 > TIMESTAMP '2023-12-19 12:34:56';
