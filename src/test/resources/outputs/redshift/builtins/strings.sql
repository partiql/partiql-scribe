--#[trim-00]
SELECT TRIM(BOTH "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[trim-01]
SELECT TRIM(BOTH "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[trim-02]
SELECT TRIM(LEADING "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[trim-03]
SELECT TRIM(TRAILING "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[trim-04]
SELECT TRIM(BOTH 'xxx' FROM "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[trim-05]
SELECT TRIM(LEADING 'xxx' FROM "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[trim-06]
SELECT TRIM(TRAILING 'xxx' FROM "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[substring-00]
SELECT SUBSTRING("T"."c" FROM 2) AS "_1" FROM "default"."T" AS "T";

--#[substring-01]
SELECT SUBSTRING("T"."c" FROM 2 FOR 3) AS "_1" FROM "default"."T" AS "T";

--#[substring-02]
SELECT SUBSTRING("T"."c" FROM 2) AS "_1" FROM "default"."T" AS "T";

--#[substring-03]
SELECT SUBSTRING("T"."c" FROM 2 FOR 3) AS "_1" FROM "default"."T" AS "T";

--#[substring-10]
SELECT SUBSTRING("T"."c" FROM -2) AS "_1" FROM "default"."T" AS "T";

--#[substring-11]
SELECT SUBSTRING("T"."c" FROM 0) AS "_1" FROM "default"."T" AS "T";

--#[substring-12]
SELECT SUBSTRING("T"."c" FROM -2 FOR 6) AS "_1" FROM "default"."T" AS "T";

--#[substring-13]
SELECT SUBSTRING('abc' FROM -9 FOR 2) AS "_1" FROM "default"."T" AS "T";

--#[substring-14]
SELECT SUBSTRING("T"."c" FROM 0 FOR 2) AS "_1" FROM "default"."T" AS "T";

--#[position-00]
SELECT POSITION('a' IN "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[char-length-00]
SELECT CHAR_LENGTH("T"."c") AS "_1" FROM "default"."T" AS "T";

--#[replace-00]
SELECT REPLACE("T"."c", 'a', 'b') AS "_1" FROM "default"."T" AS "T";

--#[split-00]
SELECT SPLIT_TO_ARRAY("T"."c", ',') AS "_1" FROM "default"."T" AS "T";

--#[split-01]
SELECT SPLIT_TO_ARRAY("T"."c", '.') AS "_1" FROM "default"."T" AS "T";

--#[split-02]
SELECT SPLIT_TO_ARRAY("T"."c", '|') AS "_1" FROM "default"."T" AS "T";

-- https://github.com/partiql/partiql-scribe/issues/158
--#[split-03]
SELECT SPLIT_TO_ARRAY("T"."c", '\') AS "_1" FROM "default"."T" AS "T";

--#[split-04]
SELECT SPLIT_TO_ARRAY("T"."c", '::') AS "_1" FROM "default"."T" AS "T";

--#[split-05]
SELECT SPLIT_TO_ARRAY("T"."c", "T"."z") AS "_1" FROM "default"."T" AS "T";

--#[split-06]
SELECT SPLIT_TO_ARRAY("T"."c", '[a-z]+') AS "_1" FROM "default"."T" AS "T";

--#[split-07]
SELECT SPLIT_TO_ARRAY("T"."z", "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[split-08]
SELECT SPLIT_TO_ARRAY("T"."c", '') AS "_1" FROM "default"."T" AS "T";
