--#[datetime-21]
SELECT INTERVAL '1' SECOND + TIMESTAMP_NTZ '2017-01-02 03:04:05.006' AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-22]
SELECT `unix_timestamp`(TIMESTAMP_NTZ '2017-01-02 03:04:20.006') - `unix_timestamp`(TIMESTAMP_NTZ '2017-01-02 03:04:05.006') AS `_1` FROM `default`.`T` AS `T`;

-- Check UTCNOW()
--#[datetime-23]
SELECT `convert_timezone`('UTC', `current_timestamp`()) AS `_1` FROM `default`.`T` AS `T`;

-- #[datetime-41]..#[datetime-44] Spark does not support Time types

--#[datetime-45]
SELECT `date_diff`(`T`.`col_date`, `T`.`col_date`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[datetime-46]
SELECT `date_diff`(`T`.`col_timestamp`, CAST(`T`.`col_date` AS TIMESTAMP_NTZ)) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[datetime-47]
SELECT `date_diff`(`T`.`col_timestampz`, CAST(`T`.`col_date` AS TIMESTAMP)) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[datetime-48]
SELECT `date_diff`(CAST(`T`.`col_date` AS TIMESTAMP_NTZ), `T`.`col_timestamp`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[datetime-49]
SELECT `date_diff`(`T`.`col_timestamp`, `T`.`col_timestamp`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[datetime-50]
SELECT `date_diff`(`T`.`col_timestampz`, CAST(`T`.`col_timestamp` AS TIMESTAMP)) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[datetime-51]
SELECT `date_diff`(CAST(`T`.`col_date` AS TIMESTAMP), `T`.`col_timestampz`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[datetime-52]
SELECT `date_diff`(CAST(`T`.`col_timestamp` AS TIMESTAMP), `T`.`col_timestampz`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[datetime-53]
SELECT `date_diff`(`T`.`col_timestampz`, `T`.`col_timestampz`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

-- #[datetime-54]..#[datetime-57] Spark does not support Time types

--#[datetime-58]
SELECT `date_diff`(DATE '2023-12-25', DATE '2023-01-15') AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[datetime-59]
SELECT `date_diff`(TIMESTAMP_NTZ '2023-12-25 10:30:00', CAST(DATE '2023-01-15' AS TIMESTAMP_NTZ)) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[datetime-60]
SELECT `date_diff`(TIMESTAMP '2023-12-25 10:30:00+08:00', CAST(DATE '2023-01-15' AS TIMESTAMP)) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[datetime-61]
SELECT `date_diff`(CAST(DATE '2023-12-25' AS TIMESTAMP_NTZ), TIMESTAMP_NTZ '2023-01-15 08:00:00') AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[datetime-62]
SELECT `date_diff`(TIMESTAMP_NTZ '2023-12-25 10:30:00', TIMESTAMP_NTZ '2023-01-15 08:00:00') AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[datetime-63]
SELECT `date_diff`(TIMESTAMP '2023-12-25 10:30:00+08:00', CAST(TIMESTAMP_NTZ '2023-01-15 08:00:00' AS TIMESTAMP)) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[datetime-64]
SELECT `date_diff`(CAST(DATE '2023-12-25' AS TIMESTAMP), TIMESTAMP '2023-01-15 08:00:00+08:00') AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[datetime-65]
SELECT `date_diff`(CAST(TIMESTAMP_NTZ '2023-12-25 10:30:00' AS TIMESTAMP), TIMESTAMP '2023-01-15 08:00:00+08:00') AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[datetime-66]
SELECT `date_diff`(TIMESTAMP '2023-12-25 10:30:00+08:00', TIMESTAMP '2023-01-15 08:00:00+08:00') AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;
