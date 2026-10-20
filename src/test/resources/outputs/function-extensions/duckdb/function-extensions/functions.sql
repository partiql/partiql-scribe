--#[date-add]
date_add(TIMESTAMP '2017-01-02 03:04:05.006', to_days(1));

--#[utcnow]
timezone('UTC', current_timestamp);

--#[contains]
SELECT contains("p"."items", 'x') AS "c" FROM "test"."payload" AS "p";

--#[hex-to-bigint]
CAST(CASE WHEN left('00C10300', 1) = '-' THEN -CAST(CAST('0x' || substring('00C10300', 2) AS UBIGINT) AS HUGEINT) WHEN left('00C10300', 1) = '+' THEN CAST('0x' || substring('00C10300', 2) AS UBIGINT) ELSE CAST('0x' || '00C10300' AS UBIGINT) END AS BIGINT);

--#[hex-to-bigint-negative]
CAST(CASE WHEN left('-FF', 1) = '-' THEN -CAST(CAST('0x' || substring('-FF', 2) AS UBIGINT) AS HUGEINT) WHEN left('-FF', 1) = '+' THEN CAST('0x' || substring('-FF', 2) AS UBIGINT) ELSE CAST('0x' || '-FF' AS UBIGINT) END AS BIGINT);

--#[hex-to-bigint-min]
CAST(CASE WHEN left('-8000000000000000', 1) = '-' THEN -CAST(CAST('0x' || substring('-8000000000000000', 2) AS UBIGINT) AS HUGEINT) WHEN left('-8000000000000000', 1) = '+' THEN CAST('0x' || substring('-8000000000000000', 2) AS UBIGINT) ELSE CAST('0x' || '-8000000000000000' AS UBIGINT) END AS BIGINT);

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
