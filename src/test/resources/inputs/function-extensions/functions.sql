-- DATE_ADD is reserved syntax; delimit it to exercise the registered extension.
--#[date-add]
"date_add"('day', 1, TIMESTAMP '2017-01-02 03:04:05.006');

--#[utcnow]
utcnow();

--#[contains]
SELECT contains(p.items, 'x') AS c FROM payload AS p;

--#[hex-to-bigint]
hex_to_bigint('00C10300');

-- A leading sign is accepted, like Long.parseLong (PartiQL: -255)
--#[hex-to-bigint-negative]
hex_to_bigint('-FF');

-- The most negative BIGINT (PartiQL: -9223372036854775808)
--#[hex-to-bigint-min]
hex_to_bigint('-8000000000000000');

--#[to-unixtime]
to_unixtime(TIMESTAMP '2017-01-02 03:04:05.006');

-- A fraction >= .5 must truncate, not round (PartiQL: 1483326245)
--#[to-unixtime-fraction]
to_unixtime(TIMESTAMP '2017-01-02 03:04:05.6');

-- Before 1970, truncation is toward negative infinity (PartiQL: -1)
--#[to-unixtime-pre-epoch]
to_unixtime(TIMESTAMP '1969-12-31 23:59:59.6');

-- Far from 1970 a DOUBLE epoch can't hold microseconds; must not round up (PartiQL: 253402300799)
--#[to-unixtime-max]
to_unixtime(TIMESTAMP '9999-12-31 23:59:59.999999');

--#[pow]
pow(2, 3);
