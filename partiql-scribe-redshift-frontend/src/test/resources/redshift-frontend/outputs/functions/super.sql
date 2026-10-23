--#[super-array-00]
SELECT [1, "t"['b'], NULL, "t"['c'], 'abc'] AS "v1", [] AS "v2", [[1, 2], [3]] AS "v3" FROM "default"."T" AS "t";

--#[super-array-01]
SELECT [COALESCE("t"['x']), "t"['b']] AS "v1", [COALESCE("t"['x'])] AS "v2" FROM "default"."T" AS "t";

--#[super-array-02]
[UNSUPPORTED{message="`ARRAY` with a TIMESTAMP argument (Redshift does not convert it to SUPER)"}];

--#[super-array-03]
[UNSUPPORTED{message="`ARRAY` with a CHAR argument"}];

--#[super-object-00]
SELECT {'a': 1, 'b': 'x', 'c': [1, 2], 'd': 10 + 5} AS "v1", {} AS "v2" FROM "default"."T" AS "t";

--#[super-object-01]
[UNSUPPORTED{message="`OBJECT` with a value that can be NULL (Redshift does not document whether it keeps the attribute)"}];

--#[super-object-02]
[UNSUPPORTED{message="`OBJECT` with a key that is not a string literal"}];

--#[super-object-03]
[UNSUPPORTED{message="`OBJECT` with duplicate keys"}];

--#[super-get-array-length-00]
SELECT CASE WHEN "t"['x'] IS LIST THEN "cardinality"("t"['x']) ELSE NULL END AS "v1", CASE WHEN "t"['array'] IS LIST THEN "cardinality"("t"['array']) ELSE NULL END AS "v2" FROM "default"."T" AS "t";

--#[super-get-number-attributes-00]
SELECT CASE WHEN "t"['x'] IS STRUCT THEN "cardinality"("t"['x']) ELSE NULL END AS "v" FROM "default"."T" AS "t";

--#[super-array-contains-00]
SELECT CASE WHEN "t"['x'] IS LIST THEN COALESCE('red' IN "t"['x'], false) ELSE NULL END AS "v" FROM "default"."T" AS "t";

--#[super-array-contains-01]
SELECT CASE WHEN "t"['array'] IS LIST THEN CASE WHEN "t"['b'] IS NULL THEN (SELECT count(1) AS "_1" FROM @"t"['array'] AS "$e1" WHERE "$e1" IS NULL) > CAST(0 AS BIGINT) ELSE COALESCE("t"['b'] IN "t"['array'], false) END ELSE NULL END AS "v" FROM "default"."T" AS "t";

--#[super-array-contains-02]
SELECT CASE WHEN "t"['x'] IS LIST THEN "t"['c'] IN "t"['x'] ELSE NULL END AS "v" FROM "default"."T" AS "t";

--#[super-array-contains-03]
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" WHERE CASE WHEN "t"['x'] IS LIST THEN COALESCE('red' IN "t"['x'], false) ELSE NULL END;

--#[super-array-contains-04]
[UNSUPPORTED{message="`ARRAY_CONTAINS` with a non-literal null_match argument"}];

--#[super-arrays-overlap-00]
SELECT CASE WHEN "t"['x'] IS LIST AND "t"['array'] IS LIST THEN (SELECT count(1) AS "_1" FROM @"t"['x'] AS "$e1" WHERE (SELECT count(1) AS "_1" FROM @"t"['array'] AS "$e2" WHERE ("$e1" = "$e2") OR ("$e1" IS NULL AND "$e2" IS NULL)) > CAST(0 AS BIGINT)) > CAST(0 AS BIGINT) ELSE NULL END AS "v" FROM "default"."T" AS "t";

--#[super-array-distinct-00]
SELECT CASE WHEN "t"['x'] IS LIST THEN (SELECT DISTINCT VALUE "$e1" FROM @"t"['x'] AS "$e1" ORDER BY "$e1" ASC NULLS LAST) ELSE NULL END AS "v" FROM "default"."T" AS "t";

--#[super-array-sort-00]
SELECT CASE WHEN "t"['x'] IS LIST THEN (SELECT VALUE "$e1" FROM @"t"['x'] AS "$e1" ORDER BY "$e1" ASC NULLS LAST) ELSE NULL END AS "v1", CASE WHEN "t"['x'] IS LIST THEN (SELECT VALUE "$e1" FROM @"t"['x'] AS "$e1" ORDER BY "$e1" DESC NULLS FIRST) ELSE NULL END AS "v2", CASE WHEN "t"['x'] IS LIST THEN (SELECT VALUE "$e1" FROM @"t"['x'] AS "$e1" ORDER BY "$e1" DESC NULLS LAST) ELSE NULL END AS "v3" FROM "default"."T" AS "t";

--#[super-array-union-00]
SELECT CASE WHEN "t"['x'] IS LIST AND "t"['array'] IS LIST THEN (SELECT DISTINCT VALUE "$e1" FROM [@"t"['x'], @"t"['array']] AS "$p1" INNER JOIN @"$p1" AS "$e1" ON true ORDER BY "$e1" ASC NULLS LAST) ELSE NULL END AS "v" FROM "default"."T" AS "t";

--#[super-array-intersection-00]
SELECT CASE WHEN "t"['x'] IS LIST AND "t"['array'] IS LIST THEN (SELECT DISTINCT VALUE "$e1" FROM @"t"['x'] AS "$e1" WHERE (SELECT count(1) AS "_1" FROM @"t"['array'] AS "$e2" WHERE ("$e1" = "$e2") OR ("$e1" IS NULL AND "$e2" IS NULL)) > CAST(0 AS BIGINT) ORDER BY "$e1" ASC NULLS LAST) ELSE NULL END AS "v" FROM "default"."T" AS "t";

--#[super-array-intersection-01]
[UNSUPPORTED{message="`ARRAY_INTERSECTION` without a literal TRUE distinct argument (duplicate elements)"}];

--#[super-is-00]
SELECT COALESCE("t"['x'] IS LIST, false) AS "v1", COALESCE("t"['x'] IS STRUCT, false) AS "v2", CASE WHEN ("t"['x'] IS NULL OR "t"['x'] IS LIST) OR "t"['x'] IS STRUCT THEN false ELSE true END AS "v3", COALESCE("t"['x'] IS BOOL, false) AS "v4" FROM "default"."T" AS "t";

--#[super-is-01]
SELECT COALESCE("t"['x'] IS DOUBLE PRECISION, false) AS "v1", COALESCE("t"['x'] IS DECIMAL OR "t"['x'] IS INT, false) AS "v2" FROM "default"."T" AS "t";

--#[super-is-integer-00]
SELECT "t"['b'] IS NOT NULL AS "v1", COALESCE("t"['b'] BETWEEN -32768 AND 32767, false) AS "v2", "t"['b'] IS NOT NULL AS "v3" FROM "default"."T" AS "t";

--#[super-is-integer-01]
SELECT COALESCE("t_all_types"['col_int64'] BETWEEN -2147483648 AND 2147483647, false) AS "v1", false AS "v2" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[super-is-integer-02]
SELECT COALESCE("t_decimals"['de'] BETWEEN -2147483648 AND 2147483647, false) AS "v1", "t_decimals"['de'] IS NOT NULL AS "v2" FROM "default"."T_DECIMALS" AS "t_decimals";

--#[super-is-integer-03]
SELECT false AS "v" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

--#[super-is-integer-04]
[UNSUPPORTED{message="`IS_INTEGER` of a SUPER value (the test for a DECIMAL value of scale 0 cannot be printed)"}];

--#[super-is-varchar-00]
SELECT COALESCE(CHAR_LENGTH("t"['x']) >= 0, false) AS "v" FROM "default"."T" AS "t";

--#[super-is-varchar-01]
SELECT "t_all_types"['col_char'] IS NOT NULL AS "v1", false AS "v2" FROM "default"."T_ALL_TYPES" AS "t_all_types";

--#[super-is-char-00]
SELECT COALESCE("octet_length"("t"['x']) = CHAR_LENGTH("t"['x']), false) AS "v1", false AS "v2" FROM "default"."T" AS "t";

--#[super-unsupported-00]
[UNSUPPORTED{message="function `JSON_TYPEOF`; the result for a NULL value is not documented"}];

--#[super-unsupported-01]
[UNSUPPORTED{message="function `SPLIT_TO_ARRAY`; the result for an empty string is not documented"}];

--#[super-unsupported-02]
[UNSUPPORTED{message="function `JSON_PARSE`; PartiQL has no JSON text functions"}];

--#[super-unsupported-03]
[UNSUPPORTED{message="function `JSON_EXTRACT_PATH_TEXT`; PartiQL has no JSON text functions"}];

--#[super-unsupported-04]
[UNSUPPORTED{message="function `ARRAY_CONCAT`; PartiQL has no array concatenation that keeps the order"}];

--#[super-unsupported-05]
[UNSUPPORTED{message="function `SUBARRAY`; PartiQL cannot print the element positions that it needs"}];

--#[super-unsupported-06]
[UNSUPPORTED{message="function `SIZE`; PartiQL SIZE is the number of elements, not the size in bytes"}];
