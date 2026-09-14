--#[in-00]
SELECT `T`.`a` AS `a`, `T`.`b` AS `b`, `T`.`c` AS `c`, `T`.`d` AS `d`, `T`.`x` AS `x`, `T`.`array` AS `array`, `T`.`z` AS `z`, `T`.`v` AS `v`, `T`.`timestamp_1` AS `timestamp_1`, `T`.`timestamp_2` AS `timestamp_2` FROM `default`.`T` AS `T` WHERE `T`.`b` IN (1, 2);

--#[in-01]
SELECT `T`.`a` AS `a`, `T`.`b` AS `b`, `T`.`c` AS `c`, `T`.`d` AS `d`, `T`.`x` AS `x`, `T`.`array` AS `array`, `T`.`z` AS `z`, `T`.`v` AS `v`, `T`.`timestamp_1` AS `timestamp_1`, `T`.`timestamp_2` AS `timestamp_2` FROM `default`.`T` AS `T` WHERE `T`.`b` IN (1, 2);

--#[in-02]
SELECT `T`.`a` AS `a`, `T`.`b` AS `b`, `T`.`c` AS `c`, `T`.`d` AS `d`, `T`.`x` AS `x`, `T`.`array` AS `array`, `T`.`z` AS `z`, `T`.`v` AS `v`, `T`.`timestamp_1` AS `timestamp_1`, `T`.`timestamp_2` AS `timestamp_2` FROM `default`.`T` AS `T` WHERE `T`.`b` IN (1, 2);

--#[in-03]
SELECT `T`.`a` AS `a`, `T`.`b` AS `b`, `T`.`c` AS `c`, `T`.`d` AS `d`, `T`.`x` AS `x`, `T`.`array` AS `array`, `T`.`z` AS `z`, `T`.`v` AS `v`, `T`.`timestamp_1` AS `timestamp_1`, `T`.`timestamp_2` AS `timestamp_2` FROM `default`.`T` AS `T` WHERE `T`.`b` NOT IN (1, 2);

--#[in-04]
SELECT `T`.`a` AS `a`, `T`.`b` AS `b`, `T`.`c` AS `c`, `T`.`d` AS `d`, `T`.`x` AS `x`, `T`.`array` AS `array`, `T`.`z` AS `z`, `T`.`v` AS `v`, `T`.`timestamp_1` AS `timestamp_1`, `T`.`timestamp_2` AS `timestamp_2` FROM `default`.`T` AS `T` WHERE `T`.`b` NOT IN (1, 2);

--#[in-05]
SELECT `T`.`a` AS `a`, `T`.`b` AS `b`, `T`.`c` AS `c`, `T`.`d` AS `d`, `T`.`x` AS `x`, `T`.`array` AS `array`, `T`.`z` AS `z`, `T`.`v` AS `v`, `T`.`timestamp_1` AS `timestamp_1`, `T`.`timestamp_2` AS `timestamp_2` FROM `default`.`T` AS `T` WHERE `T`.`b` NOT IN (1, 2);

--#[in-06]
SELECT `T`.`a` AS `a`, `T`.`b` AS `b`, `T`.`c` AS `c`, `T`.`d` AS `d`, `T`.`x` AS `x`, `T`.`array` AS `array`, `T`.`z` AS `z`, `T`.`v` AS `v`, `T`.`timestamp_1` AS `timestamp_1`, `T`.`timestamp_2` AS `timestamp_2` FROM `default`.`T` AS `T` WHERE (`T`.`b`, `T`.`c`) IN ((1, 'hello'), (2, 'world'));

--#[in-07]
SELECT `T`.`a` AS `a`, `T`.`b` AS `b`, `T`.`c` AS `c`, `T`.`d` AS `d`, `T`.`x` AS `x`, `T`.`array` AS `array`, `T`.`z` AS `z`, `T`.`v` AS `v`, `T`.`timestamp_1` AS `timestamp_1`, `T`.`timestamp_2` AS `timestamp_2` FROM `default`.`T` AS `T` WHERE (`T`.`b`, `T`.`c`) NOT IN ((1, 'hello'), (2, 'world'));


--#[in-08]
SELECT `T`.`a` AS `a`, `T`.`b` AS `b`, `T`.`c` AS `c`, `T`.`d` AS `d`, `T`.`x` AS `x`, `T`.`array` AS `array`, `T`.`z` AS `z`, `T`.`v` AS `v`, `T`.`timestamp_1` AS `timestamp_1`, `T`.`timestamp_2` AS `timestamp_2` FROM `default`.`T` AS `T` WHERE `array_contains`(`T`.`array`, `T`.`b`);

--#[in-09]
SELECT `T`.`a` AS `a`, `T`.`b` AS `b`, `T`.`c` AS `c`, `T`.`d` AS `d`, `T`.`x` AS `x`, `T`.`array` AS `array`, `T`.`z` AS `z`, `T`.`v` AS `v`, `T`.`timestamp_1` AS `timestamp_1`, `T`.`timestamp_2` AS `timestamp_2` FROM `default`.`T` AS `T` WHERE NOT (`array_contains`(`T`.`array`, `T`.`b`));

--#[in-10]
SELECT `T`.`a` AS `a`, `T`.`b` AS `b`, `T`.`c` AS `c`, `T`.`d` AS `d`, `T`.`x` AS `x`, `T`.`array` AS `array`, `T`.`z` AS `z`, `T`.`v` AS `v`, `T`.`timestamp_1` AS `timestamp_1`, `T`.`timestamp_2` AS `timestamp_2` FROM `default`.`T` AS `T` WHERE `T`.`b` IN (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T`);

--#[in-11]
SELECT `T`.`a` AS `a`, `T`.`b` AS `b`, `T`.`c` AS `c`, `T`.`d` AS `d`, `T`.`x` AS `x`, `T`.`array` AS `array`, `T`.`z` AS `z`, `T`.`v` AS `v`, `T`.`timestamp_1` AS `timestamp_1`, `T`.`timestamp_2` AS `timestamp_2` FROM `default`.`T` AS `T` WHERE `T`.`b` NOT IN (SELECT `T`.`b` AS `b` FROM `default`.`T` AS `T`);
