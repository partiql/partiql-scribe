--#[like-00]
SELECT a FROM T WHERE c LIKE 'a%' AND z NOT LIKE '%ten%';

-- The default escape character of Redshift LIKE is a backslash; PartiQL needs an ESCAPE clause.
--#[like-01]
SELECT a FROM T WHERE c LIKE 'a\\_c%';

--#[like-02]
SELECT a FROM T WHERE c LIKE '100\\%';

-- The escape character before an ordinary character makes it literal.
--#[like-03]
SELECT a FROM T WHERE c LIKE 'a\\dc';

--#[like-04]
SELECT a FROM T WHERE c LIKE 'a\\\\b%';

--#[like-05]
SELECT a FROM T WHERE c LIKE 'a^_c' ESCAPE '^';

--#[like-06]
SELECT a FROM T WHERE c LIKE 'a\\_c' ESCAPE '\\';

--#[like-07]
SELECT a FROM T WHERE c LIKE 'abc\\';

--#[like-08]
SELECT a FROM T WHERE c LIKE 'a%' ESCAPE '';

-- A pattern that is not a literal keeps the backslash escape.
--#[like-09]
SELECT a FROM T WHERE c LIKE z;

--#[like-10]
SELECT a FROM T WHERE c NOT LIKE z ESCAPE '#';

-- Redshift converts a number or a DATE to a character string.
--#[like-11]
SELECT a FROM T WHERE b LIKE '1%';

--#[like-12]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_timestamp LIKE '2020%';

-- LIKE keeps the padding of a CHAR(n) value, in both languages.
--#[like-13]
SELECT col_int32 FROM T_ALL_TYPES WHERE col_char LIKE 'abc%';

--#[like-operator-00]
SELECT a FROM T WHERE c ~~ 'a%' AND z !~~ 'b%';

-- ILIKE is case-insensitive for ASCII letters only.
--#[ilike-00]
SELECT a FROM T WHERE c ILIKE 'A_c%';

--#[ilike-01]
SELECT a FROM T WHERE c NOT ILIKE '%TEN%';

--#[ilike-02]
SELECT a FROM T WHERE c ~~* 'Ag%' OR c !~~* 'b%';

--#[ilike-03]
SELECT a FROM T WHERE c ILIKE '%é%';

--#[ilike-04]
SELECT a FROM T WHERE c ILIKE z;

--#[ilike-05]
SELECT c FROM T GROUP BY c HAVING MAX(z) ILIKE 'a%';

--#[similar-to-00]
SELECT a FROM T WHERE c SIMILAR TO 'a%' AND z NOT SIMILAR TO '%x\\.y_';

--#[similar-to-01]
SELECT a FROM T WHERE c SIMILAR TO '(a|b)%';

--#[similar-to-02]
SELECT a FROM T WHERE c SIMILAR TO 'a.c';

--#[similar-to-03]
SELECT a FROM T WHERE c SIMILAR TO 'a\\dc';

-- A POSIX pattern without metacharacters matches the string anywhere.
--#[posix-00]
SELECT a FROM T WHERE c ~ 'abc' AND z !~ 'x';

--#[posix-01]
SELECT a FROM T WHERE c ~ 'a.c';

--#[posix-02]
SELECT a FROM T WHERE c ~* 'abc';

--#[posix-03]
SELECT a FROM T WHERE c ~ z;
