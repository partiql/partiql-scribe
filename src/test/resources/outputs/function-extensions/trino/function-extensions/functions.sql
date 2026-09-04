--#[date-add]
date_add('day', 1, TIMESTAMP '2017-01-02 03:04:05.006');

--#[utcnow]
at_timezone(current_timestamp, 'UTC');

--#[contains]
SELECT VALUE "contains"("p"."items", 'x') FROM "test"."payload" AS "p";

--#[hex-to-bigint]
from_base('00C10300', 16);

--#[to-unixtime]
CAST(to_unixtime(TIMESTAMP '2017-01-02 03:04:05.006') AS BIGINT);

--#[pow]
"pow"(2, 3);
