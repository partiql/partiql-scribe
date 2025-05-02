--#[builtins-nullif-00]
SELECT NULLIF("T"['a'], "T"['b']) AS "_1" FROM "default"."T" AS "T";

--#[builtins-nullif-01]
SELECT NULLIF("T"['b'], "T"['a']) AS "_1" FROM "default"."T" AS "T";

--#[builtins-nullif-02]
SELECT NULLIF(NULLIF(NULLIF("T"['a'], "T"['b']), "T"['c']), "T"['d']) AS "_1" FROM "default"."T" AS "T";
-- Existing test was easy to misinterpret. It was a NULLIF of a row value expression with a NULLIF

--#[builtins-nullif-03]
SELECT NULLIF([NULLIF("T"['a'], "T"['b']), "T"['c']], "T"['d']) AS "_1" FROM "default"."T" AS "T";
-- Previously was
-- SELECT NULLIF((NULLIF("T"['a'], "T"['b']), "T"['c']), "T"['d']) AS "_1" FROM "default"."T" AS "T";

--#[builtins-nullif-04]
SELECT COALESCE(NULLIF(COALESCE("T"['a'], "T"['b'], "T"['c']), "T"['b']), "T"['c']) AS "_1" FROM "default"."T" AS "T";