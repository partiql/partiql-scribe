--#[string-backslash-00]
SELECT 'a\b' AS "v" FROM "default"."T" AS "t";

--#[string-backslash-01]
SELECT 'it''s' AS "v1", 'it''s' AS "v2" FROM "default"."T" AS "t";

--#[string-backslash-02]
SELECT 'a	b' AS "v" FROM "default"."T" AS "t";

--#[string-backslash-03]
SELECT 'a
b' AS "v" FROM "default"."T" AS "t";

--#[string-backslash-04]
[UNSUPPORTED{message="backslash escape `\_` in a string literal"}];

--#[string-backslash-05]
[UNSUPPORTED{message="backslash escape `\x` in a string literal"}];

--#[string-backslash-06]
[INVALID_INPUT{message="extraneous input '''"}];

-- --#[string-backslash-07] TODO(redshift)
SELECT CHAR_LENGTH('a\b') AS "v" FROM "default"."T" AS "t";

--#[exponent-00]
[UNSUPPORTED{message="numeric literal with an exponent"}];

--#[exponent-01]
[UNSUPPORTED{message="numeric literal with an exponent"}];

--#[exponent-02]
[UNSUPPORTED{message="numeric literal with an exponent"}];

--#[exponent-03]
[UNSUPPORTED{message="numeric literal with an exponent"}];

--#[sign-00]
SELECT 1 AS "v1", 1.5 AS "v2", -1 AS "v3", 5 AS "v4" FROM "default"."T" AS "t";

--#[sign-01]
SELECT -NULL AS "v" FROM "default"."T" AS "t";

--#[sign-02]
[UNSUPPORTED{message="sign on a character string literal"}];

--#[literal-decimal-00]
SELECT 0.5 AS "v1", 5. AS "v2", 1.50 AS "v3", -37. AS "v4" FROM "default"."T" AS "t";

--#[literal-integer-00]
SELECT 2147483648 AS "v1", 9223372036854775808 AS "v2", -2147483648 AS "v3" FROM "default"."T" AS "t";

--#[boolean-null-00]
SELECT true AS "v1", false AS "v2", NULL AS "v3" FROM "default"."T" AS "t";

--#[session-user-00]
[UNSUPPORTED{message="USER"}];

--#[session-user-01]
[UNSUPPORTED{message="CURRENT_USER"}];

--#[session-user-02]
[UNSUPPORTED{message="SESSION_USER"}];

--#[session-user-03]
[UNSUPPORTED{message="CURRENT_USER_ID"}];
