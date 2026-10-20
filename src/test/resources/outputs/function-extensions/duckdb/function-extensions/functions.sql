--#[date-add]
date_add(TIMESTAMP '2017-01-02 03:04:05.006', to_days(1));

--#[utcnow]
timezone('UTC', current_timestamp);

--#[contains]
SELECT contains("p"."items", 'x') AS "c" FROM "test"."payload" AS "p";

--#[hex-to-bigint]
CAST('0x' || '00C10300' AS BIGINT);

--#[to-unixtime]
CAST(epoch(date_trunc('second', TIMESTAMP '2017-01-02 03:04:05.006')) AS BIGINT);

--#[to-unixtime-fraction]
CAST(epoch(date_trunc('second', TIMESTAMP '2017-01-02 03:04:05.6')) AS BIGINT);

--#[to-unixtime-pre-epoch]
CAST(epoch(date_trunc('second', TIMESTAMP '1969-12-31 23:59:59.6')) AS BIGINT);

--#[to-unixtime-max]
CAST(epoch(date_trunc('second', TIMESTAMP '9999-12-31 23:59:59.999999')) AS BIGINT);

--#[pow]
"pow"(2, 3);
