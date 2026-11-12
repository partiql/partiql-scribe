--#[quoted-identifiers-00]
SELECT "t"."a""b" AS "a""b" FROM "default"."T_QUOTED_FIELDS" AS "t";

--#[quoted-identifiers-01]
SELECT "t"."c`d" AS "c`d" FROM "default"."T_QUOTED_FIELDS" AS "t";

--#[quoted-identifiers-02]
SELECT "t"."a""b" AS "p""q`r" FROM "default"."T_QUOTED_FIELDS" AS "t";

--#[quoted-identifiers-03]
SELECT "t""1`2"."a""b" AS "a""b" FROM "default"."T_QUOTED_FIELDS" AS "t""1`2";

--#[quoted-identifiers-04]
SELECT "t"."x""); DROP TABLE target--" AS "x""); DROP TABLE target--" FROM "default"."T_QUOTED_FIELDS" AS "t";

--#[quoted-identifiers-05]
SELECT "t"."s""t"."u""v" AS "u""v", "t"."s""t"."w`x" AS "w`x" FROM "default"."T_QUOTED_FIELDS" AS "t";

--#[quoted-identifiers-06]
SELECT "t"."a""b" AS "k""1`2" FROM "default"."T_QUOTED_FIELDS" AS "t" GROUP BY "t"."a""b";

--#[quoted-identifiers-07]
WITH "w""1`2" AS (SELECT "t"."a""b" AS "a""b" FROM "default"."T_QUOTED_FIELDS" AS "t") SELECT "w""1`2"."a""b" AS "a""b" FROM "w""1`2" AS "w""1`2";

--#[quoted-identifiers-08]
SELECT "t"."a""b" AS "a""b", "t"."c`d" AS "c`d", "t"."x""); DROP TABLE target--" AS "x""); DROP TABLE target--", {'w`x': "t"."s""t"."w`x"} AS "s""t" FROM "default"."T_QUOTED_FIELDS" AS "t";

--#[quoted-identifiers-09]
SELECT "t"."a""b" AS "a""b", "t"."c`d" AS "c`d", "t"."x""); DROP TABLE target--" AS "x""); DROP TABLE target--", {'u"v': "t"."s""t"."u""v"} AS "s""t" FROM "default"."T_QUOTED_FIELDS" AS "t";
