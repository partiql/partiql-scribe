--#[builtins-coalesce-00]
SELECT coalesce(a) FROM T;

--#[builtins-coalesce-01]
SELECT coalesce(a, b) FROM T;

--#[builtins-coalesce-02]
SELECT coalesce(a, b, c) FROM T;
