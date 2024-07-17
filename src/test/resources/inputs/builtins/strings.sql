--#[builtins-strings-lower-00]
SELECT lower(c) FROM T;

--#[builtins-strings-upper-00]
SELECT upper(c) FROM T;

--#[builtins-strings-like-00]
SELECT c LIKE 'x' FROM T;

--#[builtins-strings-like-01]
SELECT c LIKE '%x' FROM T;

--#[builtins-strings-like-02]
SELECT c LIKE 'x%' FROM T;

--#[builtins-strings-like-03]
SELECT c LIKE '%x%' FROM T;

--#[builtins-strings-like-04]
SELECT c LIKE '%' FROM T;

--#[trim-00]
SELECT TRIM(c) FROM T;

--#[trim-01]
SELECT TRIM(BOTH FROM c) FROM T;

--#[trim-02]
SELECT TRIM(LEADING FROM c) FROM T;

--#[trim-03]
SELECT TRIM(TRAILING FROM c) FROM T;

--#[trim-04]
SELECT TRIM(BOTH 'xxx' FROM c) FROM T;

--#[trim-05]
SELECT TRIM(LEADING 'xxx' FROM c) FROM T;

--#[trim-06]
SELECT TRIM(TRAILING 'xxx' FROM c) FROM T;
