--#[identifier-multibyte]
SELECT "t"['b'] AS "café" FROM "default"."T" AS "t";

--#[identifier-quoted-folds-ascii-only]
SELECT "t"['b'] AS "cafÉ" FROM "default"."T" AS "t";

--#[identifier-dollar]
SELECT "t"['b'] AS "a$b" FROM "default"."T" AS "t";

--#[identifier-double-quote]
SELECT "t"['b'] AS "x""y" FROM "default"."T" AS "t";

--#[identifier-double-quote-alias]
SELECT "q""r"['b'] AS "b" FROM "default"."T" AS "q""r";

--#[reserved-word-alias]
[INVALID_INPUT{message="`user` is a reserved word; use a delimited identifier"}];

--#[niladic-user]
[UNSUPPORTED{message="`USER`"}];

--#[niladic-current-user]
[UNSUPPORTED{message="`CURRENT_USER`"}];

--#[reserved-word-quoted]
SELECT "t"['b'] AS "user" FROM "default"."T" AS "t";

--#[non-reserved-word]
SELECT "t"['b'] AS "qualify", "t"['c'] AS "start" FROM "default"."T" AS "t";

--#[comment-nested]
[UNSUPPORTED{message="nested block comment"}];

--#[comment-block]
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t";
