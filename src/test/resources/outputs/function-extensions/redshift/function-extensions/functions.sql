--#[date-add]
DATEADD(DAY, 1, TIMESTAMP '2017-01-02 03:04:05.006');

--#[utcnow]
sysdate;

--#[contains]
SELECT VALUE 1 <= (SELECT COUNT(items) FROM "p"."items" AS items WHERE items IN ('x')) FROM "test"."payload" AS "p";

--#[hex-to-bigint]
STRTOL('00C10300', 16);

--#[to-unixtime]
CAST(DATE_PART(EPOCH, TIMESTAMP '2017-01-02 03:04:05.006') AS BIGINT);

--#[pow]
"pow"(2, 3);
