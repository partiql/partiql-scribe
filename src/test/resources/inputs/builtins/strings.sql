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

--#[substring-00]
SELECT SUBSTRING(c FROM 2) FROM T;

--#[substring-01]
SELECT SUBSTRING(c FROM 2 FOR 3) FROM T;

--#[substring-02]
SELECT SUBSTRING(c, 2) FROM T;

--#[substring-03]
SELECT SUBSTRING(c, 2, 3) FROM T;

--#[substring-10]
SELECT SUBSTRING(c FROM -2) FROM T;

--#[substring-11]
SELECT SUBSTRING(c FROM 0) FROM T;

--#[substring-12]
SELECT SUBSTRING(c FROM -2 FOR 6) FROM T;

--#[substring-13]
SELECT SUBSTRING('abc' FROM -9 FOR 2) FROM T;

--#[substring-14]
SELECT SUBSTRING(c FROM 0 FOR 2) FROM T;

--#[position-00]
SELECT POSITION('a' IN c) FROM T;

--#[char-length-00]
SELECT CHAR_LENGTH(c) FROM T;

--#[replace-00]
SELECT replace(c, 'a', 'b') FROM T;

--#[split-00]
SELECT split(c, ',') FROM T;

--#[split-01]
SELECT split(c, '.') FROM T;

--#[split-02]
SELECT split(c, '|') FROM T;

--#[split-03]
SELECT split(c, '\') FROM T;

--#[split-04]
SELECT split(c, '::') FROM T;

--#[split-05]
SELECT split(c, z) FROM T;

--#[split-06]
SELECT split(c, '[a-z]+') FROM T;

--#[split-07]
SELECT split(z, c) FROM T;

--#[split-08]
SELECT split(c, '') FROM T;
