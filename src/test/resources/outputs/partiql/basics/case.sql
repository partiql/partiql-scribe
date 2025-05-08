--#[case-00]
CASE WHEN false THEN 0 WHEN true THEN 1 ELSE 2 END;
-- PLK-1.x does not yet prune the unsatisfiable branches
-- 1;

--#[case-01]
CASE
    WHEN 1 = 2 THEN 0
    WHEN 2 = 3 THEN 1
    ELSE 3
END;

--#[case-02]
CASE
    WHEN 1 = 1 THEN 'MATCH!'
    ELSE 'NO MATCH!'
END;

--#[case-03]
CASE
    WHEN 'Hello World' = 'Hello World' THEN true
    ELSE false
END;

--#[case-04]
SELECT
    CASE
        WHEN "T"['a'] = true THEN 'a IS TRUE'
        ELSE 'a MUST BE FALSE'
        END AS "result"
FROM "default"."T" AS "T";

--#[case-05]
SELECT
    CASE
        WHEN "T"['a'] = true THEN 'a IS TRUE'
        ELSE 'a MUST BE FALSE'
        END AS "result"
FROM "default"."T" AS "T";

--#[case-06]
SELECT
    CASE
        WHEN "T"['b'] = 10 THEN 'b IS 10'
        ELSE 'b IS NOT 10'
        END AS "result"
FROM "default"."T" AS "T";

--#[case-07]
SELECT
    CASE WHEN "T"['d']['e'] = 'WATER' THEN 'd.e IS WATER'
        ELSE 'd.e IS NOT WATER'
        END AS "result"
FROM "default"."T" AS "T";

--#[case-08]
SELECT
    CASE
        WHEN "T"['x'] = 'WATER' THEN 'x IS WATER'
        WHEN "T"['x'] = 5 THEN 'x IS 5'
        ELSE 'x IS SOMETHING ELSE'
        END AS "result"
FROM "default"."T" AS "T";

--#[case-09]
SELECT
    CASE
        WHEN "T"['x'] IS INT THEN 'x IS INT'
        WHEN "T"['x'] IS STRUCT THEN 'x IS STRUCT'
        ELSE 'x IS SOMETHING ELSE'
        END AS "result"
FROM "default"."T" AS "T";

--#[case-10]
CASE WHEN false THEN 0 WHEN false THEN 1 ELSE 2 END;
-- PLK-1.x does not yet prune the unsatisfiable branches
-- 2;
