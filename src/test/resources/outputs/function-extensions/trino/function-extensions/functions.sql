--#[date-add]
date_add('day', 1, TIMESTAMP '2017-01-02 03:04:05.006');

--#[utcnow]
at_timezone(current_timestamp, 'UTC');

--#[contains]
SELECT "contains"("p"."items", 'x') AS "c" FROM "test"."payload" AS "p";

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

--#[date-trunc]
date_trunc('month', TIMESTAMP '2020-08-13 23:20:30.123456');

--#[date-trunc-date]
date_trunc('quarter', CAST(DATE '2020-08-13' AS TIMESTAMP));

--#[date-trunc-week]
date_trunc('week', TIMESTAMP '2021-01-03 23:59:00');

--#[date-trunc-zoned]
date_trunc('day', TIMESTAMP '2020-08-13 23:20:30-05:00');

--#[date-trunc-unsupported-part]
[ScribeException{code=UNSUPPORTED_OPERATION}];

--#[to-char]
format_datetime(date_trunc('millisecond', TIMESTAMP '2020-12-31 23:59:59.999999'), 'yyyy-MM-dd HH:mm:ss.SSS');

--#[to-char-date]
format_datetime(date_trunc('millisecond', CAST(DATE '2020-08-13' AS TIMESTAMP)), 'yyyy/MM/dd hh:mm:ss a');

--#[to-char-no-separators]
format_datetime(date_trunc('millisecond', TIMESTAMP '2020-08-13 23:20:30.123456'), 'yyyyMMddHHmmssSSS');

--#[to-char-zoned]
format_datetime(date_trunc('millisecond', TIMESTAMP '2020-08-13 23:20:30-05:00'), 'yyyy-MM-dd HH:mm');

--#[to-char-unsupported]
[ScribeException{code=UNSUPPORTED_OPERATION}];

--#[to-char-mixed-case]
[ScribeException{code=UNSUPPORTED_OPERATION}];
