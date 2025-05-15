--#[select-00]
SELECT "T"."a", "T"."b", "T"."c" FROM "default"."T" AS "T";

--#[select-01]
-- expand out the `SELECT *`
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T";

-- #[select-02]
-- ERROR, no SELECT VALUE!
-- SELECT VALUE { 'a': a, 'b': b, 'c': c } FROM "default"."T" AS "T";

-- #[select-03]
-- ERROR, no SELECT VALUE!
-- SELECT VALUE a FROM "default"."T" AS "T";

--#[select-04]
SELECT "t1"."a", "t1"."b", "t1"."c", "t1"."d", "t1"."x", "t1"."array", "t1"."z", "t1"."v", "t1"."timestamp_1", "t1"."timestamp_2", "t2"."a", "t2"."b", "t2"."c", "t2"."d", "t2"."x", "t2"."array", "t2"."z", "t2"."v", "t2"."timestamp_1", "t2"."timestamp_2" FROM "default"."T" AS "t1" INNER JOIN "default"."T" AS "t2" ON true;

-- Redshift doesn't support struct wildcard (i.e. <SUPER OBJECT>.*). Rewriting to include every struct field.
--#[select-05]
SELECT "T"."d"."e" FROM "default"."T" AS "T";

--#[select-06]
SELECT "T" AS "t", "T"."d"."e" FROM "default"."T" AS "T";

--#[select-07]
SELECT "T"."d"."e", "T"."d"."e" FROM "default"."T" AS "T";

--#[select-08]
SELECT "T"."d"."e" FROM "default"."T" AS "T";

--#[select-09]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T";

--#[select-10]
SELECT "T"."c" || CURRENT_USER AS "_1" FROM "default"."T" AS "T";

--#[select-11]
SELECT CURRENT_USER AS "CURRENT_USER" FROM "default"."T" AS "T";

--#[select-12]
SELECT "t"."a" FROM "default"."T" AS "t";

-- --#[select-13]
-- SELECT VALUE {z: a} FROM T;

--#[select-14]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" BETWEEN 0 AND 2;

--#[select-15]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" NOT BETWEEN 0 AND 2;

--#[select-16]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" NOT BETWEEN 0 AND 2;

--#[select-17]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" BETWEEN 0 AND 2;

--#[select-18]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" IN (0, 1, 2);

--#[select-19]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" NOT IN (0, 1, 2);

--#[select-20]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" NOT IN (0, 1, 2);

--#[select-21]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" IN (0, 1, 2);

--#[select-22]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" IS NULL;

--#[select-23]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" IS NOT NULL;

--#[select-24]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" IS NOT NULL;

--#[select-25]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" IS NULL;

--#[select-26]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."c" LIKE 'abc';

--#[select-27]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."c" NOT LIKE 'abc';

--#[select-28]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."c" NOT LIKE 'abc';

--#[select-29]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."c" LIKE 'abc';

-- preserve aliases for A, "bB", and "C"; not for d or array index
--#[select-30]
SELECT "T"."a" AS "A", "T"."b" AS "bB", "T"."c" AS "C", "T"."d", "T"."array"[1] AS "_1" FROM "default"."T" AS "T"
