--#[datetime-08]
SELECT current_date AS "CURRENT_DATE" FROM "default"."T" AS "T";

--#[datetime-09]
SELECT date_add('second', 5, current_date) AS "_1" FROM "default"."T" AS "T";

--#[datetime-10]
SELECT date_add('minute', 5, current_date) AS "_1" FROM "default"."T" AS "T";

--#[datetime-11]
SELECT date_add('hour', 5, current_date) AS "_1" FROM "default"."T" AS "T";

--#[datetime-12]
SELECT date_add('day', 5, current_date) AS "_1" FROM "default"."T" AS "T";

--#[datetime-13]
SELECT date_add('month', 5, current_date) AS "_1" FROM "default"."T" AS "T";

--#[datetime-14]
SELECT date_add('year', 5, current_date) AS "_1" FROM "default"."T" AS "T";

--#[datetime-15]
SELECT date_diff('day', current_date, current_date) AS "_1" FROM "default"."T" AS "T";
