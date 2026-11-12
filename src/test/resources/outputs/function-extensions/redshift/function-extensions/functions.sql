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

--#[date-trunc]
date_trunc('month', TIMESTAMP '2020-08-13 23:20:30.123456');

--#[date-trunc-date]
date_trunc('quarter', CAST(DATE '2020-08-13' AS TIMESTAMP));

--#[date-trunc-week]
date_trunc('week', TIMESTAMP '2021-01-03 23:59:00');

--#[date-trunc-zoned]
[ScribeException{code=UNSUPPORTED_OPERATION}];

--#[date-trunc-unsupported-part]
[ScribeException{code=UNSUPPORTED_OPERATION}];

--#[to-char]
TO_CHAR(TIMESTAMP '2020-12-31 23:59:59.999999', 'YYYY-MM-DD HH24:MI:SS.MS');

--#[to-char-date]
TO_CHAR(CAST(DATE '2020-08-13' AS TIMESTAMP), 'YYYY/MM/DD HH12:MI:SS AM');

--#[to-char-no-separators]
TO_CHAR(TIMESTAMP '2020-08-13 23:20:30.123456', 'YYYYMMDDHH24MISSMS');

--#[to-char-zoned]
[ScribeException{code=UNSUPPORTED_OPERATION}];

--#[to-char-unsupported]
[ScribeException{code=UNSUPPORTED_OPERATION}];

--#[to-char-mixed-case]
[ScribeException{code=UNSUPPORTED_OPERATION}];
