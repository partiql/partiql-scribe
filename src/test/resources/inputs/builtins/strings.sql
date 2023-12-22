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
