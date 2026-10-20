--#[in-00]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" IN (1, 2);

--#[in-01]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" IN (1, 2);

--#[in-02]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" IN (1, 2);

--#[in-03]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" NOT IN (1, 2);

--#[in-04]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" NOT IN (1, 2);

--#[in-05]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" NOT IN (1, 2);

--#[in-06]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE ("T"."b", "T"."c") IN ((1, 'hello'), (2, 'world'));

--#[in-07]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE ("T"."b", "T"."c") NOT IN ((1, 'hello'), (2, 'world'));


--#[in-08]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE CASE WHEN get_array_length("T"."array") = 0 THEN false WHEN "T"."b" IS NULL OR "T"."array" IS NULL THEN NULL WHEN array_contains("T"."array", "T"."b") THEN true WHEN (SELECT COUNT(1) FROM "T"."array" AS ___partiql_scribe_in_element___ WHERE ___partiql_scribe_in_element___ IS NULL) > 0 THEN NULL ELSE false END;

--#[in-09]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE NOT (CASE WHEN get_array_length("T"."array") = 0 THEN false WHEN "T"."b" IS NULL OR "T"."array" IS NULL THEN NULL WHEN array_contains("T"."array", "T"."b") THEN true WHEN (SELECT COUNT(1) FROM "T"."array" AS ___partiql_scribe_in_element___ WHERE ___partiql_scribe_in_element___ IS NULL) > 0 THEN NULL ELSE false END);

--#[in-10]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" IN (SELECT "T"."b" FROM "default"."T" AS "T");

--#[in-11]
SELECT "T"."a", "T"."b", "T"."c", "T"."d", "T"."x", "T"."array", "T"."z", "T"."v", "T"."timestamp_1", "T"."timestamp_2" FROM "default"."T" AS "T" WHERE "T"."b" NOT IN (SELECT "T"."b" FROM "default"."T" AS "T");

--#[in-12]
SELECT CASE WHEN "T"."b" IN (1) THEN true ELSE CAST(NULL AS BOOLEAN) END AS "r" FROM "default"."T" AS "T";

--#[in-13]
SELECT NOT (CASE WHEN "T"."b" IN (1) THEN true ELSE CAST(NULL AS BOOLEAN) END) AS "r" FROM "default"."T" AS "T";

--#[in-16]
SELECT "T"."b", NOT (false) AS "r" FROM "default"."T" AS "T";

--#[in-17]
SELECT "T"."b", NOT (CASE WHEN get_array_length("T"."array") = 0 THEN false WHEN CAST(NULL AS INT) IS NULL OR "T"."array" IS NULL THEN NULL WHEN array_contains("T"."array", CAST(NULL AS INT)) THEN true WHEN (SELECT COUNT(1) FROM "T"."array" AS ___partiql_scribe_in_element___ WHERE ___partiql_scribe_in_element___ IS NULL) > 0 THEN NULL ELSE false END) AS "r" FROM "default"."T" AS "T";
