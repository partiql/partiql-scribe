-- ----------------------------------------
--  Select struct columns
-- ----------------------------------------

--#[struct-00]
-- Select a simple struct column
SELECT `T`.`col_struct_simple` AS `col_struct_simple` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[struct-01]
-- Select a nested struct column
SELECT `T`.`col_struct_nested` AS `col_struct_nested` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[struct-02]
-- Select both struct columns
SELECT `T`.`col_struct_simple` AS `col_struct_simple`, `T`.`col_struct_nested` AS `col_struct_nested` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[struct-03]
-- Select struct alongside scalar columns
SELECT `T`.`col_int32` AS `col_int32`, `T`.`col_string` AS `col_string`, `T`.`col_struct_simple` AS `col_struct_simple` FROM `default`.`T_ALL_TYPES` AS `T`;

-- ----------------------------------------
--  Path navigation — simple struct
-- ----------------------------------------

--#[struct-04]
-- Access a field of a simple struct
SELECT `T`.`col_struct_simple`.`level1_col_int` AS `level1_col_int` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[struct-05]
-- Access multiple fields of a simple struct
SELECT `T`.`col_struct_simple`.`level1_col_int` AS `level1_col_int`, `T`.`col_struct_simple`.`level1_col_string` AS `level1_col_string` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[struct-06]
-- Access all fields of a simple struct
SELECT `T`.`col_struct_simple`.`level1_col_int` AS `level1_col_int`, `T`.`col_struct_simple`.`level1_col_double` AS `level1_col_double`, `T`.`col_struct_simple`.`level1_col_string` AS `level1_col_string`, `T`.`col_struct_simple`.`level1_col_timestamp` AS `level1_col_timestamp` FROM `default`.`T_ALL_TYPES` AS `T`;

-- ----------------------------------------
--  Path navigation — nested struct
-- ----------------------------------------

-- Single field access on nested struct (e.g. T.col_struct_nested.level1_col_int) covered by struct-04 pattern

--#[struct-07]
-- Access the nested struct itself
SELECT `T`.`col_struct_nested`.`level1_col_struct` AS `level1_col_struct` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[struct-08]
-- Access a field within the nested struct (2-level path)
SELECT `T`.`col_struct_nested`.`level1_col_struct`.`level2_col_int` AS `level2_col_int` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[struct-09]
-- Access multiple fields within the nested struct
SELECT `T`.`col_struct_nested`.`level1_col_struct`.`level2_col_int` AS `level2_col_int`, `T`.`col_struct_nested`.`level1_col_struct`.`level2_col_string` AS `level2_col_string` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[struct-10]
-- Mix top-level and nested field access
SELECT `T`.`col_struct_nested`.`level1_col_int` AS `level1_col_int`, `T`.`col_struct_nested`.`level1_col_struct`.`level2_col_double` AS `level2_col_double` FROM `default`.`T_ALL_TYPES` AS `T`;

-- ----------------------------------------
--  Struct wildcard (dot-star)
-- ----------------------------------------

--#[struct-11]
-- Wildcard on simple struct
SELECT `T`.`col_struct_simple`.`level1_col_int` AS `level1_col_int`, `T`.`col_struct_simple`.`level1_col_double` AS `level1_col_double`, `T`.`col_struct_simple`.`level1_col_string` AS `level1_col_string`, `T`.`col_struct_simple`.`level1_col_timestamp` AS `level1_col_timestamp` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[struct-12]
-- Wildcard on nested struct
SELECT `T`.`col_struct_nested`.`level1_col_int` AS `level1_col_int`, `T`.`col_struct_nested`.`level1_col_double` AS `level1_col_double`, `T`.`col_struct_nested`.`level1_col_string` AS `level1_col_string`, `T`.`col_struct_nested`.`level1_col_timestamp` AS `level1_col_timestamp`, `T`.`col_struct_nested`.`level1_col_struct` AS `level1_col_struct` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[struct-13]
-- Wildcard on inner nested struct
SELECT `T`.`col_struct_nested`.`level1_col_struct`.`level2_col_int` AS `level2_col_int`, `T`.`col_struct_nested`.`level1_col_struct`.`level2_col_double` AS `level2_col_double`, `T`.`col_struct_nested`.`level1_col_struct`.`level2_col_string` AS `level2_col_string`, `T`.`col_struct_nested`.`level1_col_struct`.`level2_col_timestamp` AS `level2_col_timestamp` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[struct-14]
-- Wildcard with other columns
SELECT `T`.`col_int32` AS `col_int32`, `T`.`col_struct_simple`.`level1_col_int` AS `level1_col_int`, `T`.`col_struct_simple`.`level1_col_double` AS `level1_col_double`, `T`.`col_struct_simple`.`level1_col_string` AS `level1_col_string`, `T`.`col_struct_simple`.`level1_col_timestamp` AS `level1_col_timestamp` FROM `default`.`T_ALL_TYPES` AS `T`;

-- ----------------------------------------
--  Struct construction in SELECT
-- ----------------------------------------

--#[struct-15]
-- Construct a struct literal in SELECT
SELECT NAMED_STRUCT('a', `T`.`col_int32`, 'b', `T`.`col_string`) AS `constructed` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[struct-16]
-- Construct a struct from struct fields
SELECT NAMED_STRUCT('x', `T`.`col_struct_simple`.`level1_col_int`, 'y', `T`.`col_struct_simple`.`level1_col_string`) AS `constructed` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[struct-17]
-- Construct a nested struct literal
SELECT NAMED_STRUCT('outer', NAMED_STRUCT('inner_val', `T`.`col_struct_nested`.`level1_col_struct`.`level2_col_int`)) AS `constructed` FROM `default`.`T_ALL_TYPES` AS `T`;

-- ----------------------------------------
--  Filtering on struct fields
-- ----------------------------------------

--#[struct-18]
-- WHERE on a simple struct field
SELECT `T`.`col_bool` AS `col_bool`, `T`.`col_int16` AS `col_int16`, `T`.`col_int32` AS `col_int32`, `T`.`col_int64` AS `col_int64`, `T`.`col_float32` AS `col_float32`, `T`.`col_float64` AS `col_float64`, `T`.`col_decimal` AS `col_decimal`, `T`.`col_string` AS `col_string`, `T`.`col_char` AS `col_char`, `T`.`col_date` AS `col_date`, `T`.`col_time` AS `col_time`, `T`.`col_timez` AS `col_timez`, `T`.`col_timestamp` AS `col_timestamp`, `T`.`col_timestampz` AS `col_timestampz`, `T`.`col_blob` AS `col_blob`, `T`.`col_clob` AS `col_clob`, `T`.`col_list` AS `col_list`, `T`.`col_bag` AS `col_bag`, `T`.`col_struct_simple` AS `col_struct_simple`, `T`.`col_struct_nested` AS `col_struct_nested`, `T`.`col_any` AS `col_any` FROM `default`.`T_ALL_TYPES` AS `T` WHERE `T`.`col_struct_simple`.`level1_col_int` > 10;

--#[struct-19]
-- WHERE on a nested struct field
SELECT `T`.`col_bool` AS `col_bool`, `T`.`col_int16` AS `col_int16`, `T`.`col_int32` AS `col_int32`, `T`.`col_int64` AS `col_int64`, `T`.`col_float32` AS `col_float32`, `T`.`col_float64` AS `col_float64`, `T`.`col_decimal` AS `col_decimal`, `T`.`col_string` AS `col_string`, `T`.`col_char` AS `col_char`, `T`.`col_date` AS `col_date`, `T`.`col_time` AS `col_time`, `T`.`col_timez` AS `col_timez`, `T`.`col_timestamp` AS `col_timestamp`, `T`.`col_timestampz` AS `col_timestampz`, `T`.`col_blob` AS `col_blob`, `T`.`col_clob` AS `col_clob`, `T`.`col_list` AS `col_list`, `T`.`col_bag` AS `col_bag`, `T`.`col_struct_simple` AS `col_struct_simple`, `T`.`col_struct_nested` AS `col_struct_nested`, `T`.`col_any` AS `col_any` FROM `default`.`T_ALL_TYPES` AS `T` WHERE `T`.`col_struct_nested`.`level1_col_struct`.`level2_col_string` = 'hello';

--#[struct-20]
-- WHERE with multiple struct field conditions
SELECT `T`.`col_bool` AS `col_bool`, `T`.`col_int16` AS `col_int16`, `T`.`col_int32` AS `col_int32`, `T`.`col_int64` AS `col_int64`, `T`.`col_float32` AS `col_float32`, `T`.`col_float64` AS `col_float64`, `T`.`col_decimal` AS `col_decimal`, `T`.`col_string` AS `col_string`, `T`.`col_char` AS `col_char`, `T`.`col_date` AS `col_date`, `T`.`col_time` AS `col_time`, `T`.`col_timez` AS `col_timez`, `T`.`col_timestamp` AS `col_timestamp`, `T`.`col_timestampz` AS `col_timestampz`, `T`.`col_blob` AS `col_blob`, `T`.`col_clob` AS `col_clob`, `T`.`col_list` AS `col_list`, `T`.`col_bag` AS `col_bag`, `T`.`col_struct_simple` AS `col_struct_simple`, `T`.`col_struct_nested` AS `col_struct_nested`, `T`.`col_any` AS `col_any` FROM `default`.`T_ALL_TYPES` AS `T` WHERE (`T`.`col_struct_simple`.`level1_col_int` > 0) AND (`T`.`col_struct_nested`.`level1_col_double` < 100.0);

--#[struct-21]
-- IS NULL check on struct field
SELECT `T`.`col_bool` AS `col_bool`, `T`.`col_int16` AS `col_int16`, `T`.`col_int32` AS `col_int32`, `T`.`col_int64` AS `col_int64`, `T`.`col_float32` AS `col_float32`, `T`.`col_float64` AS `col_float64`, `T`.`col_decimal` AS `col_decimal`, `T`.`col_string` AS `col_string`, `T`.`col_char` AS `col_char`, `T`.`col_date` AS `col_date`, `T`.`col_time` AS `col_time`, `T`.`col_timez` AS `col_timez`, `T`.`col_timestamp` AS `col_timestamp`, `T`.`col_timestampz` AS `col_timestampz`, `T`.`col_blob` AS `col_blob`, `T`.`col_clob` AS `col_clob`, `T`.`col_list` AS `col_list`, `T`.`col_bag` AS `col_bag`, `T`.`col_struct_simple` AS `col_struct_simple`, `T`.`col_struct_nested` AS `col_struct_nested`, `T`.`col_any` AS `col_any` FROM `default`.`T_ALL_TYPES` AS `T` WHERE `T`.`col_struct_simple`.`level1_col_string` IS NULL;

--#[struct-22]
-- IS NOT NULL check on nested struct field
SELECT `T`.`col_bool` AS `col_bool`, `T`.`col_int16` AS `col_int16`, `T`.`col_int32` AS `col_int32`, `T`.`col_int64` AS `col_int64`, `T`.`col_float32` AS `col_float32`, `T`.`col_float64` AS `col_float64`, `T`.`col_decimal` AS `col_decimal`, `T`.`col_string` AS `col_string`, `T`.`col_char` AS `col_char`, `T`.`col_date` AS `col_date`, `T`.`col_time` AS `col_time`, `T`.`col_timez` AS `col_timez`, `T`.`col_timestamp` AS `col_timestamp`, `T`.`col_timestampz` AS `col_timestampz`, `T`.`col_blob` AS `col_blob`, `T`.`col_clob` AS `col_clob`, `T`.`col_list` AS `col_list`, `T`.`col_bag` AS `col_bag`, `T`.`col_struct_simple` AS `col_struct_simple`, `T`.`col_struct_nested` AS `col_struct_nested`, `T`.`col_any` AS `col_any` FROM `default`.`T_ALL_TYPES` AS `T` WHERE `T`.`col_struct_nested`.`level1_col_struct`.`level2_col_timestamp` IS NOT NULL;

-- ----------------------------------------
--  Aliasing struct fields
-- ----------------------------------------

-- Single alias on struct path (e.g. T.col_struct_simple.level1_col_int AS x) covered by struct-04 pattern

--#[struct-23]
-- Alias multiple struct paths at different nesting levels
SELECT `T`.`col_struct_simple`.`level1_col_int` AS `s_int`, `T`.`col_struct_nested`.`level1_col_struct`.`level2_col_string` AS `n_str` FROM `default`.`T_ALL_TYPES` AS `T`;

-- ----------------------------------------
--  Struct fields in ORDER BY / GROUP BY
-- ----------------------------------------

--#[struct-24]
-- ORDER BY a struct field
SELECT `T`.`col_struct_simple`.`level1_col_int` AS `level1_col_int`, `T`.`col_string` AS `col_string` FROM `default`.`T_ALL_TYPES` AS `T` ORDER BY `T`.`col_struct_simple`.`level1_col_int` ASC NULLS LAST;

--#[struct-25]
-- GROUP BY a struct field
SELECT `T`.`col_struct_simple`.`level1_col_string` AS `level1_col_string`, `count`(1) AS `cnt` FROM `default`.`T_ALL_TYPES` AS `T` GROUP BY `T`.`col_struct_simple`.`level1_col_string`;

-- ----------------------------------------
--  Struct with JOIN
-- ----------------------------------------

--#[struct-26]
-- Join on struct fields
SELECT `t1`.`col_struct_simple`.`level1_col_int` AS `level1_col_int`, `T2`.`col_struct_nested`.`level1_col_string` AS `level1_col_string` FROM `default`.`T_ALL_TYPES` AS `t1` INNER JOIN `default`.`T_ALL_TYPES` AS `T2` ON `t1`.`col_struct_simple`.`level1_col_int` = `T2`.`col_struct_nested`.`level1_col_int`;

--#[struct-27]
-- Left join with nested struct field in condition
SELECT `t1`.`col_struct_simple` AS `col_struct_simple`, `T2`.`col_struct_nested`.`level1_col_struct`.`level2_col_int` AS `level2_col_int` FROM `default`.`T_ALL_TYPES` AS `t1` LEFT JOIN `default`.`T_ALL_TYPES` AS `T2` ON `t1`.`col_struct_simple`.`level1_col_string` = `T2`.`col_struct_nested`.`level1_col_struct`.`level2_col_string`;

-- ----------------------------------------
--  Struct in subquery
-- ----------------------------------------

--#[struct-28]
-- Subquery selecting struct fields
SELECT `T`.`col_struct_simple`.`level1_col_int` AS `level1_col_int` FROM `default`.`T_ALL_TYPES` AS `T` WHERE `T`.`col_struct_simple`.`level1_col_int` > (SELECT `T2`.`col_struct_nested`.`level1_col_struct`.`level2_col_int` AS `level2_col_int` FROM `default`.`T_ALL_TYPES` AS `T2` WHERE `T2`.`col_struct_nested`.`level1_col_struct`.`level2_col_int` = 1);

--#[struct-29]
-- Struct field in IN subquery
SELECT `T`.`col_struct_nested`.`level1_col_struct`.`level2_col_int` AS `level2_col_int` FROM `default`.`T_ALL_TYPES` AS `T` WHERE `T`.`col_struct_simple`.`level1_col_int` IN (SELECT `T2`.`col_struct_nested`.`level1_col_struct`.`level2_col_int` AS `level2_col_int` FROM `default`.`T_ALL_TYPES` AS `T2`);

-- ----------------------------------------
--  Struct construction with real values
-- ----------------------------------------

--#[struct-30]
-- Construct struct with mixed literal types
SELECT NAMED_STRUCT('count', 42, 'rate', 3.14, 'message', 'hello', 'enabled', false) AS `config` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[struct-31]
-- Construct nested struct with literal values
SELECT NAMED_STRUCT('user', NAMED_STRUCT('id', 100, 'email', 'test@example.com'), 'timestamp', '2023-01-01T00:00:00Z') AS `record` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[struct-32]
-- Construct struct with null values
SELECT NAMED_STRUCT('value', 123, 'optional_field', NULL, 'description', 'test') AS `data` FROM `default`.`T_ALL_TYPES` AS `T`;

--#[struct-33]
-- Construct struct with arithmetic expressions
SELECT NAMED_STRUCT('sum', 10 + 5, 'product', 3 * 7, 'ratio', 100.0 / CAST(4 AS DECIMAL(10,0))) AS `calculations` FROM `default`.`T_ALL_TYPES` AS `T`;
