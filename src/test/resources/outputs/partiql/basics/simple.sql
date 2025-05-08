-- ------------------
--  Globals
-- ------------------

--#[global-00]
"default"."my_global";

-- ------------------
--  Literals
-- ------------------

--#[sanity-lit-00]
true;

--#[sanity-lit-01]
1;

--#[sanity-lit-02]
1.0;

--#[sanity-lit-03]
'hello';

--#[sanity-lit-04]
['a', 'b', 'c'];

--#[sanity-lit-05]
<<'a', 'b', 'c'>>;

--#[sanity-lit-06]
{'a': 1, 'b': 2, 'c': 3};

-- ------------------
--  Basic Expressions
-- ------------------

--#[expr-00]
"default"."T1" OR "default"."T1";

--#[expr-01]
"default"."T1" AND "default"."T1";

--#[expr-02]
NOT ("default"."T1");

--#[expr-03]
"default"."T1" < "default"."T1";

--#[expr-04]
"default"."T1" <= "default"."T1";

--#[expr-05]
"default"."T1" > "default"."T1";

--#[expr-06]
"default"."T1" >= "default"."T1";

--#[expr-07]
"default"."T1" = "default"."T1";

--#[expr-08]
"default"."T1" <> "default"."T1";

--#[expr-09]
"default"."T1" <> "default"."T1";

--#[expr-10]
"default"."T1" IS NULL;

--#[expr-11]
"default"."T1" IS NOT NULL;

--#[expr-12]
"default"."T1" IS MISSING;

--#[expr-13]
"default"."T1" IS NOT MISSING;

--#[expr-14]
"default"."T1" IS SMALLINT;

--#[expr-15]
"default"."T1" IS NOT SMALLINT;

--#[expr-16]
-- TODO USE INT as the default INT4 name.
"default"."T1" IS INT;

--#[expr-17]
-- TODO USE INT as the default INT4 name.
"default"."T1" IS NOT INT;

--#[expr-18]
-- TODO USE BIGINT as the default BIGINT/INT8 name.
"default"."T1" IS BIGINT;

--#[expr-19]
-- TODO USE BIGINT as the default BIGINT/INT8 name.
"default"."T1" IS NOT BIGINT;

--#[expr-20]
"default"."T1" IS INT;

--#[expr-21]
"default"."T1" IS NOT INT;

--#[expr-22]
"default"."T1" IS DECIMAL;

--#[expr-23]
"default"."T1" IS NOT DECIMAL;

--#[expr-24]
"default"."T1" IS FLOAT;

--#[expr-25]
"default"."T1" IS NOT FLOAT;

--#[expr-26]
"default"."T1" IS BOOL;

--#[expr-27]
"default"."T1" IS NOT BOOL;

-- `IS SYMBOL` is not support in PLK 1.x
-- --#[expr-28]
-- "default"."T1" IS SYMBOL;
--
-- --#[expr-29]
-- "default"."T1" IS NOT SYMBOL;

--#[expr-30]
"default"."T1" IS DATE;

--#[expr-31]
"default"."T1" IS NOT DATE;

--#[expr-32]
"default"."T1" IS TIME;

--#[expr-33]
"default"."T1" IS NOT TIME;

--#[expr-34]
"default"."T1" IS TIMESTAMP;

--#[expr-35]
"default"."T1" IS NOT TIMESTAMP;

--#[expr-36]
"default"."T1" IS STRING;

--#[expr-37]
"default"."T1" IS NOT STRING;

--#[expr-38]
"default"."T1" IS CLOB;

--#[expr-39]
"default"."T1" IS NOT CLOB;

--#[expr-40]
"default"."T1" IS BLOB;

--#[expr-41]
"default"."T1" IS NOT BLOB;

--#[expr-42]
"default"."T1" IS LIST;

--#[expr-43]
"default"."T1" IS NOT LIST;

-- SEXP data type not in PLK 1.x
-- --#[expr-44]
-- "default"."T1" IS SEXP;
--
-- --#[expr-45]
-- "default"."T1" IS NOT SEXP;

--#[expr-46]
"default"."T1" IS STRUCT;

--#[expr-47]
"default"."T1" IS NOT STRUCT;

--#[expr-48]
"default"."T1" IS BAG;

--#[expr-49]
"default"."T1" IS NOT BAG;

--#[expr-50]
"default"."T1" IN (true);

--#[expr-51]
"default"."T1" NOT IN (false);

--#[expr-52]
"default"."T1" IN "default"."T1";

--#[expr-53]
"default"."T1" NOT IN "default"."T1";

--#[expr-54]
"default"."T1" LIKE "default"."T1";

--#[expr-55]
"default"."T1" NOT LIKE "default"."T1";

--#[expr-56]
"default"."T1" LIKE "default"."T1" ESCAPE "default"."T1";

--#[expr-57]
"default"."T1" BETWEEN "default"."T1" AND "default"."T1";

--#[expr-58]
"default"."T1" NOT BETWEEN "default"."T1" AND "default"."T1";

--#[expr-59]
"default"."T1" || "default"."T1";

--#[expr-60]
"default"."T1" & "default"."T1";

--#[expr-61]
"default"."T1" + "default"."T1";

--#[expr-62]
"default"."T1" - "default"."T1";

--#[expr-63]
"default"."T1" % "default"."T1";

--#[expr-64]
"default"."T1" * "default"."T1";

--#[expr-65]
"default"."T1" / "default"."T1";

--#[expr-66]
CURRENT_USER;

--#[expr-67]
CURRENT_DATE;

--#[expr-68]
'My name is ' || CURRENT_USER;

-- TO BE CONTINUED ....
