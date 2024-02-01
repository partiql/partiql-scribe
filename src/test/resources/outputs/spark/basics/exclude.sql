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
