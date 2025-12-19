--#[eq-00]
SELECT "T"['a'] = true AS "_1" FROM "default"."T" AS "T";

--#[eq-01]
SELECT "T"['b'] = 1 AS "_1" FROM "default"."T" AS "T";

--#[eq-02]
SELECT "T"['c'] = 'a' AS "_1" FROM "default"."T" AS "T";

--#[eq-03]
SELECT "T"['c'] = "T"['z'] AS "_1" FROM "default"."T" AS "T";

--#[eq-04]
SELECT "T"['c'] = "T"['d']['e'] AS "_1" FROM "default"."T" AS "T";

--#[eq-05]
SELECT "T"['c'] = "T"['x'] AS "_1" FROM "default"."T" AS "T";

--#[neq-00]
SELECT "T"['a'] <> true AS "_1" FROM "default"."T" AS "T";

--#[neq-01]
SELECT "T"['b'] <> 1 AS "_1" FROM "default"."T" AS "T";

--#[neq-02]
SELECT "T"['c'] <> 'a' AS "_1" FROM "default"."T" AS "T";

--#[neq-03]
SELECT "T"['c'] <> "T"['z'] AS "_1" FROM "default"."T" AS "T";

--#[neq-04]
SELECT "T"['c'] <> "T"['d']['e'] AS "_1" FROM "default"."T" AS "T";

--#[neq-05]
SELECT "T"['c'] <> "T"['x'] AS "_1" FROM "default"."T" AS "T";

--#[lt-00]
SELECT "T"['b'] < 1 AS "_1" FROM "default"."T" AS "T";

--#[lt-01]
SELECT "T"['b'] < "T"['x'] AS "_1" FROM "default"."T" AS "T";

--#[lte-00]
SELECT "T"['b'] <= 1 AS "_1" FROM "default"."T" AS "T";

--#[lte-01]
SELECT "T"['b'] <= "T"['x'] AS "_1" FROM "default"."T" AS "T";

--#[gt-00]
SELECT "T"['b'] > 1 AS "_1" FROM "default"."T" AS "T";

--#[gt-01]
SELECT "T"['b'] > "T"['x'] AS "_1" FROM "default"."T" AS "T";

--#[gte-00]
SELECT "T"['b'] >= 1 AS "_1" FROM "default"."T" AS "T";

--#[gte-01]
SELECT "T"['b'] >= "T"['x'] AS "_1" FROM "default"."T" AS "T";

--#[not-00]
SELECT NOT ("T"['a']) AS "_1" FROM "default"."T" AS "T";

--#[not-01]
SELECT NOT (NOT ("T"['a'])) AS "_1" FROM "default"."T" AS "T";

-- Time comparisons
--#[comparison-datetime-00]
SELECT "T"['col_time'] > "T"['col_time'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-01]
SELECT "T"['col_time'] < "T"['col_time'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-02]
SELECT "T"['col_time'] >= "T"['col_time'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-03]
SELECT "T"['col_time'] <= "T"['col_time'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-04]
SELECT CAST("T"['col_time'] AS TIME WITH TIME ZONE) > "T"['col_timez'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-05]
SELECT CAST("T"['col_time'] AS TIME WITH TIME ZONE) < "T"['col_timez'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-06]
SELECT CAST("T"['col_time'] AS TIME WITH TIME ZONE) >= "T"['col_timez'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-07]
SELECT CAST("T"['col_time'] AS TIME WITH TIME ZONE) <= "T"['col_timez'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-08]
SELECT "T"['col_timez'] > CAST("T"['col_time'] AS TIME WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-09]
SELECT "T"['col_timez'] < CAST("T"['col_time'] AS TIME WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-10]
SELECT "T"['col_timez'] >= CAST("T"['col_time'] AS TIME WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-11]
SELECT "T"['col_timez'] <= CAST("T"['col_time'] AS TIME WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-12]
SELECT "T"['col_timez'] > "T"['col_timez'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-13]
SELECT "T"['col_timez'] < "T"['col_timez'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-14]
SELECT "T"['col_timez'] >= "T"['col_timez'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-15]
SELECT "T"['col_timez'] <= "T"['col_timez'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Date comparisons
--#[comparison-datetime-16]
SELECT "T"['col_date'] > "T"['col_date'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-17]
SELECT "T"['col_date'] < "T"['col_date'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-18]
SELECT "T"['col_date'] >= "T"['col_date'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-19]
SELECT "T"['col_date'] <= "T"['col_date'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-20]
SELECT CAST("T"['col_date'] AS TIMESTAMP) > "T"['col_timestamp'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-21]
SELECT CAST("T"['col_date'] AS TIMESTAMP) < "T"['col_timestamp'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-22]
SELECT CAST("T"['col_date'] AS TIMESTAMP) >= "T"['col_timestamp'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-23]
SELECT CAST("T"['col_date'] AS TIMESTAMP) <= "T"['col_timestamp'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-24]
SELECT CAST("T"['col_date'] AS TIMESTAMP WITH TIME ZONE) > "T"['col_timestampz'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-25]
SELECT CAST("T"['col_date'] AS TIMESTAMP WITH TIME ZONE) < "T"['col_timestampz'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-26]
SELECT CAST("T"['col_date'] AS TIMESTAMP WITH TIME ZONE) >= "T"['col_timestampz'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-27]
SELECT CAST("T"['col_date'] AS TIMESTAMP WITH TIME ZONE) <= "T"['col_timestampz'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Timestamp comparisons
--#[comparison-datetime-28]
SELECT "T"['col_timestamp'] > CAST("T"['col_date'] AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-29]
SELECT "T"['col_timestamp'] < CAST("T"['col_date'] AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-30]
SELECT "T"['col_timestamp'] >= CAST("T"['col_date'] AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-31]
SELECT "T"['col_timestamp'] <= CAST("T"['col_date'] AS TIMESTAMP) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-32]
SELECT "T"['col_timestamp'] > "T"['col_timestamp'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-33]
SELECT "T"['col_timestamp'] < "T"['col_timestamp'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-34]
SELECT "T"['col_timestamp'] >= "T"['col_timestamp'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-35]
SELECT "T"['col_timestamp'] <= "T"['col_timestamp'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-36]
SELECT CAST("T"['col_timestamp'] AS TIMESTAMP WITH TIME ZONE) > "T"['col_timestampz'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-37]
SELECT CAST("T"['col_timestamp'] AS TIMESTAMP WITH TIME ZONE) < "T"['col_timestampz'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-38]
SELECT CAST("T"['col_timestamp'] AS TIMESTAMP WITH TIME ZONE) >= "T"['col_timestampz'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-39]
SELECT CAST("T"['col_timestamp'] AS TIMESTAMP WITH TIME ZONE) <= "T"['col_timestampz'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Timestampz comparisons
--#[comparison-datetime-40]
SELECT "T"['col_timestampz'] > CAST("T"['col_date'] AS TIMESTAMP WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-41]
SELECT "T"['col_timestampz'] < CAST("T"['col_date'] AS TIMESTAMP WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-42]
SELECT "T"['col_timestampz'] >= CAST("T"['col_date'] AS TIMESTAMP WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-43]
SELECT "T"['col_timestampz'] <= CAST("T"['col_date'] AS TIMESTAMP WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-44]
SELECT "T"['col_timestampz'] > CAST("T"['col_timestamp'] AS TIMESTAMP WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-45]
SELECT "T"['col_timestampz'] < CAST("T"['col_timestamp'] AS TIMESTAMP WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-46]
SELECT "T"['col_timestampz'] >= CAST("T"['col_timestamp'] AS TIMESTAMP WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-47]
SELECT "T"['col_timestampz'] <= CAST("T"['col_timestamp'] AS TIMESTAMP WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-48]
SELECT "T"['col_timestampz'] > "T"['col_timestampz'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-49]
SELECT "T"['col_timestampz'] < "T"['col_timestampz'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-50]
SELECT "T"['col_timestampz'] >= "T"['col_timestampz'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-51]
SELECT "T"['col_timestampz'] <= "T"['col_timestampz'] AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Time literal comparisons
--#[comparison-datetime-52]
SELECT TIME '12:30:45' > TIME '10:15:30' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-53]
SELECT TIME '12:30:45' < TIME '14:45:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-54]
SELECT TIME '12:30:45' >= TIME '12:30:45' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-55]
SELECT TIME '12:30:45' <= TIME '12:30:45' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-56]
SELECT CAST(TIME '12:30:45' AS TIME (6) WITH TIME ZONE) > TIME WITH TIME ZONE '10:15:30+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-57]
SELECT CAST(TIME '12:30:45' AS TIME (6) WITH TIME ZONE) < TIME WITH TIME ZONE '14:45:00+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-58]
SELECT CAST(TIME '12:30:45' AS TIME (6) WITH TIME ZONE) >= TIME WITH TIME ZONE '12:30:45+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-59]
SELECT CAST(TIME '12:30:45' AS TIME (6) WITH TIME ZONE) <= TIME WITH TIME ZONE '12:30:45+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-60]
SELECT TIME WITH TIME ZONE '12:30:45+08:00' > CAST(TIME '10:15:30' AS TIME (6) WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-61]
SELECT TIME WITH TIME ZONE '12:30:45+08:00' < CAST(TIME '14:45:00' AS TIME (6) WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-62]
SELECT TIME WITH TIME ZONE '12:30:45+08:00' >= CAST(TIME '12:30:45' AS TIME (6) WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-63]
SELECT TIME WITH TIME ZONE '12:30:45+08:00' <= CAST(TIME '12:30:45' AS TIME (6) WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-64]
SELECT TIME WITH TIME ZONE '12:30:45+08:00' > TIME WITH TIME ZONE '10:15:30+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-65]
SELECT TIME WITH TIME ZONE '12:30:45+08:00' < TIME WITH TIME ZONE '14:45:00+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-66]
SELECT TIME WITH TIME ZONE '12:30:45+08:00' >= TIME WITH TIME ZONE '12:30:45+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-67]
SELECT TIME WITH TIME ZONE '12:30:45+08:00' <= TIME WITH TIME ZONE '12:30:45+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Date literal comparisons
--#[comparison-datetime-68]
SELECT DATE '2023-12-25' > DATE '2023-06-15' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-69]
SELECT DATE '2023-12-25' < DATE '2024-01-01' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-70]
SELECT DATE '2023-12-25' >= DATE '2023-12-25' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-71]
SELECT DATE '2023-12-25' <= DATE '2023-12-25' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-72]
SELECT CAST(DATE '2023-12-25' AS TIMESTAMP (6)) > TIMESTAMP '2023-06-15 08:00:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-73]
SELECT CAST(DATE '2023-12-25' AS TIMESTAMP (6)) < TIMESTAMP '2024-01-01 00:00:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-74]
SELECT CAST(DATE '2023-12-25' AS TIMESTAMP (6)) >= TIMESTAMP '2023-12-25 00:00:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-75]
SELECT CAST(DATE '2023-12-25' AS TIMESTAMP (6)) <= TIMESTAMP '2023-12-25 23:59:59' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-76]
SELECT CAST(DATE '2023-12-25' AS TIMESTAMP (6) WITH TIME ZONE) > TIMESTAMP WITH TIME ZONE '2023-06-15 08:00:00+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-77]
SELECT CAST(DATE '2023-12-25' AS TIMESTAMP (6) WITH TIME ZONE) < TIMESTAMP WITH TIME ZONE '2024-01-01 00:00:00+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-78]
SELECT CAST(DATE '2023-12-25' AS TIMESTAMP (6) WITH TIME ZONE) >= TIMESTAMP WITH TIME ZONE '2023-12-25 00:00:00+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-79]
SELECT CAST(DATE '2023-12-25' AS TIMESTAMP (6) WITH TIME ZONE) <= TIMESTAMP WITH TIME ZONE '2023-12-25 23:59:59+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Timestamp literal comparisons
--#[comparison-datetime-80]
SELECT TIMESTAMP '2023-12-25 12:30:45' > CAST(DATE '2023-06-15' AS TIMESTAMP (6)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-81]
SELECT TIMESTAMP '2023-12-25 12:30:45' < CAST(DATE '2024-01-01' AS TIMESTAMP (6)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-82]
SELECT TIMESTAMP '2023-12-25 12:30:45' >= CAST(DATE '2023-12-25' AS TIMESTAMP (6)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-83]
SELECT TIMESTAMP '2023-12-25 12:30:45' <= CAST(DATE '2023-12-25' AS TIMESTAMP (6)) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-84]
SELECT TIMESTAMP '2023-12-25 12:30:45' > TIMESTAMP '2023-06-15 08:00:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-85]
SELECT TIMESTAMP '2023-12-25 12:30:45' < TIMESTAMP '2024-01-01 00:00:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-86]
SELECT TIMESTAMP '2023-12-25 12:30:45' >= TIMESTAMP '2023-12-25 12:30:45' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-87]
SELECT TIMESTAMP '2023-12-25 12:30:45' <= TIMESTAMP '2023-12-25 12:30:45' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-88]
SELECT CAST(TIMESTAMP '2023-12-25 12:30:45' AS TIMESTAMP (6) WITH TIME ZONE) > TIMESTAMP WITH TIME ZONE '2023-06-15 08:00:00+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-89]
SELECT CAST(TIMESTAMP '2023-12-25 12:30:45' AS TIMESTAMP (6) WITH TIME ZONE) < TIMESTAMP WITH TIME ZONE '2024-01-01 00:00:00+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-90]
SELECT CAST(TIMESTAMP '2023-12-25 12:30:45' AS TIMESTAMP (6) WITH TIME ZONE) >= TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-91]
SELECT CAST(TIMESTAMP '2023-12-25 12:30:45' AS TIMESTAMP (6) WITH TIME ZONE) <= TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Timestampz literal comparisons
--#[comparison-datetime-92]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' > CAST(DATE '2023-06-15' AS TIMESTAMP (6) WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-93]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' < CAST(DATE '2024-01-01' AS TIMESTAMP (6) WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-94]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' >= CAST(DATE '2023-12-25' AS TIMESTAMP (6) WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-95]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' <= CAST(DATE '2023-12-25' AS TIMESTAMP (6) WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-96]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' > CAST(TIMESTAMP '2023-06-15 08:00:00' AS TIMESTAMP (6) WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-97]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' < CAST(TIMESTAMP '2024-01-01 00:00:00' AS TIMESTAMP (6) WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-98]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' >= CAST(TIMESTAMP '2023-12-25 12:30:45' AS TIMESTAMP (6) WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-99]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' <= CAST(TIMESTAMP '2023-12-25 12:30:45' AS TIMESTAMP (6) WITH TIME ZONE) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-100]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' > TIMESTAMP WITH TIME ZONE '2023-06-15 08:00:00+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-101]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' < TIMESTAMP WITH TIME ZONE '2024-01-01 00:00:00+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-102]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' >= TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[comparison-datetime-103]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' <= TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- Array equality comparisons
--#[comparison-array-01]
SELECT [1, 2, 3] = [1, 2, 3] AS "_1" FROM "default"."T" AS "T";

--#[comparison-array-02]
SELECT [1, 2, 3] = [1, 2, 4] AS "_1" FROM "default"."T" AS "T";

--#[comparison-array-03]
SELECT [1, 2, 3] <> [1, 2, 4] AS "_1" FROM "default"."T" AS "T";

--#[comparison-array-04]
SELECT [1, 2] = [1, 2, 3] AS "_1" FROM "default"."T" AS "T";

--#[comparison-array-05]
SELECT [] = [] AS "_1" FROM "default"."T" AS "T";

--#[comparison-array-06]
SELECT ['a', 'b', 'c'] = ['a', 'b', 'c'] AS "_1" FROM "default"."T" AS "T";

--#[comparison-array-07]
SELECT [DATE '2023-01-01', DATE '2023-12-31'] = [DATE '2023-01-01', DATE '2023-12-31'] AS "_1" FROM "default"."T" AS "T";

--#[comparison-array-08]
SELECT [[1, 2], [3, 4]] = [[1, 2], [3, 4]] AS "_1" FROM "default"."T" AS "T";

--#[comparison-array-09]
SELECT [[1, 2], [3, 4]] = [[1, 2], [3, 5]] AS "_1" FROM "default"."T" AS "T";

--#[comparison-array-10]
SELECT [NULL, 1, 2] = [NULL, 1, 2] AS "_1" FROM "default"."T" AS "T";
