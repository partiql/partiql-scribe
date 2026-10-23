-- A backslash escapes a character in a Redshift string literal: two backslashes are one backslash.
--#[string-backslash-00]
SELECT 'a\\b' AS v FROM T;

-- A backslash before a quote is a quote, like two quotes.
--#[string-backslash-01]
SELECT 'it\'s' AS v1, 'it''s' AS v2 FROM T;

-- \t is a TAB; PartiQL string literals have no escapes, so the TAB is written as it is.
--#[string-backslash-02]
SELECT 'a\tb' AS v FROM T;

--#[string-backslash-03]
SELECT 'a\nb' AS v FROM T;

-- The other backslash escapes are not documented.
--#[string-backslash-04]
SELECT 'a\_b' AS v FROM T;

--#[string-backslash-05]
SELECT 'a\x41' AS v FROM T;

-- A backslash before the closing quote escapes it, so the literal is not terminated.
--#[string-backslash-06]
SELECT 'a\' AS v FROM T;

-- The decoded value has one backslash, so its length is 3, as in Redshift.
--#[string-backslash-07]
SELECT CHAR_LENGTH('a\\b') AS v FROM T;

-- The type of a literal with an exponent is not documented; it is one token, not a number and an alias.
--#[exponent-00]
SELECT 1e3 FROM T;

--#[exponent-01]
SELECT 2.0e19 AS v FROM T;

--#[exponent-02]
SELECT a FROM T WHERE b > -2E-19;

--#[exponent-03]
SELECT .5e2 AS v FROM T;

-- A sign on a numeric literal is part of the literal.
--#[sign-00]
SELECT - -1 AS v1, -(-1.5) AS v2, -(-(-1)) AS v3, +5 AS v4 FROM T;

--#[sign-01]
SELECT -NULL AS v FROM T;

--#[sign-02]
SELECT -'5' AS v FROM T;

--#[literal-decimal-00]
SELECT .5 AS v1, 5. AS v2, 1.50 AS v3, -37. AS v4 FROM T;

--#[literal-integer-00]
SELECT 2147483648 AS v1, 9223372036854775808 AS v2, -2147483648 AS v3 FROM T;

--#[boolean-null-00]
SELECT TRUE AS v1, FALSE AS v2, NULL AS v3 FROM T;

-- The session user is not part of a PartiQL session.
--#[session-user-00]
SELECT user AS u FROM T;

--#[session-user-01]
SELECT current_user AS u FROM T;

--#[session-user-02]
SELECT session_user AS u FROM T;

--#[session-user-03]
SELECT current_user_id AS u FROM T;
