--#[trim-00]
SELECT TRIM(BOTH FROM "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[trim-01]
SELECT TRIM(BOTH FROM "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[trim-02]
SELECT TRIM(LEADING FROM "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[trim-03]
SELECT TRIM(TRAILING FROM "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[trim-04]
SELECT TRIM(BOTH 'xxx' FROM "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[trim-05]
SELECT TRIM(LEADING 'xxx' FROM "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[trim-06]
SELECT TRIM(TRAILING 'xxx' FROM "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[substring-00]
SELECT substring("T"."c", 2) AS "_1" FROM "default"."T" AS "T";

--#[substring-01]
SELECT substring("T"."c", 2, 3) AS "_1" FROM "default"."T" AS "T";

--#[substring-02]
SELECT substring("T"."c", 2) AS "_1" FROM "default"."T" AS "T";

--#[substring-03]
SELECT substring("T"."c", 2, 3) AS "_1" FROM "default"."T" AS "T";

--#[position-00]
SELECT POSITION('a' IN "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[char-length-00]
SELECT length("T"."c") AS "_1" FROM "default"."T" AS "T";

--#[replace-00]
SELECT REPLACE("T"."c", 'a', 'b') AS "_1" FROM "default"."T" AS "T";

--#[split-00]
SELECT SPLIT("T"."c", ',') AS "_1" FROM "default"."T" AS "T";

--#[split-01]
SELECT SPLIT("T"."c", '.') AS "_1" FROM "default"."T" AS "T";

--#[split-02]
SELECT SPLIT("T"."c", '|') AS "_1" FROM "default"."T" AS "T";

--#[split-03]
SELECT SPLIT("T"."c", '\') AS "_1" FROM "default"."T" AS "T";

--#[split-04]
SELECT SPLIT("T"."c", '::') AS "_1" FROM "default"."T" AS "T";

--#[split-05]
SELECT SPLIT("T"."c", "T"."z") AS "_1" FROM "default"."T" AS "T";

--#[split-06]
SELECT SPLIT("T"."c", '[a-z]+') AS "_1" FROM "default"."T" AS "T";

--#[split-07]
SELECT SPLIT("T"."z", "T"."c") AS "_1" FROM "default"."T" AS "T";
