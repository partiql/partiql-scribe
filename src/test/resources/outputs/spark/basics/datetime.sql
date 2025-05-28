-- DATE
--#[datetime-00]
SELECT DATE '0001-02-03' AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-01]
SELECT DATE '2020-02-03' AS `_1` FROM `default`.`T` AS `T`;

-- TIME -- Spark has no `TIME` data type. Give an error when attempting to construct a SQL time literal
-- --#[datetime-02]
-- SELECT TIME '01:02:03' AS `_1` FROM `default`.`T` AS `T`;
--
-- --#[datetime-03]
-- SELECT TIME '01:02:03.456' AS `_1` FROM `default`.`T` AS `T`;
--
-- --#[datetime-04]
-- SELECT TIME '01:02:03.456-00:00' AS `_1` FROM `default`.`T` AS `T`;
--
-- --#[datetime-05]
-- SELECT TIME '01:02:03.456+00:00' AS `_1` FROM `default`.`T` AS `T`;
--
-- --#[datetime-06]
-- SELECT TIME '01:02:03.456+00:30' AS `_1` FROM `default`.`T` AS `T`;
--
-- --#[datetime-07]
-- SELECT TIME '01:02:03.456-00:30' AS `_1` FROM `default`.`T` AS `T`;

-- TIMESTAMP
--#[datetime-08]
SELECT TIMESTAMP '0001-02-03 04:05:06.78' AS `_1` FROM `default`.`T` AS `T`;

-- previous test includes timezone offset for TIMESTAMP and negative timezone offset is not supported
-- --#[datetime-09]
-- SELECT TIMESTAMP '0001-02-03 04:05:06.78-00:00' AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-10]
SELECT TIMESTAMP WITH TIME ZONE '0001-02-03 04:05:06.78+00:00' AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-11]
SELECT TIMESTAMP WITH TIME ZONE '0001-02-03 04:05:06.78+00:30' AS `_1` FROM `default`.`T` AS `T`;

--#[datetime-12]
SELECT TIMESTAMP WITH TIME ZONE '0001-02-03 04:05:06.78-00:30' AS `_1` FROM `default`.`T` AS `T`;

-- -- Ion Timestamp -> map to SQL timestamp literal
-- --#[datetime-13]
-- SELECT TIMESTAMP '2007-01-01 00:00:00-00:00' AS `_1` FROM `default`.`T` AS `T`;
--
-- --#[datetime-14]
-- SELECT TIMESTAMP '2007-02-23 12:14:33.079-08:00' AS `_1` FROM `default`.`T` AS `T`;
