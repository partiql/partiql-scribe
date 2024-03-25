--#[builtins-nullif-00]
SELECT nullif(a, b) FROM T;

--#[builtins-nullif-01]
SELECT nullif(b, a) FROM T;

--#[builtins-nullif-02]
SELECT nullif((nullif(a, b), c), d) FROM T;

--#[builtins-nullif-03]
SELECT coalesce(nullif(coalesce(a, b, c), b), c) FROM T;
