--#[order-by-00]
SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` ORDER BY `T`.`a` ASC NULLS LAST;

--#[order-by-01]
SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` ORDER BY `T`.`a` ASC NULLS LAST, `T`.`b` ASC NULLS LAST;

--#[order-by-02]
SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` ORDER BY `T`.`a` ASC NULLS LAST, `T`.`b` ASC NULLS LAST;

--#[order-by-03]
SELECT `T`.`flds` AS `flds` FROM `default`.`EXCLUDE_T` AS `T` ORDER BY `T`.`flds`.`c`.`field_x` ASC NULLS LAST;

--#[order-by-04]
SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` ORDER BY `T`.`a` ASC NULLS LAST, `T`.`b` DESC NULLS FIRST;

--#[order-by-05]
SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` ORDER BY `T`.`a` ASC NULLS FIRST, `T`.`b` DESC NULLS LAST;

--#[order-by-06]
SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` ORDER BY `T`.`a` ASC NULLS LAST, `T`.`b` DESC NULLS FIRST;

--#[order-by-07]
(SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` ORDER BY `T`.`a` ASC NULLS LAST, `T`.`b` ASC NULLS LAST) UNION DISTINCT (SELECT `T`.`a` AS `a` FROM `default`.`T` AS `T` ORDER BY `T`.`a` ASC NULLS LAST, `T`.`b` ASC NULLS LAST);

--#[order-by-08]
(SELECT `T`.`a` AS `a`, `T`.`b` AS `b` FROM `default`.`T` AS `T` ORDER BY `T`.`a` ASC NULLS LAST, `T`.`b` ASC NULLS LAST) UNION DISTINCT (SELECT `T`.`a` AS `a`, `T`.`b` AS `b` FROM `default`.`T` AS `T` ORDER BY `T`.`a` ASC NULLS LAST, `T`.`b` ASC NULLS LAST) ORDER BY `a` ASC NULLS LAST;

--#[order-by-09]
(SELECT `T`.`a` AS `a`, `T`.`b` AS `b` FROM `default`.`T` AS `T` ORDER BY `T`.`a` ASC NULLS LAST, `T`.`b` ASC NULLS LAST LIMIT 1 OFFSET 2) UNION DISTINCT (SELECT `T`.`a` AS `a`, `T`.`b` AS `b` FROM `default`.`T` AS `T` ORDER BY `T`.`a` ASC NULLS LAST, `T`.`b` ASC NULLS LAST LIMIT 3 OFFSET 4) ORDER BY `a` ASC NULLS LAST LIMIT 5 OFFSET 6;

--#[order-by-10]
(SELECT `T`.`flds` AS `flds` FROM `default`.`EXCLUDE_T` AS `T`) UNION DISTINCT (SELECT `T`.`flds` AS `flds` FROM `default`.`EXCLUDE_T` AS `T`) ORDER BY `flds`.`c`.`field_x` ASC NULLS LAST;

--#[order-by-11]
SELECT `T`.`a` AS `a`, `max`(`T`.`b`) AS `_1` FROM `default`.`T` AS `T` GROUP BY `T`.`a` ORDER BY `max`(`T`.`b`) ASC NULLS LAST;

--#[order-by-12]
SELECT `T`.`a` AS `a`, `max`(`T`.`b`) AS `_1` FROM `default`.`T` AS `T` GROUP BY `T`.`a` ORDER BY `min`(`T`.`b`) ASC NULLS LAST;

--#[order-by-13]
SELECT `T`.`a` AS `a`, `max`(`T`.`b`) AS `c` FROM `default`.`T` AS `T` GROUP BY `T`.`a` ORDER BY `max`(`T`.`b`) ASC NULLS LAST;

--#[order-by-14]
SELECT `T`.`a` AS `a`, `T`.`b` AS `c` FROM `default`.`T` AS `T` ORDER BY `T`.`b` ASC NULLS LAST;
