--#[date-add]
DATEADD(DAY, 1, TIMESTAMP '2017-01-02 03:04:05.006');

--#[utcnow]
sysdate;

--#[contains]
SELECT 1 <= (SELECT COUNT(___partiql_scribe_contains_element___) FROM "p"."items" AS ___partiql_scribe_contains_element___ WHERE ___partiql_scribe_contains_element___ IN ('x')) AS "c" FROM "test"."payload" AS "p";

--#[hex-to-bigint]
STRTOL('00C10300', 16);

--#[hex-to-bigint-negative]
STRTOL('-FF', 16);

--#[hex-to-bigint-min]
STRTOL('-8000000000000000', 16);

--#[to-unixtime]
CAST(DATE_PART(EPOCH, date_trunc('second', TIMESTAMP '2017-01-02 03:04:05.006')) AS BIGINT);

--#[to-unixtime-fraction]
CAST(DATE_PART(EPOCH, date_trunc('second', TIMESTAMP '2017-01-02 03:04:05.6')) AS BIGINT);

--#[to-unixtime-pre-epoch]
CAST(DATE_PART(EPOCH, date_trunc('second', TIMESTAMP '1969-12-31 23:59:59.6')) AS BIGINT);

--#[to-unixtime-max]
CAST(DATE_PART(EPOCH, date_trunc('second', TIMESTAMP '9999-12-31 23:59:59.999999')) AS BIGINT);

--#[pow]
"pow"(2, 3);
