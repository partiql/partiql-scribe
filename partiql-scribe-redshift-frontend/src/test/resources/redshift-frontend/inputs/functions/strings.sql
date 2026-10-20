-- LEN, LENGTH, TEXTLEN, CHAR_LENGTH, and CHARACTER_LENGTH do not count the padding of a CHAR(n) value.
--#[str-len-00]
SELECT LEN(col_char) AS v1, LENGTH(col_string) AS v2, CHARACTER_LENGTH(col_char) AS v3 FROM T_ALL_TYPES;

--#[str-len-01]
SELECT TEXTLEN(c) AS v FROM T;

-- Redshift converts a number or a DATE argument of a string function to a character string.
--#[str-len-02]
SELECT LEN(b) AS v1, LEN(col_date) AS v2 FROM T, T_ALL_TYPES;

--#[str-len-03]
SELECT LEN(t.foo.keep) AS v FROM datatypes.T_DECIMAL_5_2 t;

-- PartiQL writes the text of other DECIMAL expressions with another scale, or with an exponent.
--#[str-len-04]
SELECT LEN(col_decimal * 2) AS v FROM T_ALL_TYPES;

-- PartiQL writes the text of TIMESTAMP and DOUBLE PRECISION values differently.
--#[str-len-05]
SELECT LEN(timestamp_1) AS v FROM T;

--#[str-len-06]
SELECT LEN(col_float64) AS v FROM T_ALL_TYPES;

-- A CHAR(n) value unified with a VARCHAR value loses its padding.
--#[str-len-07]
SELECT LEN(NVL(col_char, col_string)) AS v FROM T_ALL_TYPES;

-- OCTET_LENGTH counts the padding of a CHAR(n) value in both languages.
--#[str-octet-length-00]
SELECT OCTET_LENGTH(col_char) AS v FROM T_ALL_TYPES;

--#[str-octet-length-01]
SELECT OCTET_LENGTH(NVL(col_char, 'x')) AS v FROM T_ALL_TYPES;

--#[str-upper-00]
SELECT UPPER(col_char) AS v1, LOWER(b) AS v2 FROM T, T_ALL_TYPES;

--#[str-substring-00]
SELECT SUBSTRING(c, 2, 3) AS v1, SUBSTRING(c FROM 2) AS v2, SUBSTRING(c FROM 2 FOR 3) AS v3, SUBSTRING(c FOR 3) AS v4 FROM T;

--#[str-substring-01]
SELECT SUBSTRING(c, -2, 6) AS v FROM T;

--#[str-substring-02]
SELECT SUBSTRING(col_char, 1, 5) AS v FROM T_ALL_TYPES;

--#[str-substring-03]
SELECT SUBSTRING(b, 1, 2) AS v FROM T;

--#[str-substring-04]
SELECT SUBSTRING(timestamp_1, 6, 2) AS v FROM T;

-- Redshift rounds a DECIMAL position half away from zero, and PartiQL half to even.
--#[str-substring-05]
SELECT SUBSTRING(c, 1.5) AS v FROM T;

--#[str-trim-00]
SELECT TRIM(c) AS v1, TRIM(BOTH c) AS v2, TRIM(LEADING 'x' FROM c) AS v3, TRIM('CDG' FROM c) AS v4 FROM T;

-- The LEADING forms, and the forms with characters, do not remove the padding of a CHAR(n) value.
--#[str-trim-01]
SELECT TRIM(TRAILING FROM col_char) AS v1, TRIM(LEADING FROM col_char) AS v2, TRIM(BOTH 'x' FROM col_char) AS v3 FROM T_ALL_TYPES;

--#[str-btrim-00]
SELECT BTRIM(c) AS v1, BTRIM(c, 'xyz') AS v2 FROM T;

--#[str-ltrim-00]
SELECT LTRIM(c) AS v1, LTRIM(c, '2008-') AS v2 FROM T;

--#[str-rtrim-00]
SELECT RTRIM(c, 'Park') AS v1, RTRIM(col_char, 'x') AS v2 FROM T, T_ALL_TYPES;

--#[str-position-00]
SELECT POSITION('a' IN c) AS v FROM T;

-- Whether Redshift counts the padding of a CHAR(n) value in POSITION is not documented.
--#[str-position-01]
SELECT POSITION('a' IN col_char) AS v FROM T_ALL_TYPES;

--#[str-strpos-00]
SELECT STRPOS(c, 'a') AS v FROM T;

--#[str-strpos-01]
SELECT STRPOS(col_char, 'a') AS v FROM T_ALL_TYPES;

--#[str-charindex-00]
SELECT CHARINDEX('a', c) AS v FROM T;

--#[str-replace-00]
SELECT REPLACE(c, 'a', 'b') AS v FROM T;

--#[str-split-part-00]
SELECT SPLIT_PART(c, '$', 2) AS v FROM T;

--#[str-split-part-01]
SELECT SPLIT_PART(c, z, 1) AS v FROM T;

--#[str-split-part-02]
SELECT SPLIT_PART(c, ',', b) AS v FROM T;

--#[str-split-part-03]
SELECT SPLIT_PART(c, ',', 0) AS v FROM T;

--#[str-left-right-00]
SELECT LEFT(c, 5) AS v1, RIGHT(c, 5) AS v2, RIGHT(c, 1) AS v3 FROM T;

--#[str-left-right-01]
SELECT LEFT(c, b) AS v FROM T;

--#[str-left-right-02]
SELECT RIGHT(c, 0) AS v FROM T;

--#[str-concat-00]
SELECT CONCAT(c, z) AS v1, CONCAT(c, b) AS v2 FROM T;

--#[str-concat-01]
SELECT CONCAT(col_char, col_string) AS v FROM T_ALL_TYPES;

--#[str-concat-02]
SELECT CONCAT(col_char, col_char) AS v FROM T_ALL_TYPES;

--#[str-pad-00]
SELECT LPAD(c, 5, '0') AS v FROM T;

--#[str-pad-01]
SELECT RPAD(c, 20, '0123456789') AS v FROM T;

--#[str-pad-02]
SELECT LPAD(c, 3) AS v1, RPAD(c, 0, 'x') AS v2 FROM T;

--#[str-pad-03]
SELECT LPAD(b, 5, '0') AS v FROM T;

--#[str-pad-04]
SELECT LPAD(c, b, '0') AS v FROM T;

--#[str-pad-05]
SELECT RPAD(c, 5, '') AS v FROM T;

--#[str-repeat-00]
SELECT REPEAT(c, 3) AS v1, REPLICATE(b, 2) AS v2 FROM T;

--#[str-repeat-01]
SELECT REPEAT(c, 0) AS v FROM T;

--#[str-translate-00]
SELECT TRANSLATE(c, ' .', '_') AS v FROM T;

-- The replacements are ordered so that no replacement changes a character that an earlier one inserted.
--#[str-translate-01]
SELECT TRANSLATE(c, 'inea', 'osin') AS v FROM T;

-- A character that occurs again keeps its first mapping.
--#[str-translate-02]
SELECT TRANSLATE(c, 'aab', 'xyz') AS v FROM T;

--#[str-translate-03]
SELECT TRANSLATE(c, 'ab', 'ba') AS v FROM T;

--#[str-octetindex-00]
SELECT OCTETINDEX('b', c) AS v FROM T;

--#[str-chr-00]
SELECT CHR(65) AS v1, c || CHR(39) AS v2, CHR(0) AS v3, CHR(200) AS v4 FROM T;

-- CHR returns CHAR, so the blank is an insignificant trailing blank in Redshift.
--#[str-chr-01]
SELECT CHR(32) AS v FROM T;

--#[str-chr-02]
SELECT CHR(b) AS v FROM T;

--#[str-quote-literal-00]
SELECT QUOTE_LITERAL(c) AS v1, QUOTE_LITERAL(b) AS v2 FROM T;

--#[str-quote-literal-01]
SELECT QUOTE_LITERAL(col_char) AS v FROM T_ALL_TYPES;

-- A pattern without regular expression operators matches only itself.
--#[str-regexp-00]
SELECT REGEXP_COUNT(c, 'ab') AS v1, REGEXP_COUNT(c, 'a') AS v2, REGEXP_INSTR(c, 'ab') AS v3 FROM T;

--#[str-regexp-01]
SELECT REGEXP_REPLACE(c, ',') AS v1, REGEXP_REPLACE(c, 'x', 'y') AS v2 FROM T;

--#[str-regexp-02]
SELECT REGEXP_SUBSTR(c, 'ab') AS v FROM T;

--#[str-regexp-03]
SELECT REGEXP_REPLACE(c, '[0-9]+', '#') AS v FROM T;

--#[str-regexp-04]
SELECT REGEXP_COUNT(c, 'a', 2) AS v FROM T;

--#[str-unsupported-00]
SELECT INITCAP(c) AS v FROM T;

-- SUBSTR is a leader node-only function.
--#[str-unsupported-01]
SELECT SUBSTR(c, 1, 2) AS v FROM T;

--#[str-unsupported-02]
SELECT REVERSE(c) AS v FROM T;

--#[str-unsupported-03]
SELECT ASCII(c) AS v FROM T;

--#[str-unsupported-04]
SELECT STRTOL(c, 16) AS v FROM T;
