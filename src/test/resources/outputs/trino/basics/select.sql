--#[select-00]
SELECT "T"."a" AS "a", "T"."b" AS "b", "T"."c" AS "c" FROM "default"."T" AS "T";

--#[select-01]
SELECT "T".* FROM "default"."T" AS "T";

--#[select-02]
SELECT "T"."a" AS "a", "T"."b" AS "b", "T"."c" AS "c" FROM "default"."T" AS "T";

-- #[select-03]
-- ERR! SELECT VALUE does not exist
-- SELECT VALUE a FROM T;

--#[select-04]
SELECT "t1".*, "t2".* FROM "default"."T" AS "t1" INNER JOIN "default"."T" AS "t2" ON true;

-- #[select-05]
-- ERR! Trino doesn't have path expressions
-- SELECT t.d.* FROM "default"."T";

-- #[select-06]
-- ERR! Trino doesn't have path expressions
-- SELECT t, t.d.* FROM "default"."T";

-- #[select-07]
-- ERR! Trino doesn't have path expressions
-- SELECT t.d.*, t.d.* FROM "default"."T";

-- #[select-08]
-- ERR! Trino doesn't have path expressions
-- SELECT "T"."d".* FROM "default"."T" AS "T";

--#[select-09]
SELECT "T".* FROM "default"."T" AS "T";

--#[select-10]
SELECT "T"."c" || current_user AS "_1" FROM "default"."T" AS "T";

--#[select-11]
SELECT current_user AS "CURRENT_USER" FROM "default"."T" AS "T";

--#[select-12]
SELECT "t"."a" AS "a" FROM "default"."T" AS "t";
