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

-- DATE_DIFF test cases using T_ALL_TYPES columns
--#[datetime-41]
SELECT DATE_DIFF(SECOND, T.col_time, T.col_time) FROM T_ALL_TYPES AS T;

--#[datetime-42]
SELECT DATE_DIFF(SECOND, T.col_time, T.col_timez) FROM T_ALL_TYPES AS T;

--#[datetime-43]
SELECT DATE_DIFF(SECOND, T.col_timez, T.col_time) FROM T_ALL_TYPES AS T;

--#[datetime-44]
SELECT DATE_DIFF(SECOND, T.col_timez, T.col_timez) FROM T_ALL_TYPES AS T;

--#[datetime-45]
SELECT DATE_DIFF(DAY, T.col_date, T.col_date) FROM T_ALL_TYPES AS T;

--#[datetime-46]
SELECT DATE_DIFF(DAY, T.col_date, T.col_timestamp) FROM T_ALL_TYPES AS T;

--#[datetime-47]
SELECT DATE_DIFF(DAY, T.col_date, T.col_timestampz) FROM T_ALL_TYPES AS T;

--#[datetime-48]
SELECT DATE_DIFF(DAY, T.col_timestamp, T.col_date) FROM T_ALL_TYPES AS T;

--#[datetime-49]
SELECT DATE_DIFF(DAY, T.col_timestamp, T.col_timestamp) FROM T_ALL_TYPES AS T;

--#[datetime-50]
SELECT DATE_DIFF(DAY, T.col_timestamp, T.col_timestampz) FROM T_ALL_TYPES AS T;

--#[datetime-51]
SELECT DATE_DIFF(DAY, T.col_timestampz, T.col_date) FROM T_ALL_TYPES AS T;

--#[datetime-52]
SELECT DATE_DIFF(DAY, T.col_timestampz, T.col_timestamp) FROM T_ALL_TYPES AS T;

--#[datetime-53]
SELECT DATE_DIFF(DAY, T.col_timestampz, T.col_timestampz) FROM T_ALL_TYPES AS T;

-- DATE_DIFF test cases using datetime literals
--#[datetime-54]
SELECT DATE_DIFF(SECOND, TIME '12:34:56', TIME '13:45:00') FROM T_ALL_TYPES AS T;

--#[datetime-55]
SELECT DATE_DIFF(SECOND, TIME '12:34:56', TIME WITH TIME ZONE '13:45:00+08:00') FROM T_ALL_TYPES AS T;

--#[datetime-56]
SELECT DATE_DIFF(SECOND, TIME WITH TIME ZONE '12:34:56+08:00', TIME '13:45:00') FROM T_ALL_TYPES AS T;

--#[datetime-57]
SELECT DATE_DIFF(SECOND, TIME WITH TIME ZONE '12:34:56+08:00', TIME WITH TIME ZONE '13:45:00+08:00') FROM T_ALL_TYPES AS T;

--#[datetime-58]
SELECT DATE_DIFF(DAY, DATE '2023-01-15', DATE '2023-12-25') FROM T_ALL_TYPES AS T;

--#[datetime-59]
SELECT DATE_DIFF(DAY, DATE '2023-01-15', TIMESTAMP '2023-12-25 10:30:00') FROM T_ALL_TYPES AS T;

--#[datetime-60]
SELECT DATE_DIFF(DAY, DATE '2023-01-15', TIMESTAMP WITH TIME ZONE '2023-12-25 10:30:00+08:00') FROM T_ALL_TYPES AS T;

--#[datetime-61]
SELECT DATE_DIFF(DAY, TIMESTAMP '2023-01-15 08:00:00', DATE '2023-12-25') FROM T_ALL_TYPES AS T;

--#[datetime-62]
SELECT DATE_DIFF(DAY, TIMESTAMP '2023-01-15 08:00:00', TIMESTAMP '2023-12-25 10:30:00') FROM T_ALL_TYPES AS T;

--#[datetime-63]
SELECT DATE_DIFF(DAY, TIMESTAMP '2023-01-15 08:00:00', TIMESTAMP WITH TIME ZONE '2023-12-25 10:30:00+08:00') FROM T_ALL_TYPES AS T;

--#[datetime-64]
SELECT DATE_DIFF(DAY, TIMESTAMP WITH TIME ZONE '2023-01-15 08:00:00+08:00', DATE '2023-12-25') FROM T_ALL_TYPES AS T;

--#[datetime-65]
SELECT DATE_DIFF(DAY, TIMESTAMP WITH TIME ZONE '2023-01-15 08:00:00+08:00', TIMESTAMP '2023-12-25 10:30:00') FROM T_ALL_TYPES AS T;

--#[datetime-66]
SELECT DATE_DIFF(DAY, TIMESTAMP WITH TIME ZONE '2023-01-15 08:00:00+08:00', TIMESTAMP WITH TIME ZONE '2023-12-25 10:30:00+08:00') FROM T_ALL_TYPES AS T;
