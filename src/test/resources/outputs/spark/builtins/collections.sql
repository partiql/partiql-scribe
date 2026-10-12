--#[collections-01]
SELECT `size`(`t`.`array`) AS `v` FROM `default`.`T` AS `t`;

--#[collections-02]
SELECT `size`(`t`.`array`) AS `v` FROM `default`.`T` AS `t`;

--#[collections-03]
SELECT `size`(`t`.`array`) > 0 AS `v` FROM `default`.`T` AS `t`;
