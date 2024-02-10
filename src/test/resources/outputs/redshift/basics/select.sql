--#[select-00]
SELECT "T"."a" AS "a", "T"."b" AS "b", "T"."c" AS "c" FROM "default"."T" AS "T";

--#[select-01]
SELECT "T".* FROM "default"."T" AS "T";

-- #[select-02]
-- ERROR, no SELECT VALUE!
-- SELECT VALUE { 'a': a, 'b': b, 'c': c } FROM "default"."T" AS "T";

-- #[select-03]
-- ERROR, no SELECT VALUE!
-- SELECT VALUE a FROM "default"."T" AS "T";

--#[select-04]
SELECT "t1".*, "t2".* FROM "default"."T" AS "t1" INNER JOIN "default"."T" AS "t2" ON true;

-- Redshift doesn't support struct wildcard (i.e. SUPER OBJECT). Rewriting to include every struct field.
--#[select-05]
SELECT "T"."d".e AS "e" FROM "default"."T" AS "T";

--#[select-06]
SELECT "T" AS "t", "T"."d".e AS "e" FROM "default"."T" AS "T";

--#[select-07]
SELECT "T"."d".e AS "e", "T"."d".e AS "e" FROM "default"."T" AS "T";

--#[select-08]
SELECT "T"."d".e AS "e" FROM "default"."T" AS "T";

--#[select-09]
SELECT "T".* FROM "default"."T" AS "T";

--#[select-10]
SELECT "T"."c" || CURRENT_USER AS "_1" FROM "default"."T" AS "T";

--#[select-10]
SELECT "T"."c" || CURRENT_USER AS "_1" FROM "default"."T" AS "T";

--#[select-11]
SELECT CURRENT_USER AS "CURRENT_USER" FROM "default"."T" AS "T";

--#[select-12]
SELECT "t"."a" AS "a" FROM "default"."T" AS "t";

-- --#[select-13]
-- SELECT VALUE {z: a} FROM T;

--#[select-14]
SELECT "T".* FROM "default"."T" AS "T" WHERE "T"."b" BETWEEN 0 AND 2;

--#[select-15]
SELECT "T".* FROM "default"."T" AS "T" WHERE "T"."b" NOT BETWEEN 0 AND 2;

--#[select-16]
SELECT "T".* FROM "default"."T" AS "T" WHERE "T"."b" NOT BETWEEN 0 AND 2;

--#[select-17]
SELECT "T".* FROM "default"."T" AS "T" WHERE "T"."b" BETWEEN 0 AND 2;

--#[select-18]
SELECT "T".* FROM "default"."T" AS "T" WHERE "T"."b" IN (0, 1, 2);

--#[select-19]
SELECT "T".* FROM "default"."T" AS "T" WHERE "T"."b" NOT IN (0, 1, 2);

--#[select-20]
SELECT "T".* FROM "default"."T" AS "T" WHERE "T"."b" NOT IN (0, 1, 2);

--#[select-21]
SELECT "T".* FROM "default"."T" AS "T" WHERE "T"."b" IN (0, 1, 2);

--#[select-22]
SELECT "T".* FROM "default"."T" AS "T" WHERE "T"."b" IS NULL;

--#[select-23]
SELECT "T".* FROM "default"."T" AS "T" WHERE "T"."b" IS NOT NULL;

--#[select-24]
SELECT "T".* FROM "default"."T" AS "T" WHERE "T"."b" IS NOT NULL;

--#[select-25]
SELECT "T".* FROM "default"."T" AS "T" WHERE "T"."b" IS NULL;

--#[select-26]
SELECT "T".* FROM "default"."T" AS "T" WHERE "T"."c" LIKE 'abc';

--#[select-27]
SELECT "T".* FROM "default"."T" AS "T" WHERE "T"."c" NOT LIKE 'abc';

--#[select-28]
SELECT "T".* FROM "default"."T" AS "T" WHERE "T"."c" NOT LIKE 'abc';

--#[select-29]
SELECT "T".* FROM "default"."T" AS "T" WHERE "T"."c" LIKE 'abc';
