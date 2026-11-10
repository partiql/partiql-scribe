--#[exclude-special-keys-00]
SELECT OBJECT_TRANSFORM("t"."flds" KEEP '"k\\\\"', '"k\\"q"') AS "flds" FROM "default"."EXCLUDE_T_SPECIAL_KEYS" AS "t";
