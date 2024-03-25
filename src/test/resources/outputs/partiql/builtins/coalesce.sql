--#[builtins-coalesce-00]
SELECT COALESCE("T"['a']) AS "_1" FROM "default"."T" AS "T";

--#[builtins-coalesce-01]
SELECT COALESCE("T"['a'], "T"['b']) AS "_1" FROM "default"."T" AS "T";

--#[builtins-coalesce-02]
SELECT COALESCE("T"['a'], "T"['b'], "T"['c']) AS "_1" FROM "default"."T" AS "T";

--#[builtins-coalesce-03]
SELECT COALESCE(COALESCE(COALESCE("T"['a'], "T"['b'], "T"['c']), "T"['b']), "T"['c']) AS "_1" FROM "default"."T" AS "T";
