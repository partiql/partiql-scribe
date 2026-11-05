-- Inputs the grammar accepts and the translation refuses: the functions that later changes translate, the forms with
-- no PartiQL counterpart yet, and operations PartiQL does not define on the operand types.

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

