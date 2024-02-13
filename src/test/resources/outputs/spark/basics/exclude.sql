--#[exclude-00]
SELECT `$__EXCLUDE_ALIAS__`.`t`.* FROM (SELECT STRUCT(STRUCT(STRUCT(`t`.`flds`.`a`.`field_x` AS `field_x`, `t`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t`.`flds`.`b`.`field_x` AS `field_x`, `t`.`flds`.`b`.`field_y` AS `field_y`) AS `b`, STRUCT(`t`.`flds`.`c`.`field_x` AS `field_x`, `t`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`) AS `t` FROM `default`.`EXCLUDE_T` AS `t`) AS `$__EXCLUDE_ALIAS__`;

--#[exclude-01]
SELECT `$__EXCLUDE_ALIAS__`.`t`.* FROM (SELECT STRUCT(`t`.`foo` AS `foo`) AS `t` FROM `default`.`EXCLUDE_T` AS `t`) AS `$__EXCLUDE_ALIAS__`;

--#[exclude-02]
SELECT `$__EXCLUDE_ALIAS__`.`t`.* FROM (SELECT STRUCT(STRUCT(STRUCT(`t`.`flds`.`a`.`field_x` AS `field_x`, `t`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t`.`flds`.`c`.`field_x` AS `field_x`, `t`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t`.`foo` AS `foo`) AS `t` FROM `default`.`EXCLUDE_T` AS `t`) AS `$__EXCLUDE_ALIAS__`;

--#[exclude-03]
SELECT `$__EXCLUDE_ALIAS__`.`t`.* FROM (SELECT STRUCT(STRUCT(STRUCT(`t`.`flds`.`a`.`field_x` AS `field_x`, `t`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t`.`flds`.`b`.`field_x` AS `field_x`, `t`.`flds`.`b`.`field_y` AS `field_y`) AS `b`, STRUCT(`t`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t`.`foo` AS `foo`) AS `t` FROM `default`.`EXCLUDE_T` AS `t`) AS `$__EXCLUDE_ALIAS__`;

--#[exclude-04]
SELECT `$__EXCLUDE_ALIAS__`.`t`.* FROM (SELECT STRUCT(STRUCT(STRUCT(`t`.`flds`.`a`.`field_x` AS `field_x`, `t`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t`.`foo` AS `foo`) AS `t` FROM `default`.`EXCLUDE_T` AS `t`) AS `$__EXCLUDE_ALIAS__`;

--#[exclude-05]
SELECT `$__EXCLUDE_ALIAS__`.`t`.* FROM (SELECT STRUCT(STRUCT(STRUCT(`t`.`flds`.`a`.`field_x` AS `field_x`, `t`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t`.`flds`.`b`.`field_x` AS `field_x`, `t`.`flds`.`b`.`field_y` AS `field_y`) AS `b`, STRUCT(`t`.`flds`.`c`.`field_x` AS `field_x`) AS `c`) AS `flds`, `t`.`foo` AS `foo`) AS `t` FROM `default`.`EXCLUDE_T` AS `t`) AS `$__EXCLUDE_ALIAS__`;

--#[exclude-06]
-- Exclude all the fields of `t.flds.c`
SELECT `$__EXCLUDE_ALIAS__`.`t`.* FROM (SELECT STRUCT(STRUCT(STRUCT(`t`.`flds`.`a`.`field_x` AS `field_x`, `t`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t`.`flds`.`b`.`field_x` AS `field_x`, `t`.`flds`.`b`.`field_y` AS `field_y`) AS `b`, STRUCT() AS `c`) AS `flds`, `t`.`foo` AS `foo`) AS `t` FROM `default`.`EXCLUDE_T` AS `t`) AS `$__EXCLUDE_ALIAS__`;

-- START OF EXCLUDE with COLLECTION WILDCARD
--#[exclude-07]
SELECT `$__EXCLUDE_ALIAS__`.`t`.* FROM (SELECT STRUCT(`transform`(`t`.`a`, ___coll_wildcard___ -> STRUCT(___coll_wildcard___.`field_y` AS `field_y`)) AS `a`, `t`.`foo` AS `foo`) AS `t` FROM `default`.`EXCLUDE_T_COLL_WILDCARD` AS `t`) AS `$__EXCLUDE_ALIAS__`;

--#[exclude-08]
SELECT `$__EXCLUDE_ALIAS__`.`t`.* FROM (SELECT STRUCT(`transform`(`t`.`a`, ___coll_wildcard___ -> STRUCT(___coll_wildcard___.`field_x` AS `field_x`)) AS `a`, `t`.`foo` AS `foo`) AS `t` FROM `default`.`EXCLUDE_T_COLL_WILDCARD` AS `t`) AS `$__EXCLUDE_ALIAS__`;

--#[exclude-09]
-- EXCLUDE with JOIN and WHERE clause
SELECT `$__EXCLUDE_ALIAS__`.`t1`.*, `$__EXCLUDE_ALIAS__`.`t2`.* FROM (SELECT STRUCT(STRUCT(STRUCT(`t1`.`flds`.`a`.`field_x` AS `field_x`, `t1`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t1`.`flds`.`b`.`field_x` AS `field_x`, `t1`.`flds`.`b`.`field_y` AS `field_y`) AS `b`, STRUCT(`t1`.`flds`.`c`.`field_x` AS `field_x`, `t1`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`) AS `t1`, STRUCT(STRUCT(STRUCT(`t2`.`flds`.`a`.`field_x` AS `field_x`, `t2`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t2`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t2`.`foo` AS `foo`) AS `t2` FROM `default`.`EXCLUDE_T` AS `t1` INNER JOIN `default`.`EXCLUDE_T` AS `t2` ON true WHERE `t1`.`foo` = `t2`.`foo`) AS `$__EXCLUDE_ALIAS__`;

--#[exclude-10]
-- EXCLUDE with multiple JOIN and WHERE clause
SELECT `$__EXCLUDE_ALIAS__`.`t1`.*, `$__EXCLUDE_ALIAS__`.`t2`.*, `$__EXCLUDE_ALIAS__`.`t3`.* FROM (SELECT STRUCT(STRUCT(STRUCT(`t1`.`flds`.`b`.`field_x` AS `field_x`, `t1`.`flds`.`b`.`field_y` AS `field_y`) AS `b`, STRUCT(`t1`.`flds`.`c`.`field_x` AS `field_x`, `t1`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t1`.`foo` AS `foo`) AS `t1`, STRUCT(STRUCT(STRUCT(`t2`.`flds`.`a`.`field_x` AS `field_x`, `t2`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t2`.`flds`.`c`.`field_x` AS `field_x`, `t2`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t2`.`foo` AS `foo`) AS `t2`, STRUCT(STRUCT(STRUCT(`t3`.`flds`.`a`.`field_x` AS `field_x`, `t3`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t3`.`flds`.`b`.`field_x` AS `field_x`, `t3`.`flds`.`b`.`field_y` AS `field_y`) AS `b`) AS `flds`, `t3`.`foo` AS `foo`) AS `t3` FROM `default`.`EXCLUDE_T` AS `t1` INNER JOIN `default`.`EXCLUDE_T` AS `t2` ON true INNER JOIN `default`.`EXCLUDE_T` AS `t3` ON true WHERE `t1`.`foo` = `t2`.`foo` AND `t2`.`foo` = `t3`.`foo`) AS `$__EXCLUDE_ALIAS__`;

--#[exclude-11]
-- EXCLUDE with select projection list and multiple JOINs
SELECT `$__EXCLUDE_ALIAS__`.`t1`.`flds` AS `flds`, `$__EXCLUDE_ALIAS__`.`t2`.`flds` AS `flds`, `$__EXCLUDE_ALIAS__`.`t3`.`flds` AS `flds` FROM (SELECT STRUCT(STRUCT(STRUCT(`t1`.`flds`.`b`.`field_x` AS `field_x`, `t1`.`flds`.`b`.`field_y` AS `field_y`) AS `b`, STRUCT(`t1`.`flds`.`c`.`field_x` AS `field_x`, `t1`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t1`.`foo` AS `foo`) AS `t1`, STRUCT(STRUCT(STRUCT(`t2`.`flds`.`a`.`field_x` AS `field_x`, `t2`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t2`.`flds`.`c`.`field_x` AS `field_x`, `t2`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t2`.`foo` AS `foo`) AS `t2`, STRUCT(STRUCT(STRUCT(`t3`.`flds`.`a`.`field_x` AS `field_x`, `t3`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t3`.`flds`.`b`.`field_x` AS `field_x`, `t3`.`flds`.`b`.`field_y` AS `field_y`) AS `b`) AS `flds`, `t3`.`foo` AS `foo`) AS `t3` FROM `default`.`EXCLUDE_T` AS `t1` INNER JOIN `default`.`EXCLUDE_T` AS `t2` ON true INNER JOIN `default`.`EXCLUDE_T` AS `t3` ON true WHERE `t1`.`foo` = `t2`.`foo` AND `t2`.`foo` = `t3`.`foo`) AS `$__EXCLUDE_ALIAS__`;
