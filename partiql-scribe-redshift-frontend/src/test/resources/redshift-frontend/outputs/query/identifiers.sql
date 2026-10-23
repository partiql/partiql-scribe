-- --#[identifier-multibyte] TODO(redshift)
SELECT "t"['b'] AS "café" FROM "default"."T" AS "t";

-- --#[identifier-quoted-folds-ascii-only] TODO(redshift)
SELECT "t"['b'] AS "cafÉ" FROM "default"."T" AS "t";

-- --#[identifier-dollar] TODO(redshift)
SELECT "t"['b'] AS "a$b" FROM "default"."T" AS "t";

-- --#[identifier-double-quote] TODO(redshift)
SELECT "t"['b'] AS "x""y" FROM "default"."T" AS "t";

-- --#[identifier-double-quote-alias] TODO(redshift)
SELECT "q""r"['b'] AS "b" FROM "default"."T" AS "q""r";

-- --#[reserved-word-alias] TODO(redshift)
[INVALID_INPUT{message="`user` is a reserved word; use a delimited identifier"}];

-- --#[niladic-user] TODO(redshift)
[UNSUPPORTED{message="`USER`"}];

-- --#[niladic-current-user] TODO(redshift)
[UNSUPPORTED{message="`CURRENT_USER`"}];

-- --#[reserved-word-quoted] TODO(redshift)
SELECT "t"['b'] AS "user" FROM "default"."T" AS "t";

-- --#[non-reserved-word] TODO(redshift)
SELECT "t"['b'] AS "qualify", "t"['c'] AS "start" FROM "default"."T" AS "t";

-- --#[comment-nested] TODO(redshift)
[UNSUPPORTED{message="nested block comment"}];

-- --#[comment-block] TODO(redshift)
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t";
