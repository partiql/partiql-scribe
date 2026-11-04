-- Inputs the grammar accepts and the translation refuses: the functions that later changes translate, the forms with
-- no PartiQL counterpart yet, and operations PartiQL does not define on the operand types.

--#[function-nvl]
SELECT NVL(c, 'none') FROM SIMPLE_T;

--#[function-coalesce]
SELECT COALESCE(c, 'none') FROM SIMPLE_T;

--#[function-decode]
SELECT DECODE(b, 1, 'one', 'other') FROM SIMPLE_T;

--#[function-greatest]
SELECT GREATEST(b, 1), LEAST(b, 1) FROM SIMPLE_T;

--#[function-nvl2]
SELECT NVL2(c, 'set', 'unset') FROM SIMPLE_T;

--#[function-nullif]
SELECT NULLIF(b, 0) FROM SIMPLE_T;

--#[function-power]
SELECT POWER(b, 2), POW(b, 2) FROM SIMPLE_T;

--#[function-concat]
SELECT CONCAT(c, 'x') FROM SIMPLE_T;

--#[function-lower-upper]
SELECT LOWER(c), UPPER(c) FROM SIMPLE_T;

--#[function-dateadd]
SELECT DATEADD(day, 1, timestamp_1) FROM T;

--#[function-datediff]
SELECT DATEDIFF(day, timestamp_1, timestamp_2) FROM T;

--#[function-getdate]
SELECT GETDATE() FROM SIMPLE_T;

--#[function-sysdate]
SELECT SYSDATE FROM SIMPLE_T;

--#[function-current-date]
SELECT CURRENT_DATE FROM SIMPLE_T;

--#[function-extract-epoch]
SELECT EXTRACT(EPOCH FROM timestamp_1) FROM T;

--#[function-in-where]
SELECT a FROM SIMPLE_T WHERE UPPER(c) = 'X';

--#[interval-literal]
SELECT INTERVAL '1 day' FROM SIMPLE_T;

--#[from-column-aliases]
SELECT s.x FROM (SELECT a FROM SIMPLE_T) AS s (x);

--#[type-mismatch-arith]
SELECT c + 1 FROM SIMPLE_T;

