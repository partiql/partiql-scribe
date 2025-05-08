--#[builtins-nullif-00]
SELECT nullif(a, b) FROM T;

--#[builtins-nullif-01]
SELECT nullif(b, a) FROM T;

--#[builtins-nullif-02]
SELECT nullif(nullif(nullif(a, b), c), d) FROM T;
-- Existing test was easy to misinterpret. It was a NULLIF of a row value expression with a NULLIF
-- SELECT nullif((nullif(a, b), c), d) FROM T;

--#[builtins-nullif-03]
SELECT nullif((nullif(a, b), c), d) FROM T;

--#[builtins-nullif-04]
SELECT coalesce(nullif(coalesce(a, b, c), b), c) FROM T;
