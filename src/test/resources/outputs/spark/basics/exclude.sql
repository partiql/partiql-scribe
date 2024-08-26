--#[exclude-00]
SELECT `t`.`flds` AS `flds` FROM `default`.`EXCLUDE_T` AS `t`;

--#[exclude-01]
SELECT `t`.`foo` AS `foo` FROM `default`.`EXCLUDE_T` AS `t`;

--#[exclude-02]
SELECT `$__EXCLUDE_ALIAS__`.`t`.`flds` AS `flds`, `$__EXCLUDE_ALIAS__`.`t`.`foo` AS `foo` FROM (SELECT STRUCT(STRUCT(STRUCT(`t`.`flds`.`a`.`field_x` AS `field_x`, `t`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t`.`flds`.`c`.`field_x` AS `field_x`, `t`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t`.`foo` AS `foo`) AS `t` FROM `default`.`EXCLUDE_T` AS `t`) AS `$__EXCLUDE_ALIAS__`;

--#[exclude-03]
SELECT `$__EXCLUDE_ALIAS__`.`t`.`flds` AS `flds`, `$__EXCLUDE_ALIAS__`.`t`.`foo` AS `foo` FROM (SELECT STRUCT(STRUCT(STRUCT(`t`.`flds`.`a`.`field_x` AS `field_x`, `t`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t`.`flds`.`b`.`field_x` AS `field_x`, `t`.`flds`.`b`.`field_y` AS `field_y`) AS `b`, STRUCT(`t`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t`.`foo` AS `foo`) AS `t` FROM `default`.`EXCLUDE_T` AS `t`) AS `$__EXCLUDE_ALIAS__`;

--#[exclude-04]
SELECT `$__EXCLUDE_ALIAS__`.`t`.`flds` AS `flds`, `$__EXCLUDE_ALIAS__`.`t`.`foo` AS `foo` FROM (SELECT STRUCT(STRUCT(STRUCT(`t`.`flds`.`a`.`field_x` AS `field_x`, `t`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t`.`foo` AS `foo`) AS `t` FROM `default`.`EXCLUDE_T` AS `t`) AS `$__EXCLUDE_ALIAS__`;

--#[exclude-05]
SELECT `$__EXCLUDE_ALIAS__`.`t`.`flds` AS `flds`, `$__EXCLUDE_ALIAS__`.`t`.`foo` AS `foo` FROM (SELECT STRUCT(STRUCT(STRUCT(`t`.`flds`.`a`.`field_x` AS `field_x`, `t`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t`.`flds`.`b`.`field_x` AS `field_x`, `t`.`flds`.`b`.`field_y` AS `field_y`) AS `b`, STRUCT(`t`.`flds`.`c`.`field_x` AS `field_x`) AS `c`) AS `flds`, `t`.`foo` AS `foo`) AS `t` FROM `default`.`EXCLUDE_T` AS `t`) AS `$__EXCLUDE_ALIAS__`;

--#[exclude-06]
-- Exclude all the fields of `t.flds.c`
SELECT `$__EXCLUDE_ALIAS__`.`t`.`flds` AS `flds`, `$__EXCLUDE_ALIAS__`.`t`.`foo` AS `foo` FROM (SELECT STRUCT(STRUCT(STRUCT(`t`.`flds`.`a`.`field_x` AS `field_x`, `t`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t`.`flds`.`b`.`field_x` AS `field_x`, `t`.`flds`.`b`.`field_y` AS `field_y`) AS `b`, STRUCT() AS `c`) AS `flds`, `t`.`foo` AS `foo`) AS `t` FROM `default`.`EXCLUDE_T` AS `t`) AS `$__EXCLUDE_ALIAS__`;

-- START OF EXCLUDE with COLLECTION WILDCARD
--#[exclude-07]
SELECT `$__EXCLUDE_ALIAS__`.`t`.`a` AS `a`, `$__EXCLUDE_ALIAS__`.`t`.`foo` AS `foo` FROM (SELECT STRUCT(`transform`(`t`.`a`, ___coll_wildcard___ -> STRUCT(___coll_wildcard___.`field_y` AS `field_y`)) AS `a`, `t`.`foo` AS `foo`) AS `t` FROM `default`.`EXCLUDE_T_COLL_WILDCARD` AS `t`) AS `$__EXCLUDE_ALIAS__`;

--#[exclude-08]
SELECT `$__EXCLUDE_ALIAS__`.`t`.`a` AS `a`, `$__EXCLUDE_ALIAS__`.`t`.`foo` AS `foo` FROM (SELECT STRUCT(`transform`(`t`.`a`, ___coll_wildcard___ -> STRUCT(___coll_wildcard___.`field_x` AS `field_x`)) AS `a`, `t`.`foo` AS `foo`) AS `t` FROM `default`.`EXCLUDE_T_COLL_WILDCARD` AS `t`) AS `$__EXCLUDE_ALIAS__`;

--#[exclude-09]
-- EXCLUDE with JOIN and WHERE clause
SELECT `$__EXCLUDE_ALIAS__`.`t1`.`flds` AS `flds`, `$__EXCLUDE_ALIAS__`.`t2`.`flds` AS `flds`, `$__EXCLUDE_ALIAS__`.`t2`.`foo` AS `foo` FROM (SELECT STRUCT(STRUCT(STRUCT(`t1`.`flds`.`a`.`field_x` AS `field_x`, `t1`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t1`.`flds`.`b`.`field_x` AS `field_x`, `t1`.`flds`.`b`.`field_y` AS `field_y`) AS `b`, STRUCT(`t1`.`flds`.`c`.`field_x` AS `field_x`, `t1`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`) AS `t1`, STRUCT(STRUCT(STRUCT(`t2`.`flds`.`a`.`field_x` AS `field_x`, `t2`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t2`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t2`.`foo` AS `foo`) AS `t2` FROM `default`.`EXCLUDE_T` AS `t1` INNER JOIN `default`.`EXCLUDE_T` AS `t2` ON true WHERE `t1`.`foo` = `t2`.`foo`) AS `$__EXCLUDE_ALIAS__`;

--#[exclude-10]
-- EXCLUDE with multiple JOIN and WHERE clause
SELECT `$__EXCLUDE_ALIAS__`.`t1`.`flds` AS `flds`, `$__EXCLUDE_ALIAS__`.`t1`.`foo` AS `foo`, `$__EXCLUDE_ALIAS__`.`t2`.`flds` AS `flds`, `$__EXCLUDE_ALIAS__`.`t2`.`foo` AS `foo`, `$__EXCLUDE_ALIAS__`.`t3`.`flds` AS `flds`, `$__EXCLUDE_ALIAS__`.`t3`.`foo` AS `foo` FROM (SELECT STRUCT(STRUCT(STRUCT(`t1`.`flds`.`b`.`field_x` AS `field_x`, `t1`.`flds`.`b`.`field_y` AS `field_y`) AS `b`, STRUCT(`t1`.`flds`.`c`.`field_x` AS `field_x`, `t1`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t1`.`foo` AS `foo`) AS `t1`, STRUCT(STRUCT(STRUCT(`t2`.`flds`.`a`.`field_x` AS `field_x`, `t2`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t2`.`flds`.`c`.`field_x` AS `field_x`, `t2`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t2`.`foo` AS `foo`) AS `t2`, STRUCT(STRUCT(STRUCT(`t3`.`flds`.`a`.`field_x` AS `field_x`, `t3`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t3`.`flds`.`b`.`field_x` AS `field_x`, `t3`.`flds`.`b`.`field_y` AS `field_y`) AS `b`) AS `flds`, `t3`.`foo` AS `foo`) AS `t3` FROM `default`.`EXCLUDE_T` AS `t1` INNER JOIN `default`.`EXCLUDE_T` AS `t2` ON true INNER JOIN `default`.`EXCLUDE_T` AS `t3` ON true WHERE (`t1`.`foo` = `t2`.`foo`) AND (`t2`.`foo` = `t3`.`foo`)) AS `$__EXCLUDE_ALIAS__`;

--#[exclude-11]
-- EXCLUDE with select projection list and multiple JOINs
SELECT `$__EXCLUDE_ALIAS__`.`t1`.`flds` AS `flds`, `$__EXCLUDE_ALIAS__`.`t2`.`flds` AS `flds`, `$__EXCLUDE_ALIAS__`.`t3`.`flds` AS `flds` FROM (SELECT STRUCT(STRUCT(STRUCT(`t1`.`flds`.`b`.`field_x` AS `field_x`, `t1`.`flds`.`b`.`field_y` AS `field_y`) AS `b`, STRUCT(`t1`.`flds`.`c`.`field_x` AS `field_x`, `t1`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t1`.`foo` AS `foo`) AS `t1`, STRUCT(STRUCT(STRUCT(`t2`.`flds`.`a`.`field_x` AS `field_x`, `t2`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t2`.`flds`.`c`.`field_x` AS `field_x`, `t2`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t2`.`foo` AS `foo`) AS `t2`, STRUCT(STRUCT(STRUCT(`t3`.`flds`.`a`.`field_x` AS `field_x`, `t3`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t3`.`flds`.`b`.`field_x` AS `field_x`, `t3`.`flds`.`b`.`field_y` AS `field_y`) AS `b`) AS `flds`, `t3`.`foo` AS `foo`) AS `t3` FROM `default`.`EXCLUDE_T` AS `t1` INNER JOIN `default`.`EXCLUDE_T` AS `t2` ON true INNER JOIN `default`.`EXCLUDE_T` AS `t3` ON true WHERE (`t1`.`foo` = `t2`.`foo`) AND (`t2`.`foo` = `t3`.`foo`)) AS `$__EXCLUDE_ALIAS__`;

-- Tests for EXCLUDE on top-level columns only --
-- Baseline query without `EXCLUDE`
--#[exclude-36]
SELECT `t`.`a` AS `a`, `t`.`b` AS `b`, `t`.`c` AS `c`, `t`.`d` AS `d`, `t`.`e` AS `e`, `t`.`f` AS `f`, `t`.`g` AS `g` FROM `default`.`T_EXCLUDE_TOP_LEVEL` AS `t`;

-- EXCLUDE single top-level column (no `t.a`)
--#[exclude-37]
SELECT `t`.`b` AS `b`, `t`.`c` AS `c`, `t`.`d` AS `d`, `t`.`e` AS `e`, `t`.`f` AS `f`, `t`.`g` AS `g` FROM `default`.`T_EXCLUDE_TOP_LEVEL` AS `t`;

-- EXCLUDE multiple top-level columns (no `t.a` through `t.e`)
--#[exclude-38]
SELECT `t`.`f` AS `f`, `t`.`g` AS `g` FROM `default`.`T_EXCLUDE_TOP_LEVEL` AS `t`;

-- EXCLUDE top-level columns with WHERE
--#[exclude-39]
SELECT `t`.`c` AS `c`, `t`.`d` AS `d`, `t`.`e` AS `e`, `t`.`f` AS `f`, `t`.`g` AS `g` FROM `default`.`T_EXCLUDE_TOP_LEVEL` AS `t` WHERE `t`.`a` AND (`t`.`c` = 'remove');

-- EXCLUDE top-level columns with explicit SELECT list
--#[exclude-40]
SELECT `t`.`c` AS `c`, `t`.`d` AS `d`, `t`.`e` AS `e`, `t`.`f` AS `f`, `t`.`g` AS `g` FROM `default`.`T_EXCLUDE_TOP_LEVEL` AS `t` WHERE `t`.`a` AND (`t`.`c` = 'remove');

-- EXCLUDE top-level with subquery
--#[exclude-41]
SELECT `subq`.`a` AS `a`, `subq`.`b` AS `b`, `subq`.`c` AS `c`, `subq`.`d` AS `d`, `subq`.`e` AS `e`, `subq`.`f` AS `f`, `subq`.`g` AS `g` FROM (SELECT `t`.`a` AS `a`, `t`.`b` AS `b`, `t`.`c` AS `c`, `t`.`d` AS `d`, `t`.`e` AS `e`, `t`.`f` AS `f`, `t`.`g` AS `g`, 'foo' AS `remove_me` FROM `default`.`T_EXCLUDE_TOP_LEVEL` AS `t` WHERE `t`.`a`) AS `subq`;

-- EXCLUDE top-level columns with JOIN
--#[exclude-42]
SELECT `t1`.`b` AS `b`, `t1`.`d` AS `d`, `t1`.`f` AS `f`, `t2`.`a` AS `a`, `t2`.`c` AS `c`, `t2`.`e` AS `e`, `t2`.`g` AS `g` FROM `default`.`T_EXCLUDE_TOP_LEVEL` AS `t1` INNER JOIN `default`.`T_EXCLUDE_TOP_LEVEL` AS `t2` ON true;

-- EXCLUDE top-level columns with JOIN and WHERE
--#[exclude-43]
SELECT `t1`.`b` AS `b`, `t1`.`d` AS `d`, `t1`.`f` AS `f`, `t2`.`a` AS `a`, `t2`.`c` AS `c`, `t2`.`e` AS `e`, `t2`.`g` AS `g` FROM `default`.`T_EXCLUDE_TOP_LEVEL` AS `t1` INNER JOIN `default`.`T_EXCLUDE_TOP_LEVEL` AS `t2` ON true WHERE `t1`.`a` AND `t2`.`a`;

-- EXCLUDE top-level columns with JOIN and WHERE and specified SELECT element
--#[exclude-44]
SELECT `t1`.`b` AS `b`, `t1`.`d` AS `d`, `t1`.`f` AS `f` FROM `default`.`T_EXCLUDE_TOP_LEVEL` AS `t1` INNER JOIN `default`.`T_EXCLUDE_TOP_LEVEL` AS `t2` ON true WHERE `t1`.`a` AND `t2`.`a`;

-- EXCLUDE top-level columns with JOIN and WHERE and specified SELECT elements
--#[exclude-45]
SELECT `t1`.`b` AS `b`, `t1`.`d` AS `d`, `t1`.`f` AS `f`, `t2`.`a` AS `special` FROM `default`.`T_EXCLUDE_TOP_LEVEL` AS `t1` INNER JOIN `default`.`T_EXCLUDE_TOP_LEVEL` AS `t2` ON true WHERE `t1`.`a` AND `t2`.`a`;

-- EXCLUDE top-level columns with multiple JOINs
--#[exclude-46]
SELECT
    `t1`.`a` AS `a`,
    `t2`.`b` AS `b`,
    `t3`.`c` AS `c`,
    `t4`.`d` AS `d`,
    `t5`.`e` AS `e`,
    `t6`.`f` AS `f`,
    `t7`.`g` AS `g`
FROM
    `default`.`T_EXCLUDE_TOP_LEVEL` AS `t1` INNER JOIN
    `default`.`T_EXCLUDE_TOP_LEVEL` AS `t2` ON `t2`.`a` LEFT OUTER JOIN
    `default`.`T_EXCLUDE_TOP_LEVEL` AS `t3` ON `t3`.`a` INNER JOIN
    `default`.`T_EXCLUDE_TOP_LEVEL` AS `t4` ON `t4`.`a` RIGHT OUTER JOIN
    `default`.`T_EXCLUDE_TOP_LEVEL` AS `t5` ON `t5`.`a` FULL OUTER JOIN
    `default`.`T_EXCLUDE_TOP_LEVEL` AS `t6` ON `t6`.`a` INNER JOIN
    `default`.`T_EXCLUDE_TOP_LEVEL` AS `t7` ON true
WHERE `t1`.`a` AND `t2`.`a`;

--#[exclude-49]
-- Exclude two nested fields; same transpiled query (other than table name) as #[exclude-04]
SELECT `$__EXCLUDE_ALIAS__`.`t`.`flds` AS `flds`, `$__EXCLUDE_ALIAS__`.`t`.`foo` AS `foo` FROM (SELECT STRUCT(STRUCT(STRUCT(`t`.`flds`.`a`.`field_x` AS `field_x`, `t`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t`.`foo` AS `foo`) AS `t` FROM `default`.`EXCLUDE_T_NULLABLE` AS `t`) AS `$__EXCLUDE_ALIAS__`;
