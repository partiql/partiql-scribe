--#[quoted-identifiers-00]
SELECT "t"."a""b" FROM "default"."T_QUOTED_FIELDS" AS "t";

--#[quoted-identifiers-01]
SELECT "t"."c`d" FROM "default"."T_QUOTED_FIELDS" AS "t";

--#[quoted-identifiers-02]
SELECT "t"."a""b" AS "p""q`r" FROM "default"."T_QUOTED_FIELDS" AS "t";

--#[quoted-identifiers-03]
SELECT "t""1`2"."a""b" FROM "default"."T_QUOTED_FIELDS" AS "t""1`2";

--#[quoted-identifiers-04]
SELECT "t"."e""),f" FROM "default"."T_QUOTED_FIELDS" AS "t";

--#[quoted-identifiers-05]
SELECT "t"."s""t"."u""v", "t"."s""t"."w`x" FROM "default"."T_QUOTED_FIELDS" AS "t";

--#[quoted-identifiers-06]
SELECT "t"."a""b" AS "k""1`2" FROM "default"."T_QUOTED_FIELDS" AS "t" GROUP BY "t"."a""b";

--#[quoted-identifiers-07]
WITH "w""1`2" AS (SELECT "t"."a""b" FROM "default"."T_QUOTED_FIELDS" AS "t") SELECT "w""1`2"."a""b" FROM "w""1`2" AS "w""1`2";

--#[quoted-identifiers-08]
SELECT "t"."a""b", "t"."c`d", "t"."e""),f", OBJECT_TRANSFORM("t"."s""t" KEEP '"w`x"') AS "s""t" FROM "default"."T_QUOTED_FIELDS" AS "t";

--#[quoted-identifiers-09]
SELECT "t"."a""b", "t"."c`d", "t"."e""),f", OBJECT_TRANSFORM("t"."s""t" KEEP '"u\\"v"') AS "s""t" FROM "default"."T_QUOTED_FIELDS" AS "t";

--#[quoted-identifiers-10]
SELECT "E"."a", "x""1`" FROM "default"."T" AS "E" LEFT JOIN "E"."array" AS "x""1`" ON true;

--#[quoted-identifiers-11]
SELECT "x""1`" FROM "default"."T" AS "E" INNER JOIN "E"."array" AS "x""1`" ON true;

--#[quoted-identifiers-12]
SELECT "x""1`" FROM "default"."T" AS "E" INNER JOIN "E"."array" AS "x""1`" ON true;

--#[quoted-identifiers-13]
SELECT "t"."c" AS "a`b" FROM "default"."T" AS "t";

--#[quoted-identifiers-14]
SELECT "t"."c" AS "a""b" FROM "default"."T" AS "t";

--#[quoted-identifiers-15]
SELECT "t"."x"."a`b" AS "v" FROM "default"."T" AS "t";

--#[quoted-identifiers-16]
SELECT "t"."x"."a""b" AS "v" FROM "default"."T" AS "t";

--#[quoted-identifiers-17]
SELECT "v"."a`b" FROM (SELECT "t"."c" AS "a`b" FROM "default"."T" AS "t") AS "v";
