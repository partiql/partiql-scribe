-- https://trino.io/docs/current/functions/conditional.html

--#[case-04]
SELECT
    CASE "T"."a"
        WHEN TRUE THEN 'a IS TRUE'
        ELSE 'a MUST BE FALSE'
        END AS "result"
FROM "default"."T" AS "T"

--#[case-05]
SELECT
    CASE
        WHEN "T"."a" = TRUE THEN 'a IS TRUE'
        ELSE 'a MUST BE FALSE'
        END AS "result"
FROM "default"."T" AS "T"

--#[case-06]
SELECT
    CASE "T"."b"
        WHEN 10 THEN 'b IS 10'
        ELSE 'b IS NOT 10'
        END AS "result"
FROM "default"."T" AS "T"

--#[case-07]
SELECT
    CASE "T"."d"."e"
        WHEN 'WATER' THEN 'd.e IS WATER'
        ELSE 'd.e IS NOT WATER'
        END AS "result"
FROM "default"."T" AS "T"

--#[case-08]
SELECT
    CASE "T"."x"
        WHEN 'WATER' THEN 'x IS WATER'
        WHEN 5 THEN 'x IS 5'
        ELSE 'x IS SOMETHING ELSE'
        END AS "result"
FROM "default"."T" AS "T"

--#[case-09]
SELECT
    CASE
        WHEN "T"."x" IS INT THEN 'x IS INT'
        WHEN "T"."x" IS STRUCT THEN 'x IS STRUCT'
        ELSE 'x IS SOMETHING ELSE'
END AS "result"
FROM "default"."T" AS "T";
