-- INTERVAL <interval string> <single datetime field>
--#[interval-00]
SELECT INTERVAL '10' YEAR AS `i` FROM `default`.`T` AS `T`;

--#[interval-01]
SELECT INTERVAL '-10' YEAR AS `i` FROM `default`.`T` AS `T`;

--#[interval-02]
SELECT INTERVAL '10' YEAR AS `i` FROM `default`.`T` AS `T`;

--#[interval-03]
SELECT INTERVAL '-10' YEAR AS `i` FROM `default`.`T` AS `T`;

--#[interval-04]
SELECT INTERVAL '10' MONTH AS `i` FROM `default`.`T` AS `T`;

--#[interval-05]
SELECT INTERVAL '-10' MONTH AS `i` FROM `default`.`T` AS `T`;

--#[interval-06]
SELECT INTERVAL '10' MONTH AS `i` FROM `default`.`T` AS `T`;

--#[interval-07]
SELECT INTERVAL '-10' MONTH AS `i` FROM `default`.`T` AS `T`;

--#[interval-08]
SELECT INTERVAL '10' DAY AS `i` FROM `default`.`T` AS `T`;

--#[interval-09]
SELECT INTERVAL '-10' DAY AS `i` FROM `default`.`T` AS `T`;

--#[interval-10]
SELECT INTERVAL '10' DAY AS `i` FROM `default`.`T` AS `T`;

--#[interval-11]
SELECT INTERVAL '-10' DAY AS `i` FROM `default`.`T` AS `T`;

--#[interval-12]
SELECT INTERVAL '10' HOUR AS `i` FROM `default`.`T` AS `T`;

--#[interval-13]
SELECT INTERVAL '-10' HOUR AS `i` FROM `default`.`T` AS `T`;

--#[interval-14]
SELECT INTERVAL '10' HOUR AS `i` FROM `default`.`T` AS `T`;

--#[interval-15]
SELECT INTERVAL '-10' HOUR AS `i` FROM `default`.`T` AS `T`;

--#[interval-16]
SELECT INTERVAL '10' MINUTE AS `i` FROM `default`.`T` AS `T`;

--#[interval-17]
SELECT INTERVAL '-10' MINUTE AS `i` FROM `default`.`T` AS `T`;

--#[interval-18]
SELECT INTERVAL '10' MINUTE AS `i` FROM `default`.`T` AS `T`;

--#[interval-19]
SELECT INTERVAL '-10' MINUTE AS `i` FROM `default`.`T` AS `T`;

--#[interval-20]
SELECT INTERVAL '10' SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-21]
SELECT INTERVAL '-10' SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-22]
SELECT INTERVAL '10' SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-23]
SELECT INTERVAL '-10' SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-24]
SELECT INTERVAL '10.234' SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-25]
SELECT INTERVAL '-10.234' SECOND AS `i` FROM `default`.`T` AS `T`;

-- <start field> TO <end field>
--#[interval-26]
SELECT INTERVAL '10-3' YEAR TO MONTH AS `i` FROM `default`.`T` AS `T`;

--#[interval-27]
SELECT INTERVAL '-10-3' YEAR TO MONTH AS `i` FROM `default`.`T` AS `T`;

--#[interval-28]
SELECT INTERVAL '10-3' YEAR TO MONTH AS `i` FROM `default`.`T` AS `T`;

--#[interval-29]
SELECT INTERVAL '-10-3' YEAR TO MONTH AS `i` FROM `default`.`T` AS `T`;

--#[interval-30]
SELECT INTERVAL '10 3' DAY TO HOUR AS `i` FROM `default`.`T` AS `T`;

--#[interval-31]
SELECT INTERVAL '-10 3' DAY TO HOUR AS `i` FROM `default`.`T` AS `T`;

--#[interval-32]
SELECT INTERVAL '10 3' DAY TO HOUR AS `i` FROM `default`.`T` AS `T`;

--#[interval-33]
SELECT INTERVAL '-10 3' DAY TO HOUR AS `i` FROM `default`.`T` AS `T`;

--#[interval-34]
SELECT INTERVAL '10 3:4' DAY TO MINUTE AS `i` FROM `default`.`T` AS `T`;

--#[interval-35]
SELECT INTERVAL '-10 3:4' DAY TO MINUTE AS `i` FROM `default`.`T` AS `T`;

--#[interval-36]
SELECT INTERVAL '10 3:4' DAY TO MINUTE AS `i` FROM `default`.`T` AS `T`;

--#[interval-37]
SELECT INTERVAL '-10 3:4' DAY TO MINUTE AS `i` FROM `default`.`T` AS `T`;

--#[interval-38]
SELECT INTERVAL '10 3:4:5' DAY TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-39]
SELECT INTERVAL '-10 3:4:5' DAY TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-40]
SELECT INTERVAL '10 3:4:5' DAY TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-41]
SELECT INTERVAL '-10 3:4:5' DAY TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-42]
SELECT INTERVAL '10 3:4:5.678' DAY TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-43]
SELECT INTERVAL '-10 3:4:5.678' DAY TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-44]
SELECT INTERVAL '10 3:4:5.678' DAY TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-45]
SELECT INTERVAL '-10 3:4:5.678' DAY TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-46]
SELECT INTERVAL '3:4' HOUR TO MINUTE AS `i` FROM `default`.`T` AS `T`;

--#[interval-47]
SELECT INTERVAL '-3:4' HOUR TO MINUTE AS `i` FROM `default`.`T` AS `T`;

--#[interval-48]
SELECT INTERVAL '3:4' HOUR TO MINUTE AS `i` FROM `default`.`T` AS `T`;

--#[interval-49]
SELECT INTERVAL '-3:4' HOUR TO MINUTE AS `i` FROM `default`.`T` AS `T`;

--#[interval-50]
SELECT INTERVAL '2:3:4' HOUR TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-51]
SELECT INTERVAL '-2:3:4' HOUR TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-52]
SELECT INTERVAL '2:3:4' HOUR TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-53]
SELECT INTERVAL '-2:3:4' HOUR TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-54]
SELECT INTERVAL '2:3:4.567' HOUR TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-55]
SELECT INTERVAL '-2:3:4.567' HOUR TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-56]
SELECT INTERVAL '2:3:4.567' HOUR TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-57]
SELECT INTERVAL '-2:3:4.567' HOUR TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-58]
SELECT INTERVAL '3:4' MINUTE TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-59]
SELECT INTERVAL '-3:4' MINUTE TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-60]
SELECT INTERVAL '3:4' MINUTE TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-61]
SELECT INTERVAL '-3:4' MINUTE TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-62]
SELECT INTERVAL '3:4.567' MINUTE TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-63]
SELECT INTERVAL '-3:4.567' MINUTE TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-64]
SELECT INTERVAL '3:4.567' MINUTE TO SECOND AS `i` FROM `default`.`T` AS `T`;

--#[interval-65]
SELECT INTERVAL '-3:4.567' MINUTE TO SECOND AS `i` FROM `default`.`T` AS `T`;
