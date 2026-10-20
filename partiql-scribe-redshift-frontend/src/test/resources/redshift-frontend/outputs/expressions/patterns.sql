--#[like-00]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['c'] LIKE 'a%' AND "t"['z'] NOT LIKE '%ten%';

--#[like-01]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['c'] LIKE 'a\_c%' ESCAPE '\';

--#[like-02]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['c'] LIKE '100\%' ESCAPE '\';

--#[like-03]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['c'] LIKE 'adc';

--#[like-04]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['c'] LIKE 'a\\b%' ESCAPE '\';

--#[like-05]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['c'] LIKE 'a\_c' ESCAPE '\';

--#[like-06]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['c'] LIKE 'a\_c' ESCAPE '\';

--#[like-07]
[UNSUPPORTED{message="LIKE pattern that ends with the escape character"}];

--#[like-08]
[UNSUPPORTED{message="ESCAPE ''; expected one character"}];

--#[like-09]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['c'] LIKE "t"['z'] ESCAPE '\';

--#[like-10]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['c'] NOT LIKE "t"['z'] ESCAPE '#';

--#[like-11]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE CAST("t"['b'] AS STRING) LIKE '1%';

--#[like-12]
[UNSUPPORTED{message="implicit conversion of TIMESTAMP to VARCHAR in `LIKE`"}];

--#[like-13]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE "t_all_types"['col_char'] LIKE 'abc%';

--#[like-operator-00]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['c'] LIKE 'a%' AND "t"['z'] NOT LIKE 'b%';

--#[ilike-00]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "lower"(REPLACE(REPLACE("t"['c'], 'İ', '·'), 'K', '·')) LIKE 'a_c%';

--#[ilike-01]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "lower"(REPLACE(REPLACE("t"['c'], 'İ', '·'), 'K', '·')) NOT LIKE '%ten%';

--#[ilike-02]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "lower"(REPLACE(REPLACE("t"['c'], 'İ', '·'), 'K', '·')) LIKE 'ag%' OR "lower"(REPLACE(REPLACE("t"['c'], 'İ', '·'), 'K', '·')) NOT LIKE 'b%';

--#[ilike-03]
[UNSUPPORTED{message="`ILIKE` with a non-ASCII pattern"}];

--#[ilike-04]
[UNSUPPORTED{message="ILIKE with a pattern that is not a literal"}];

--#[similar-to-00]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE "t"['c'] LIKE 'a%' AND "t"['z'] NOT LIKE '%x.y_';

--#[similar-to-01]
[UNSUPPORTED{message="SIMILAR TO with regular expression metacharacters"}];

--#[similar-to-02]
[UNSUPPORTED{message="SIMILAR TO with regular expression metacharacters"}];

--#[similar-to-03]
[UNSUPPORTED{message="SIMILAR TO with regular expression metacharacters"}];

--#[posix-00]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE (POSITION('abc' IN "t"['c']) > CAST(0 AS BIGINT)) AND (POSITION('x' IN "t"['z']) = 0);

--#[posix-01]
[UNSUPPORTED{message="POSIX regular expression `~` with metacharacters"}];

--#[posix-02]
[UNSUPPORTED{message="POSIX regular expression `~*`"}];

--#[posix-03]
[UNSUPPORTED{message="POSIX regular expression with a pattern that is not a literal"}];
