--#[date-add]
date_add(TIMESTAMP '2017-01-02 03:04:05.006', to_days(1));

--#[utcnow]
timezone('UTC', current_timestamp);

--#[contains]
SELECT contains("p"."items", 'x') AS "c" FROM "test"."payload" AS "p";

--#[hex-to-bigint]
CAST('0x' || '00C10300' AS BIGINT);

--#[to-unixtime]
CAST(epoch(TIMESTAMP '2017-01-02 03:04:05.006') AS BIGINT);

--#[pow]
"pow"(2, 3);
