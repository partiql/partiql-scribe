-- DATE_ADD is reserved syntax; delimit it to exercise the registered extension.
--#[date-add]
"date_add"('day', 1, TIMESTAMP '2017-01-02 03:04:05.006');

--#[utcnow]
utcnow();

--#[contains]
SELECT VALUE contains(p.items, 'x') FROM payload AS p;

--#[hex-to-bigint]
hex_to_bigint('00C10300');

--#[to-unixtime]
to_unixtime(TIMESTAMP '2017-01-02 03:04:05.006');

--#[pow]
pow(2, 3);
