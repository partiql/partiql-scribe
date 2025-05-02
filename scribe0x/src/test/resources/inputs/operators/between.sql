--#[between-00]
-- between(decimal, int32, int32)
SELECT da FROM T_DECIMALS WHERE da BETWEEN -1 AND 1;

--#[between-01]
-- between(decimal, int64, int64)
SELECT da FROM T_DECIMALS WHERE da BETWEEN -2147483649 AND 2147483648;

--#[between-02]
-- between(decimal, decimal, decimal)
SELECT da FROM T_DECIMALS WHERE da BETWEEN -9223372036854775809 AND 9223372036854775808;

--#[between-04]
-- between(decimal(p,s), int32, int32)
SELECT de FROM T_DECIMALS WHERE de BETWEEN -1 AND 1;

--#[between-05]
-- between(decimal(p,s), int64, int64)
SELECT de FROM T_DECIMALS WHERE de BETWEEN -2147483649 AND 2147483648;

--#[between-06]
-- between(decimal(p,s), decimal, decimal)
SELECT de FROM T_DECIMALS WHERE de BETWEEN -9223372036854775809 AND 9223372036854775808;
