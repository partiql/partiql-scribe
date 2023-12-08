--#[datetime-00]
CURRENT_DATE;

--#[datetime-01]
DATE_ADD(SECOND, 5, CURRENT_DATE);

--#[datetime-02]
DATE_ADD(MINUTE, 5, CURRENT_DATE);

--#[datetime-03]
DATE_ADD(HOUR, 5, CURRENT_DATE);

--#[datetime-04]
DATE_ADD(DAY, 5, CURRENT_DATE);

--#[datetime-05]
DATE_ADD(MONTH, 5, CURRENT_DATE);

--#[datetime-06]
DATE_ADD(YEAR, 5, CURRENT_DATE);

--#[datetime-07]
DATE_DIFF(DAY, CURRENT_DATE, CURRENT_DATE);

--#[datetime-08]
SELECT CURRENT_DATE FROM T;

--#[datetime-09]
SELECT DATE_ADD(SECOND, 5, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";

--#[datetime-10]
SELECT DATE_ADD(MINUTE, 5, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";

--#[datetime-11]
SELECT DATE_ADD(HOUR, 5, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";

--#[datetime-12]
SELECT DATE_ADD(DAY, 5, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";

--#[datetime-13]
SELECT DATE_ADD(MONTH, 5, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";

--#[datetime-14]
SELECT DATE_ADD(YEAR, 5, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";

--#[datetime-15]
SELECT DATE_DIFF(DAY, CURRENT_DATE, CURRENT_DATE) AS "_1" FROM "default"."T" AS "T";
