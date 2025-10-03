-- DATETIME - DATETIME -> INTERVAL
--#[dt-minus-dt-00]
SELECT `T`.`col_date` - `T`.`col_date` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

-- Spark does not support time values
-- --#[dt-minus-dt-01]
-- SELECT `T`.`col_time` - `T`.`col_time` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

-- --#[dt-minus-dt-02]
-- SELECT `T`.`col_timez` - `T`.`col_timez` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[dt-minus-dt-03]
SELECT `T`.`col_timestamp` - `T`.`col_timestamp` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[dt-minus-dt-04]
SELECT `T`.`col_timestampz` - `T`.`col_timestampz` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

-- TODO there are probably some others we can add for DATETIME - DATETIME -> DATETIME

-- DATETIME + INTERVAL -> DATETIME and DATETIME - INTERVAL -> DATETIME and INTERVAL + DATETIME -> DATETIME

-- DATETIME + INTERVAL -> DATETIME
--#[dt-plus-interval-00]
SELECT `T`.`col_date` + `T`.`col_y` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-01]
SELECT `T`.`col_date` + `T`.`col_mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-02]
SELECT `T`.`col_date` + `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-03]
SELECT `T`.`col_date` + `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-04]
SELECT `T`.`col_date` + `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-05]
SELECT `T`.`col_date` + `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-06]
SELECT `T`.`col_date` + `T`.`col_y2mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-07]
SELECT `T`.`col_date` + `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-08]
SELECT `T`.`col_date` + `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-09]
SELECT `T`.`col_date` + `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-10]
SELECT `T`.`col_date` + `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-11]
SELECT `T`.`col_date` + `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-12]
SELECT `T`.`col_date` + `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

-- Spark does not support time values
-- By SQL2023: 6.42, error
-- --#[dt-plus-interval-13]
-- SELECT `T`.`col_time` + `T`.`col_y` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- By SQL2023: 6.42, error
-- --#[dt-plus-interval-14]
-- SELECT `T`.`col_time` + `T`.`col_mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-15]
-- SELECT `T`.`col_time` + `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-16]
-- SELECT `T`.`col_time` + `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-17]
-- SELECT `T`.`col_time` + `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-18]
-- SELECT `T`.`col_time` + `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- By SQL2023: 6.42, error
-- --#[dt-plus-interval-19]
-- SELECT `T`.`col_time` + `T`.`col_y2mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-20]
-- SELECT `T`.`col_time` + `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-21]
-- SELECT `T`.`col_time` + `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-22]
-- SELECT `T`.`col_time` + `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-23]
-- SELECT `T`.`col_time` + `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-24]
-- SELECT `T`.`col_time` + `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-25]
-- SELECT `T`.`col_time` + `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

-- Spark does not support time values
-- By SQL2023: 6.42, error
-- --#[dt-plus-interval-26]
-- SELECT `T`.`col_timez` + `T`.`col_y` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- By SQL2023: 6.42, error
-- --#[dt-plus-interval-27]
-- SELECT `T`.`col_timez` + `T`.`col_mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-28]
-- SELECT `T`.`col_timez` + `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-29]
-- SELECT `T`.`col_timez` + `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-30]
-- SELECT `T`.`col_timez` + `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-31]
-- SELECT `T`.`col_timez` + `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- By SQL2023: 6.42, error
-- --#[dt-plus-interval-32]
-- SELECT `T`.`col_timez` + `T`.`col_y2mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-33]
-- SELECT `T`.`col_timez` + `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-34]
-- SELECT `T`.`col_timez` + `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-35]
-- SELECT `T`.`col_timez` + `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-36]
-- SELECT `T`.`col_timez` + `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-37]
-- SELECT `T`.`col_timez` + `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-plus-interval-38]
-- SELECT `T`.`col_timez` + `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[dt-plus-interval-39]
SELECT `T`.`col_timestamp` + `T`.`col_y` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-40]
SELECT `T`.`col_timestamp` + `T`.`col_mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-41]
SELECT `T`.`col_timestamp` + `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-42]
SELECT `T`.`col_timestamp` + `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-43]
SELECT `T`.`col_timestamp` + `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-44]
SELECT `T`.`col_timestamp` + `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-45]
SELECT `T`.`col_timestamp` + `T`.`col_y2mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-46]
SELECT `T`.`col_timestamp` + `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-47]
SELECT `T`.`col_timestamp` + `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-48]
SELECT `T`.`col_timestamp` + `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-49]
SELECT `T`.`col_timestamp` + `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-50]
SELECT `T`.`col_timestamp` + `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-51]
SELECT `T`.`col_timestamp` + `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[dt-plus-interval-52]
SELECT `T`.`col_timestampz` + `T`.`col_y` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-53]
SELECT `T`.`col_timestampz` + `T`.`col_mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-54]
SELECT `T`.`col_timestampz` + `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-55]
SELECT `T`.`col_timestampz` + `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-56]
SELECT `T`.`col_timestampz` + `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-57]
SELECT `T`.`col_timestampz` + `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-58]
SELECT `T`.`col_timestampz` + `T`.`col_y2mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-59]
SELECT `T`.`col_timestampz` + `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-60]
SELECT `T`.`col_timestampz` + `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-61]
SELECT `T`.`col_timestampz` + `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-62]
SELECT `T`.`col_timestampz` + `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-63]
SELECT `T`.`col_timestampz` + `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-plus-interval-64]
SELECT `T`.`col_timestampz` + `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

-- DATETIME - INTERVAL -> DATETIME
--#[dt-minus-interval-00]
SELECT `T`.`col_date` - `T`.`col_y` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-01]
SELECT `T`.`col_date` - `T`.`col_mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-02]
SELECT `T`.`col_date` - `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-03]
SELECT `T`.`col_date` - `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-04]
SELECT `T`.`col_date` - `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-05]
SELECT `T`.`col_date` - `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-06]
SELECT `T`.`col_date` - `T`.`col_y2mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-07]
SELECT `T`.`col_date` - `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-08]
SELECT `T`.`col_date` - `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-09]
SELECT `T`.`col_date` - `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-10]
SELECT `T`.`col_date` - `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-11]
SELECT `T`.`col_date` - `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-12]
SELECT `T`.`col_date` - `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

-- Spark does not support time values
-- By SQL2023: 6.42, error
-- --#[dt-minus-interval-13]
-- SELECT `T`.`col_time` - `T`.`col_y` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- By SQL2023: 6.42, error
-- --#[dt-minus-interval-14]
-- SELECT `T`.`col_time` - `T`.`col_mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-15]
-- SELECT `T`.`col_time` - `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-16]
-- SELECT `T`.`col_time` - `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-17]
-- SELECT `T`.`col_time` - `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-18]
-- SELECT `T`.`col_time` - `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- By SQL2023: 6.42, error
-- --#[dt-minus-interval-19]
-- SELECT `T`.`col_time` - `T`.`col_y2mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-20]
-- SELECT `T`.`col_time` - `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-21]
-- SELECT `T`.`col_time` - `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-22]
-- SELECT `T`.`col_time` - `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-23]
-- SELECT `T`.`col_time` - `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-24]
-- SELECT `T`.`col_time` - `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-25]
-- SELECT `T`.`col_time` - `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

-- Spark does not support time values
-- By SQL2023: 6.42, error
-- --#[dt-minus-interval-26]
-- SELECT `T`.`col_timez` - `T`.`col_y` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- By SQL2023: 6.42, error
-- --#[dt-minus-interval-27]
-- SELECT `T`.`col_timez` - `T`.`col_mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-28]
-- SELECT `T`.`col_timez` - `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-29]
-- SELECT `T`.`col_timez` - `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-30]
-- SELECT `T`.`col_timez` - `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-31]
-- SELECT `T`.`col_timez` - `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- By SQL2023: 6.42, error
-- --#[dt-minus-interval-32]
-- SELECT `T`.`col_timez` - `T`.`col_y2mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-33]
-- SELECT `T`.`col_timez` - `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-34]
-- SELECT `T`.`col_timez` - `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-35]
-- SELECT `T`.`col_timez` - `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-36]
-- SELECT `T`.`col_timez` - `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-37]
-- SELECT `T`.`col_timez` - `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[dt-minus-interval-38]
-- SELECT `T`.`col_timez` - `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[dt-minus-interval-39]
SELECT `T`.`col_timestamp` - `T`.`col_y` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-40]
SELECT `T`.`col_timestamp` - `T`.`col_mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-41]
SELECT `T`.`col_timestamp` - `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-42]
SELECT `T`.`col_timestamp` - `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-43]
SELECT `T`.`col_timestamp` - `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-44]
SELECT `T`.`col_timestamp` - `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-45]
SELECT `T`.`col_timestamp` - `T`.`col_y2mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-46]
SELECT `T`.`col_timestamp` - `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-47]
SELECT `T`.`col_timestamp` - `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-48]
SELECT `T`.`col_timestamp` - `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-49]
SELECT `T`.`col_timestamp` - `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-50]
SELECT `T`.`col_timestamp` - `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-51]
SELECT `T`.`col_timestamp` - `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[dt-minus-interval-52]
SELECT `T`.`col_timestampz` - `T`.`col_y` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-53]
SELECT `T`.`col_timestampz` - `T`.`col_mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-54]
SELECT `T`.`col_timestampz` - `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-55]
SELECT `T`.`col_timestampz` - `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-56]
SELECT `T`.`col_timestampz` - `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-57]
SELECT `T`.`col_timestampz` - `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-58]
SELECT `T`.`col_timestampz` - `T`.`col_y2mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-59]
SELECT `T`.`col_timestampz` - `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-60]
SELECT `T`.`col_timestampz` - `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-61]
SELECT `T`.`col_timestampz` - `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-62]
SELECT `T`.`col_timestampz` - `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-63]
SELECT `T`.`col_timestampz` - `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[dt-minus-interval-64]
SELECT `T`.`col_timestampz` - `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;


-- INTERVAL + DATETIME -> DATETIME
--#[interval-plus-00]
SELECT `T`.`col_y` + `T`.`col_date` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-01]
SELECT `T`.`col_mon` + `T`.`col_date` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-02]
SELECT `T`.`col_d` + `T`.`col_date` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-03]
SELECT `T`.`col_h` + `T`.`col_date` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-04]
SELECT `T`.`col_min` + `T`.`col_date` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-05]
SELECT `T`.`col_s` + `T`.`col_date` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-06]
SELECT `T`.`col_y2mon` + `T`.`col_date` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-07]
SELECT `T`.`col_d2h` + `T`.`col_date` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-08]
SELECT `T`.`col_d2min` + `T`.`col_date` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-09]
SELECT `T`.`col_d2s` + `T`.`col_date` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-10]
SELECT `T`.`col_h2min` + `T`.`col_date` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-11]
SELECT `T`.`col_h2s` + `T`.`col_date` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-12]
SELECT `T`.`col_m2s` + `T`.`col_date` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

-- Spark does not support time values
-- By SQL2023: 6.42, error
-- --#[interval-plus-13]
-- SELECT `T`.`col_y` + `T`.`col_time` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- By SQL2023: 6.42, error
-- --#[interval-plus-14]
-- SELECT `T`.`col_mon` + `T`.`col_time` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-15]
-- SELECT `T`.`col_d` + `T`.`col_time` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-16]
-- SELECT `T`.`col_h` + `T`.`col_time` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-17]
-- SELECT `T`.`col_min` + `T`.`col_time` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-18]
-- SELECT `T`.`col_s` + `T`.`col_time` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- By SQL2023: 6.42, error
-- --#[interval-plus-19]
-- SELECT `T`.`col_y2mon` + `T`.`col_time` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-20]
-- SELECT `T`.`col_d2h` + `T`.`col_time` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-21]
-- SELECT `T`.`col_d2min` + `T`.`col_time` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-22]
-- SELECT `T`.`col_d2s` + `T`.`col_time` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-23]
-- SELECT `T`.`col_h2min` + `T`.`col_time` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-24]
-- SELECT `T`.`col_h2s` + `T`.`col_time` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-25]
-- SELECT `T`.`col_m2s` + `T`.`col_time` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

-- Spark does not support time values
-- By SQL2023: 6.42, error
-- --#[interval-plus-26]
-- SELECT `T`.`col_y` + `T`.`col_timez` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- By SQL2023: 6.42, error
-- --#[interval-plus-27]
-- SELECT `T`.`col_mon` + `T`.`col_timez` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-28]
-- SELECT `T`.`col_d` + `T`.`col_timez` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-29]
-- SELECT `T`.`col_h` + `T`.`col_timez` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-30]
-- SELECT `T`.`col_min` + `T`.`col_timez` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-31]
-- SELECT `T`.`col_s` + `T`.`col_timez` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- By SQL2023: 6.42, error
-- --#[interval-plus-32]
-- SELECT `T`.`col_y2mon` + `T`.`col_timez` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-33]
-- SELECT `T`.`col_d2h` + `T`.`col_timez` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-34]
-- SELECT `T`.`col_d2min` + `T`.`col_timez` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-35]
-- SELECT `T`.`col_d2s` + `T`.`col_timez` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-36]
-- SELECT `T`.`col_h2min` + `T`.`col_timez` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-37]
-- SELECT `T`.`col_h2s` + `T`.`col_timez` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
-- --#[interval-plus-38]
-- SELECT `T`.`col_m2s` + `T`.`col_timez` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-plus-39]
SELECT `T`.`col_y` + `T`.`col_timestamp` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-40]
SELECT `T`.`col_mon` + `T`.`col_timestamp` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-41]
SELECT `T`.`col_d` + `T`.`col_timestamp` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-42]
SELECT `T`.`col_h` + `T`.`col_timestamp` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-43]
SELECT `T`.`col_min` + `T`.`col_timestamp` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-44]
SELECT `T`.`col_s` + `T`.`col_timestamp` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-45]
SELECT `T`.`col_y2mon` + `T`.`col_timestamp` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-46]
SELECT `T`.`col_d2h` + `T`.`col_timestamp` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-47]
SELECT `T`.`col_d2min` + `T`.`col_timestamp` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-48]
SELECT `T`.`col_d2s` + `T`.`col_timestamp` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-49]
SELECT `T`.`col_h2min` + `T`.`col_timestamp` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-50]
SELECT `T`.`col_h2s` + `T`.`col_timestamp` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-51]
SELECT `T`.`col_m2s` + `T`.`col_timestamp` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-plus-52]
SELECT `T`.`col_y` + `T`.`col_timestampz` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-53]
SELECT `T`.`col_mon` + `T`.`col_timestampz` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-54]
SELECT `T`.`col_d` + `T`.`col_timestampz` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-55]
SELECT `T`.`col_h` + `T`.`col_timestampz` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-56]
SELECT `T`.`col_min` + `T`.`col_timestampz` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-57]
SELECT `T`.`col_s` + `T`.`col_timestampz` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-58]
SELECT `T`.`col_y2mon` + `T`.`col_timestampz` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-59]
SELECT `T`.`col_d2h` + `T`.`col_timestampz` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-60]
SELECT `T`.`col_d2min` + `T`.`col_timestampz` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-61]
SELECT `T`.`col_d2s` + `T`.`col_timestampz` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-62]
SELECT `T`.`col_h2min` + `T`.`col_timestampz` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-63]
SELECT `T`.`col_h2s` + `T`.`col_timestampz` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-64]
SELECT `T`.`col_m2s` + `T`.`col_timestampz` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

-- INTERVAL + INTERVAL -> INTERVAL
--#[interval-plus-interval-00]
SELECT `T`.`col_d` + `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-01]
SELECT `T`.`col_d` + `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-02]
SELECT `T`.`col_d` + `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-03]
SELECT `T`.`col_d` + `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-04]
SELECT `T`.`col_d` + `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-05]
SELECT `T`.`col_d` + `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-06]
SELECT `T`.`col_d` + `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-07]
SELECT `T`.`col_d` + `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-08]
SELECT `T`.`col_d` + `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-09]
SELECT `T`.`col_d` + `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-plus-interval-10]
SELECT `T`.`col_h` + `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-11]
SELECT `T`.`col_h` + `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-12]
SELECT `T`.`col_h` + `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-13]
SELECT `T`.`col_h` + `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-14]
SELECT `T`.`col_h` + `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-15]
SELECT `T`.`col_h` + `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-16]
SELECT `T`.`col_h` + `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-17]
SELECT `T`.`col_h` + `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-18]
SELECT `T`.`col_h` + `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-19]
SELECT `T`.`col_h` + `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-plus-interval-20]
SELECT `T`.`col_min` + `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-21]
SELECT `T`.`col_min` + `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-22]
SELECT `T`.`col_min` + `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-23]
SELECT `T`.`col_min` + `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-24]
SELECT `T`.`col_min` + `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-25]
SELECT `T`.`col_min` + `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-26]
SELECT `T`.`col_min` + `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-27]
SELECT `T`.`col_min` + `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-28]
SELECT `T`.`col_min` + `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-29]
SELECT `T`.`col_min` + `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-plus-interval-30]
SELECT `T`.`col_s` + `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-31]
SELECT `T`.`col_s` + `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-32]
SELECT `T`.`col_s` + `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-33]
SELECT `T`.`col_s` + `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-34]
SELECT `T`.`col_s` + `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-35]
SELECT `T`.`col_s` + `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-36]
SELECT `T`.`col_s` + `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-37]
SELECT `T`.`col_s` + `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-38]
SELECT `T`.`col_s` + `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-39]
SELECT `T`.`col_s` + `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-plus-interval-40]
SELECT `T`.`col_d2h` + `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-41]
SELECT `T`.`col_d2h` + `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-42]
SELECT `T`.`col_d2h` + `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-43]
SELECT `T`.`col_d2h` + `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-44]
SELECT `T`.`col_d2h` + `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-45]
SELECT `T`.`col_d2h` + `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-46]
SELECT `T`.`col_d2h` + `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-47]
SELECT `T`.`col_d2h` + `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-48]
SELECT `T`.`col_d2h` + `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-49]
SELECT `T`.`col_d2h` + `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-plus-interval-50]
SELECT `T`.`col_d2min` + `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-51]
SELECT `T`.`col_d2min` + `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-52]
SELECT `T`.`col_d2min` + `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-53]
SELECT `T`.`col_d2min` + `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-54]
SELECT `T`.`col_d2min` + `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-55]
SELECT `T`.`col_d2min` + `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-56]
SELECT `T`.`col_d2min` + `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-57]
SELECT `T`.`col_d2min` + `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-58]
SELECT `T`.`col_d2min` + `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-59]
SELECT `T`.`col_d2min` + `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-plus-interval-60]
SELECT `T`.`col_d2s` + `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-61]
SELECT `T`.`col_d2s` + `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-62]
SELECT `T`.`col_d2s` + `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-63]
SELECT `T`.`col_d2s` + `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-64]
SELECT `T`.`col_d2s` + `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-65]
SELECT `T`.`col_d2s` + `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-66]
SELECT `T`.`col_d2s` + `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-67]
SELECT `T`.`col_d2s` + `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-68]
SELECT `T`.`col_d2s` + `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-69]
SELECT `T`.`col_d2s` + `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-plus-interval-70]
SELECT `T`.`col_h2min` + `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-71]
SELECT `T`.`col_h2min` + `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-72]
SELECT `T`.`col_h2min` + `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-73]
SELECT `T`.`col_h2min` + `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-74]
SELECT `T`.`col_h2min` + `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-75]
SELECT `T`.`col_h2min` + `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-76]
SELECT `T`.`col_h2min` + `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-77]
SELECT `T`.`col_h2min` + `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-78]
SELECT `T`.`col_h2min` + `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-79]
SELECT `T`.`col_h2min` + `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-plus-interval-80]
SELECT `T`.`col_h2s` + `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-81]
SELECT `T`.`col_h2s` + `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-82]
SELECT `T`.`col_h2s` + `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-83]
SELECT `T`.`col_h2s` + `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-84]
SELECT `T`.`col_h2s` + `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-85]
SELECT `T`.`col_h2s` + `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-86]
SELECT `T`.`col_h2s` + `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-87]
SELECT `T`.`col_h2s` + `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-88]
SELECT `T`.`col_h2s` + `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-89]
SELECT `T`.`col_h2s` + `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-plus-interval-90]
SELECT `T`.`col_m2s` + `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-91]
SELECT `T`.`col_m2s` + `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-92]
SELECT `T`.`col_m2s` + `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-93]
SELECT `T`.`col_m2s` + `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-94]
SELECT `T`.`col_m2s` + `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-95]
SELECT `T`.`col_m2s` + `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-96]
SELECT `T`.`col_m2s` + `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-97]
SELECT `T`.`col_m2s` + `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-98]
SELECT `T`.`col_m2s` + `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-99]
SELECT `T`.`col_m2s` + `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-plus-interval-100]
SELECT `T`.`col_y` + `T`.`col_y` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-101]
SELECT `T`.`col_y` + `T`.`col_mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-102]
SELECT `T`.`col_y` + `T`.`col_y2mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-103]
SELECT `T`.`col_mon` + `T`.`col_y` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-104]
SELECT `T`.`col_mon` + `T`.`col_mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-plus-interval-105]
SELECT `T`.`col_mon` + `T`.`col_y2mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;


-- INTERVAL - INTERVAL -> INTERVAL
--#[interval-minus-interval-00]
SELECT `T`.`col_d` - `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-01]
SELECT `T`.`col_d` - `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-02]
SELECT `T`.`col_d` - `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-03]
SELECT `T`.`col_d` - `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-04]
SELECT `T`.`col_d` - `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-05]
SELECT `T`.`col_d` - `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-06]
SELECT `T`.`col_d` - `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-07]
SELECT `T`.`col_d` - `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-08]
SELECT `T`.`col_d` - `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-09]
SELECT `T`.`col_d` - `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-minus-interval-10]
SELECT `T`.`col_h` - `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-11]
SELECT `T`.`col_h` - `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-12]
SELECT `T`.`col_h` - `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-13]
SELECT `T`.`col_h` - `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-14]
SELECT `T`.`col_h` - `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-15]
SELECT `T`.`col_h` - `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-16]
SELECT `T`.`col_h` - `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-17]
SELECT `T`.`col_h` - `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-18]
SELECT `T`.`col_h` - `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-19]
SELECT `T`.`col_h` - `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-minus-interval-20]
SELECT `T`.`col_min` - `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-21]
SELECT `T`.`col_min` - `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-22]
SELECT `T`.`col_min` - `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-23]
SELECT `T`.`col_min` - `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-24]
SELECT `T`.`col_min` - `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-25]
SELECT `T`.`col_min` - `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-26]
SELECT `T`.`col_min` - `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-27]
SELECT `T`.`col_min` - `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-28]
SELECT `T`.`col_min` - `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-29]
SELECT `T`.`col_min` - `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-minus-interval-30]
SELECT `T`.`col_s` - `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-31]
SELECT `T`.`col_s` - `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-32]
SELECT `T`.`col_s` - `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-33]
SELECT `T`.`col_s` - `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-34]
SELECT `T`.`col_s` - `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-35]
SELECT `T`.`col_s` - `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-36]
SELECT `T`.`col_s` - `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-37]
SELECT `T`.`col_s` - `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-38]
SELECT `T`.`col_s` - `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-39]
SELECT `T`.`col_s` - `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-minus-interval-40]
SELECT `T`.`col_d2h` - `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-41]
SELECT `T`.`col_d2h` - `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-42]
SELECT `T`.`col_d2h` - `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-43]
SELECT `T`.`col_d2h` - `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-44]
SELECT `T`.`col_d2h` - `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-45]
SELECT `T`.`col_d2h` - `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-46]
SELECT `T`.`col_d2h` - `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-47]
SELECT `T`.`col_d2h` - `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-48]
SELECT `T`.`col_d2h` - `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-49]
SELECT `T`.`col_d2h` - `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-minus-interval-50]
SELECT `T`.`col_d2min` - `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-51]
SELECT `T`.`col_d2min` - `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-52]
SELECT `T`.`col_d2min` - `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-53]
SELECT `T`.`col_d2min` - `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-54]
SELECT `T`.`col_d2min` - `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-55]
SELECT `T`.`col_d2min` - `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-56]
SELECT `T`.`col_d2min` - `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-57]
SELECT `T`.`col_d2min` - `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-58]
SELECT `T`.`col_d2min` - `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-59]
SELECT `T`.`col_d2min` - `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-minus-interval-60]
SELECT `T`.`col_d2s` - `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-61]
SELECT `T`.`col_d2s` - `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-62]
SELECT `T`.`col_d2s` - `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-63]
SELECT `T`.`col_d2s` - `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-64]
SELECT `T`.`col_d2s` - `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-65]
SELECT `T`.`col_d2s` - `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-66]
SELECT `T`.`col_d2s` - `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-67]
SELECT `T`.`col_d2s` - `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-68]
SELECT `T`.`col_d2s` - `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-69]
SELECT `T`.`col_d2s` - `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-minus-interval-70]
SELECT `T`.`col_h2min` - `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-71]
SELECT `T`.`col_h2min` - `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-72]
SELECT `T`.`col_h2min` - `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-73]
SELECT `T`.`col_h2min` - `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-74]
SELECT `T`.`col_h2min` - `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-75]
SELECT `T`.`col_h2min` - `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-76]
SELECT `T`.`col_h2min` - `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-77]
SELECT `T`.`col_h2min` - `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-78]
SELECT `T`.`col_h2min` - `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-79]
SELECT `T`.`col_h2min` - `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-minus-interval-80]
SELECT `T`.`col_h2s` - `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-81]
SELECT `T`.`col_h2s` - `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-82]
SELECT `T`.`col_h2s` - `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-83]
SELECT `T`.`col_h2s` - `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-84]
SELECT `T`.`col_h2s` - `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-85]
SELECT `T`.`col_h2s` - `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-86]
SELECT `T`.`col_h2s` - `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-87]
SELECT `T`.`col_h2s` - `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-88]
SELECT `T`.`col_h2s` - `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-89]
SELECT `T`.`col_h2s` - `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-minus-interval-90]
SELECT `T`.`col_m2s` - `T`.`col_d` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-91]
SELECT `T`.`col_m2s` - `T`.`col_h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-92]
SELECT `T`.`col_m2s` - `T`.`col_min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-93]
SELECT `T`.`col_m2s` - `T`.`col_s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-94]
SELECT `T`.`col_m2s` - `T`.`col_d2h` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-95]
SELECT `T`.`col_m2s` - `T`.`col_d2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-96]
SELECT `T`.`col_m2s` - `T`.`col_d2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-97]
SELECT `T`.`col_m2s` - `T`.`col_h2min` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-98]
SELECT `T`.`col_m2s` - `T`.`col_h2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-99]
SELECT `T`.`col_m2s` - `T`.`col_m2s` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

--#[interval-minus-interval-100]
SELECT `T`.`col_y` - `T`.`col_y` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-101]
SELECT `T`.`col_y` - `T`.`col_mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-102]
SELECT `T`.`col_y` - `T`.`col_y2mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-103]
SELECT `T`.`col_mon` - `T`.`col_y` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-104]
SELECT `T`.`col_mon` - `T`.`col_mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;
--#[interval-minus-interval-105]
SELECT `T`.`col_mon` - `T`.`col_y2mon` AS `res` FROM `default`.`T_INTERVALS` AS `T`;

-- INTERVAL * NUMERIC -> INTERVAL and INTERVAL / NUMERIC -> INTERVAL and NUMERIC * INTERVAL -> INTERVAL
-- TODO pending PLK impl https://github.com/partiql/partiql-lang-kotlin/issues/1779
