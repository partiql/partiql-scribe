--#[like-05]
SELECT "t"."c" AS "c" FROM "default"."T" AS "t" WHERE "t"."c" LIKE 'y';

-- #[like-06]
-- SELECT "t"."c" AS "c" FROM "default"."T" AS "t" WHERE "t"."c" NOT LIKE 'y';

--#[like-07]
SELECT "t"."c" AS "c" FROM "default"."T" AS "t" WHERE "t"."c" LIKE '%y';

--#[like-08]
SELECT "t"."c" AS "c" FROM "default"."T" AS "t" WHERE "t"."c" LIKE 'y%';

--#[like-09]
SELECT "t"."c" AS "c" FROM "default"."T" AS "t" WHERE "t"."c" LIKE '%y%';
