-- ----------------------------------------
--  Map construction — literal
-- ----------------------------------------

--#[map-00]
-- Construct a map literal with string keys
SELECT MAP('a', 1, 'b', 2, 'c', 3) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-01]
-- Construct a map literal with decimal keys
SELECT MAP(1.1, 'x', 2.2, 'y', 3.3, 'z') AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-02]
-- Construct a map literal referencing columns for values
SELECT MAP('x', CAST(`T`.`col_int32` AS DOUBLE), 'y', `T`.`col_float64`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-03]
-- Construct a map literal referencing columns for keys
SELECT MAP(`T`.`col_string`, `T`.`col_int32`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-04]
-- Construct an empty map literal
SELECT MAP() AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-05]
-- Construct a map literal with integer keys
SELECT MAP(1, 'a', 2, 'b', 3, 'c') AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-06]
-- Construct a map literal with expression as key
SELECT MAP(`T`.`col_int32` + 1, `T`.`col_string`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

-- ----------------------------------------
--  Select map columns
-- ----------------------------------------

--#[map-07]
-- Select map alongside other columns
SELECT `T`.`col_int32` AS `col_int32`, `T`.`col_string` AS `col_string`, `T`.`col_map_str_key` AS `col_map_str_key` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-08]
-- Select map column with float key type
SELECT `T`.`col_map_float_key` AS `col_map_float_key` FROM `default`.`T_ALL_TYPES` AS `T`;

-- ----------------------------------------
--  Bracket notation lookup
-- ----------------------------------------

--#[map-09]
-- Lookup map with bracket notation (string key)
SELECT `T`.`col_map_str_key`['a'] AS `a` FROM `default`.`T_ALL_TYPES` AS `T` WHERE `map_contains_key`(`T`.`col_map_str_key`, 'a');

--#[map-10]
-- Lookup map with bracket notation (float key)
SELECT `T`.`col_map_float_key`[CAST(1.0 AS DOUBLE)] AS `_1` FROM `default`.`T_ALL_TYPES` AS `T` WHERE `map_contains_key`(`T`.`col_map_float_key`, CAST(1.0 AS DOUBLE));

--#[map-11]
-- Lookup map with bracket notation using column as key
SELECT `T`.`col_map_str_key`[`T`.`col_string`] AS `_1` FROM `default`.`T_ALL_TYPES` AS `T` WHERE `map_contains_key`(`T`.`col_map_str_key`, `T`.`col_string`);

-- ----------------------------------------
--  Map functions — map_keys, map_values, map_entries
-- ----------------------------------------

--#[map-12]
-- Get keys from a string-key map
SELECT `map_keys`(`T`.`col_map_str_key`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-13]
-- Get keys from a float-key map
SELECT `map_keys`(`T`.`col_map_float_key`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-14]
-- Get values from a string-key map
SELECT `map_values`(`T`.`col_map_str_key`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-15]
-- Get values from a float-key map
SELECT `map_values`(`T`.`col_map_float_key`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-16]
-- Get entries from a string-key map
SELECT `map_entries`(`T`.`col_map_str_key`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-17]
-- Get entries from a float-key map
SELECT `map_entries`(`T`.`col_map_float_key`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

-- ----------------------------------------
--  Map functions — size, cardinality, exists
-- ----------------------------------------

--#[map-18]
-- Size of a map column
SELECT `size`(`T`.`col_map_str_key`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-19]
-- Size of a map literal
SELECT `size`(MAP('a', 1, 'b', 2, 'c', 3)) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-20]
-- Cardinality of a map column
SELECT `size`(`T`.`col_map_str_key`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-21]
-- Exists on a map column
SELECT `size`(`T`.`col_map_str_key`) > 0 AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

-- ----------------------------------------
--  Map functions — map_contains_key
-- ----------------------------------------

--#[map-22]
-- map_contains_key with string key that exists
SELECT `map_contains_key`(`T`.`col_map_str_key`, 'a') AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-23]
-- map_contains_key with column reference as key
SELECT `map_contains_key`(`T`.`col_map_str_key`, `T`.`col_string`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-24]
-- map_contains_key with float key
SELECT `map_contains_key`(`T`.`col_map_float_key`, `T`.`col_float64`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

-- ----------------------------------------
--  Map functions — map_get
-- ----------------------------------------

--#[map-25]
-- map_get with string key
SELECT `element_at`(`T`.`col_map_str_key`, 'a') AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-26]
-- map_get with column reference as key
SELECT `element_at`(`T`.`col_map_str_key`, `T`.`col_string`) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[map-27]
-- map_get with float key
SELECT `element_at`(`T`.`col_map_float_key`, CAST(1.0 AS DOUBLE)) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T`;

-- ----------------------------------------
--  Filtering
-- ----------------------------------------

--#[map-31]
-- Filter with map lookup comparison
SELECT `T`.`col_int32` AS `col_int32` FROM `default`.`T_ALL_TYPES` AS `T` WHERE `T`.`col_map_str_key`['a'] > 10;

--#[map-32]
-- Filter with map_contains_key
SELECT `T`.`col_int32` AS `col_int32` FROM `default`.`T_ALL_TYPES` AS `T` WHERE `map_contains_key`(`T`.`col_map_str_key`, 'a');

--#[map-33]
-- Filter with map size
SELECT `T`.`col_int32` AS `col_int32` FROM `default`.`T_ALL_TYPES` AS `T` WHERE `size`(`T`.`col_map_str_key`) > 0;

-- ----------------------------------------
--  Aggregation
-- ----------------------------------------

--#[map-34]
-- Group by map lookup
SELECT `T`.`col_map_str_key`['a'] AS `a`, `count`(1) AS `_1` FROM `default`.`T_ALL_TYPES` AS `T` GROUP BY `T`.`col_map_str_key`['a'];