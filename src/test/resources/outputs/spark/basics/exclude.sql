--#[exclude-00]
SELECT `t`.* FROM (SELECT STRUCT(STRUCT(`t`.`flds`.`a`.`field_x` AS `field_x`, `t`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t`.`flds`.`b`.`field_x` AS `field_x`, `t`.`flds`.`b`.`field_y` AS `field_y`) AS `b`, STRUCT(`t`.`flds`.`c`.`field_x` AS `field_x`, `t`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds` FROM `default`.`EXCLUDE_T` AS `t`) AS `t`;

--#[exclude-01]
SELECT `t`.* FROM (SELECT `t`.`foo` AS `foo` FROM `default`.`EXCLUDE_T` AS `t`) AS `t`;

--#[exclude-02]
SELECT `t`.* FROM (SELECT STRUCT(STRUCT(`t`.`flds`.`a`.`field_x` AS `field_x`, `t`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t`.`flds`.`c`.`field_x` AS `field_x`, `t`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t`.`foo` AS `foo` FROM `default`.`EXCLUDE_T` AS `t`) AS `t`;

--#[exclude-03]
SELECT `t`.* FROM (SELECT STRUCT(STRUCT(`t`.`flds`.`a`.`field_x` AS `field_x`, `t`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t`.`flds`.`b`.`field_x` AS `field_x`, `t`.`flds`.`b`.`field_y` AS `field_y`) AS `b`, STRUCT(`t`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t`.`foo` AS `foo` FROM `default`.`EXCLUDE_T` AS `t`) AS `t`;

--#[exclude-04]
SELECT `t`.* FROM (SELECT STRUCT(STRUCT(`t`.`flds`.`a`.`field_x` AS `field_x`, `t`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t`.`flds`.`c`.`field_y` AS `field_y`) AS `c`) AS `flds`, `t`.`foo` AS `foo` FROM `default`.`EXCLUDE_T` AS `t`) AS `t`;

--#[exclude-05]
SELECT `t`.* FROM (SELECT STRUCT(STRUCT(`t`.`flds`.`a`.`field_x` AS `field_x`, `t`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t`.`flds`.`b`.`field_x` AS `field_x`, `t`.`flds`.`b`.`field_y` AS `field_y`) AS `b`, STRUCT(`t`.`flds`.`c`.`field_x` AS `field_x`) AS `c`) AS `flds`, `t`.`foo` AS `foo` FROM `default`.`EXCLUDE_T` AS `t`) AS `t`;

--#[exclude-06]
-- Exclude all the fields of `t.flds.c`
SELECT `t`.* FROM (SELECT STRUCT(STRUCT(`t`.`flds`.`a`.`field_x` AS `field_x`, `t`.`flds`.`a`.`field_y` AS `field_y`) AS `a`, STRUCT(`t`.`flds`.`b`.`field_x` AS `field_x`, `t`.`flds`.`b`.`field_y` AS `field_y`) AS `b`, STRUCT() AS `c`) AS `flds`, `t`.`foo` AS `foo` FROM `default`.`EXCLUDE_T` AS `t`) AS `t`;

-- START OF EXCLUDE with COLLECTION WILDCARD
--#[exclude-07]
SELECT `t`.* FROM (SELECT `transform`(`t`.`a`, coll_wildcard -> STRUCT(coll_wildcard.`field_y` AS `field_y`)) AS `a`, `t`.`foo` AS `foo` FROM `default`.`EXCLUDE_T_COLL_WILDCARD` AS `t`) AS `t`;

--#[exclude-08]
SELECT `t`.* FROM (SELECT `transform`(`t`.`a`, coll_wildcard -> STRUCT(coll_wildcard.`field_x` AS `field_x`)) AS `a`, `t`.`foo` AS `foo` FROM `default`.`EXCLUDE_T_COLL_WILDCARD` AS `t`) AS `t`;
