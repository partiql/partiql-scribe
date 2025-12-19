--#[eq-00]
SELECT a = true FROM T;

--#[eq-01]
SELECT b = 1 FROM T;

--#[eq-02]
SELECT c = 'a' FROM T;

--#[eq-03]
SELECT c = z FROM T;

--#[eq-04]
SELECT c = d.e FROM T;

--#[eq-05]
SELECT c = x FROM T;

--#[neq-00]
SELECT a != true FROM T;

--#[neq-01]
SELECT b != 1 FROM T;

--#[neq-02]
SELECT c != 'a' FROM T;

--#[neq-03]
SELECT c != z FROM T;

--#[neq-04]
SELECT c != d.e FROM T;

--#[neq-05]
SELECT c != x FROM T;

--#[lt-00]
SELECT b < 1 FROM T;

--#[lt-01]
SELECT b < x FROM T;

--#[lte-00]
SELECT b <= 1 FROM T;

--#[lte-01]
SELECT b <= x FROM T;

--#[gt-00]
SELECT b > 1 FROM T;

--#[gt-01]
SELECT b > x FROM T;

--#[gte-00]
SELECT b >= 1 FROM T;

--#[gte-01]
SELECT b >= x FROM T;

--#[not-00]
SELECT NOT a FROM T;

--#[not-01]
SELECT NOT (NOT a) FROM T;

-- Time comparisons
--#[comparison-datetime-00]
SELECT col_time > col_time FROM T_ALL_TYPES AS T;

--#[comparison-datetime-01]
SELECT col_time < col_time FROM T_ALL_TYPES AS T;

--#[comparison-datetime-02]
SELECT col_time >= col_time FROM T_ALL_TYPES AS T;

--#[comparison-datetime-03]
SELECT col_time <= col_time FROM T_ALL_TYPES AS T;

--#[comparison-datetime-04]
SELECT col_time > col_timez FROM T_ALL_TYPES AS T;

--#[comparison-datetime-05]
SELECT col_time < col_timez FROM T_ALL_TYPES AS T;

--#[comparison-datetime-06]
SELECT col_time >= col_timez FROM T_ALL_TYPES AS T;

--#[comparison-datetime-07]
SELECT col_time <= col_timez FROM T_ALL_TYPES AS T;

--#[comparison-datetime-08]
SELECT col_timez > col_time FROM T_ALL_TYPES AS T;

--#[comparison-datetime-09]
SELECT col_timez < col_time FROM T_ALL_TYPES AS T;

--#[comparison-datetime-10]
SELECT col_timez >= col_time FROM T_ALL_TYPES AS T;

--#[comparison-datetime-11]
SELECT col_timez <= col_time FROM T_ALL_TYPES AS T;

--#[comparison-datetime-12]
SELECT col_timez > col_timez FROM T_ALL_TYPES AS T;

--#[comparison-datetime-13]
SELECT col_timez < col_timez FROM T_ALL_TYPES AS T;

--#[comparison-datetime-14]
SELECT col_timez >= col_timez FROM T_ALL_TYPES AS T;

--#[comparison-datetime-15]
SELECT col_timez <= col_timez FROM T_ALL_TYPES AS T;

-- Date comparisons
--#[comparison-datetime-16]
SELECT col_date > col_date FROM T_ALL_TYPES AS T;

--#[comparison-datetime-17]
SELECT col_date < col_date FROM T_ALL_TYPES AS T;

--#[comparison-datetime-18]
SELECT col_date >= col_date FROM T_ALL_TYPES AS T;

--#[comparison-datetime-19]
SELECT col_date <= col_date FROM T_ALL_TYPES AS T;

--#[comparison-datetime-20]
SELECT col_date > col_timestamp FROM T_ALL_TYPES AS T;

--#[comparison-datetime-21]
SELECT col_date < col_timestamp FROM T_ALL_TYPES AS T;

--#[comparison-datetime-22]
SELECT col_date >= col_timestamp FROM T_ALL_TYPES AS T;

--#[comparison-datetime-23]
SELECT col_date <= col_timestamp FROM T_ALL_TYPES AS T;

--#[comparison-datetime-24]
SELECT col_date > col_timestampz FROM T_ALL_TYPES AS T;

--#[comparison-datetime-25]
SELECT col_date < col_timestampz FROM T_ALL_TYPES AS T;

--#[comparison-datetime-26]
SELECT col_date >= col_timestampz FROM T_ALL_TYPES AS T;

--#[comparison-datetime-27]
SELECT col_date <= col_timestampz FROM T_ALL_TYPES AS T;

-- Timestamp comparisons
--#[comparison-datetime-28]
SELECT col_timestamp > col_date FROM T_ALL_TYPES AS T;

--#[comparison-datetime-29]
SELECT col_timestamp < col_date FROM T_ALL_TYPES AS T;

--#[comparison-datetime-30]
SELECT col_timestamp >= col_date FROM T_ALL_TYPES AS T;

--#[comparison-datetime-31]
SELECT col_timestamp <= col_date FROM T_ALL_TYPES AS T;

--#[comparison-datetime-32]
SELECT col_timestamp > col_timestamp FROM T_ALL_TYPES AS T;

--#[comparison-datetime-33]
SELECT col_timestamp < col_timestamp FROM T_ALL_TYPES AS T;

--#[comparison-datetime-34]
SELECT col_timestamp >= col_timestamp FROM T_ALL_TYPES AS T;

--#[comparison-datetime-35]
SELECT col_timestamp <= col_timestamp FROM T_ALL_TYPES AS T;

--#[comparison-datetime-36]
SELECT col_timestamp > col_timestampz FROM T_ALL_TYPES AS T;

--#[comparison-datetime-37]
SELECT col_timestamp < col_timestampz FROM T_ALL_TYPES AS T;

--#[comparison-datetime-38]
SELECT col_timestamp >= col_timestampz FROM T_ALL_TYPES AS T;

--#[comparison-datetime-39]
SELECT col_timestamp <= col_timestampz FROM T_ALL_TYPES AS T;

-- Timestampz comparisons
--#[comparison-datetime-40]
SELECT col_timestampz > col_date FROM T_ALL_TYPES AS T;

--#[comparison-datetime-41]
SELECT col_timestampz < col_date FROM T_ALL_TYPES AS T;

--#[comparison-datetime-42]
SELECT col_timestampz >= col_date FROM T_ALL_TYPES AS T;

--#[comparison-datetime-43]
SELECT col_timestampz <= col_date FROM T_ALL_TYPES AS T;

--#[comparison-datetime-44]
SELECT col_timestampz > col_timestamp FROM T_ALL_TYPES AS T;

--#[comparison-datetime-45]
SELECT col_timestampz < col_timestamp FROM T_ALL_TYPES AS T;

--#[comparison-datetime-46]
SELECT col_timestampz >= col_timestamp FROM T_ALL_TYPES AS T;

--#[comparison-datetime-47]
SELECT col_timestampz <= col_timestamp FROM T_ALL_TYPES AS T;

--#[comparison-datetime-48]
SELECT col_timestampz > col_timestampz FROM T_ALL_TYPES AS T;

--#[comparison-datetime-49]
SELECT col_timestampz < col_timestampz FROM T_ALL_TYPES AS T;

--#[comparison-datetime-50]
SELECT col_timestampz >= col_timestampz FROM T_ALL_TYPES AS T;

--#[comparison-datetime-51]
SELECT col_timestampz <= col_timestampz FROM T_ALL_TYPES AS T;

-- Time literal comparisons
--#[comparison-datetime-52]
SELECT TIME '12:30:45' > TIME '10:15:30' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-53]
SELECT TIME '12:30:45' < TIME '14:45:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-54]
SELECT TIME '12:30:45' >= TIME '12:30:45' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-55]
SELECT TIME '12:30:45' <= TIME '12:30:45' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-56]
SELECT TIME '12:30:45' > TIME WITH TIME ZONE '10:15:30+08:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-57]
SELECT TIME '12:30:45' < TIME WITH TIME ZONE '14:45:00+08:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-58]
SELECT TIME '12:30:45' >= TIME WITH TIME ZONE '12:30:45+08:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-59]
SELECT TIME '12:30:45' <= TIME WITH TIME ZONE '12:30:45+08:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-60]
SELECT TIME WITH TIME ZONE '12:30:45+08:00' > TIME '10:15:30' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-61]
SELECT TIME WITH TIME ZONE '12:30:45+08:00' < TIME '14:45:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-62]
SELECT TIME WITH TIME ZONE '12:30:45+08:00' >= TIME '12:30:45' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-63]
SELECT TIME WITH TIME ZONE '12:30:45+08:00' <= TIME '12:30:45' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-64]
SELECT TIME WITH TIME ZONE '12:30:45+08:00' > TIME WITH TIME ZONE '10:15:30+08:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-65]
SELECT TIME WITH TIME ZONE '12:30:45+08:00' < TIME WITH TIME ZONE '14:45:00+08:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-66]
SELECT TIME WITH TIME ZONE '12:30:45+08:00' >= TIME WITH TIME ZONE '12:30:45+08:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-67]
SELECT TIME WITH TIME ZONE '12:30:45+08:00' <= TIME WITH TIME ZONE '12:30:45+08:00' FROM T_ALL_TYPES AS T;

-- Date literal comparisons
--#[comparison-datetime-68]
SELECT DATE '2023-12-25' > DATE '2023-06-15' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-69]
SELECT DATE '2023-12-25' < DATE '2024-01-01' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-70]
SELECT DATE '2023-12-25' >= DATE '2023-12-25' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-71]
SELECT DATE '2023-12-25' <= DATE '2023-12-25' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-72]
SELECT DATE '2023-12-25' > TIMESTAMP '2023-06-15 08:00:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-73]
SELECT DATE '2023-12-25' < TIMESTAMP '2024-01-01 00:00:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-74]
SELECT DATE '2023-12-25' >= TIMESTAMP '2023-12-25 00:00:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-75]
SELECT DATE '2023-12-25' <= TIMESTAMP '2023-12-25 23:59:59' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-76]
SELECT DATE '2023-12-25' > TIMESTAMP WITH TIME ZONE '2023-06-15 08:00:00+08:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-77]
SELECT DATE '2023-12-25' < TIMESTAMP WITH TIME ZONE '2024-01-01 00:00:00+08:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-78]
SELECT DATE '2023-12-25' >= TIMESTAMP WITH TIME ZONE '2023-12-25 00:00:00+08:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-79]
SELECT DATE '2023-12-25' <= TIMESTAMP WITH TIME ZONE '2023-12-25 23:59:59+08:00' FROM T_ALL_TYPES AS T;

-- Timestamp literal comparisons
--#[comparison-datetime-80]
SELECT TIMESTAMP '2023-12-25 12:30:45' > DATE '2023-06-15' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-81]
SELECT TIMESTAMP '2023-12-25 12:30:45' < DATE '2024-01-01' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-82]
SELECT TIMESTAMP '2023-12-25 12:30:45' >= DATE '2023-12-25' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-83]
SELECT TIMESTAMP '2023-12-25 12:30:45' <= DATE '2023-12-25' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-84]
SELECT TIMESTAMP '2023-12-25 12:30:45' > TIMESTAMP '2023-06-15 08:00:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-85]
SELECT TIMESTAMP '2023-12-25 12:30:45' < TIMESTAMP '2024-01-01 00:00:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-86]
SELECT TIMESTAMP '2023-12-25 12:30:45' >= TIMESTAMP '2023-12-25 12:30:45' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-87]
SELECT TIMESTAMP '2023-12-25 12:30:45' <= TIMESTAMP '2023-12-25 12:30:45' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-88]
SELECT TIMESTAMP '2023-12-25 12:30:45' > TIMESTAMP WITH TIME ZONE '2023-06-15 08:00:00+08:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-89]
SELECT TIMESTAMP '2023-12-25 12:30:45' < TIMESTAMP WITH TIME ZONE '2024-01-01 00:00:00+08:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-90]
SELECT TIMESTAMP '2023-12-25 12:30:45' >= TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-91]
SELECT TIMESTAMP '2023-12-25 12:30:45' <= TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' FROM T_ALL_TYPES AS T;

-- Timestampz literal comparisons
--#[comparison-datetime-92]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' > DATE '2023-06-15' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-93]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' < DATE '2024-01-01' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-94]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' >= DATE '2023-12-25' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-95]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' <= DATE '2023-12-25' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-96]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' > TIMESTAMP '2023-06-15 08:00:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-97]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' < TIMESTAMP '2024-01-01 00:00:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-98]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' >= TIMESTAMP '2023-12-25 12:30:45' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-99]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' <= TIMESTAMP '2023-12-25 12:30:45' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-100]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' > TIMESTAMP WITH TIME ZONE '2023-06-15 08:00:00+08:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-101]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' < TIMESTAMP WITH TIME ZONE '2024-01-01 00:00:00+08:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-102]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' >= TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' FROM T_ALL_TYPES AS T;

--#[comparison-datetime-103]
SELECT TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' <= TIMESTAMP WITH TIME ZONE '2023-12-25 12:30:45+08:00' FROM T_ALL_TYPES AS T;

-- Array equality comparisons
--#[comparison-array-01]
SELECT [1, 2, 3] = [1, 2, 3] FROM T;

--#[comparison-array-02]
SELECT [1, 2, 3] = [1, 2, 4] FROM T;

--#[comparison-array-03]
SELECT [1, 2, 3] <> [1, 2, 4] FROM T;

--#[comparison-array-04]
SELECT [1, 2] = [1, 2, 3] FROM T;

--#[comparison-array-05]
SELECT [] = [] FROM T;

--#[comparison-array-06]
SELECT ['a', 'b', 'c'] = ['a', 'b', 'c'] FROM T;

--#[comparison-array-07]
SELECT [DATE '2023-01-01', DATE '2023-12-31'] = [DATE '2023-01-01', DATE '2023-12-31'] FROM T;

--#[comparison-array-08]
SELECT [[1, 2], [3, 4]] = [[1, 2], [3, 4]] FROM T;

--#[comparison-array-09]
SELECT [[1, 2], [3, 4]] = [[1, 2], [3, 5]] FROM T;

--#[comparison-array-10]
SELECT [NULL, 1, 2] = [NULL, 1, 2] FROM T;
