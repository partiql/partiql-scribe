--#[date-add]
date_add('day', 1, TIMESTAMP '2017-01-02 03:04:05.006');

--#[utcnow]
at_timezone(current_timestamp, 'UTC');

--#[contains]
SELECT "contains"("p"."items", 'x') AS "c" FROM "test"."payload" AS "p";

--#[contains-embedded-quote]
SELECT "contains"("p"."e""),f", 'x') AS "c" FROM "test"."payload" AS "p";

--#[contains-embedded-backtick]
SELECT "contains"("p"."e`),f", 'x') AS "c" FROM "test"."payload" AS "p";

--#[contains-embedded-quote-select-value]
[ScribeException{code=UNSUPPORTED_PLAN_TO_AST_CONVERSION, message="SELECT VALUE is not supported by SQL targets unless it is a struct with string-literal keys (e.g. SELECT VALUE {'a': a}); use a SELECT list instead"}];

--#[hex-to-bigint]
from_base('00C10300', 16);

--#[hex-to-bigint-negative]
from_base('-FF', 16);

--#[hex-to-bigint-min]
from_base('-8000000000000000', 16);

--#[to-unixtime]
CAST(to_unixtime(with_timezone(date_trunc('second', TIMESTAMP '2017-01-02 03:04:05.006'), 'UTC')) AS BIGINT);

--#[to-unixtime-fraction]
CAST(to_unixtime(with_timezone(date_trunc('second', TIMESTAMP '2017-01-02 03:04:05.6'), 'UTC')) AS BIGINT);

--#[to-unixtime-pre-epoch]
CAST(to_unixtime(with_timezone(date_trunc('second', TIMESTAMP '1969-12-31 23:59:59.6'), 'UTC')) AS BIGINT);

--#[to-unixtime-max]
CAST(to_unixtime(with_timezone(date_trunc('second', TIMESTAMP '9999-12-31 23:59:59.999999'), 'UTC')) AS BIGINT);

--#[pow]
"pow"(2, 3);
