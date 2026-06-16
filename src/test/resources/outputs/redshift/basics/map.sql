-- ----------------------------------------
--  Map construction — literal
-- ----------------------------------------

--#[map-00]
-- Construct a map literal with string keys
SELECT OBJECT('a', 1, 'b', 2, 'c', 3) AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- map-01 (decimal keys) is not supported — Redshift SUPER OBJECT requires string keys

--#[map-02]
-- Construct a map literal referencing columns for values
SELECT OBJECT('x', CAST("T"."col_int32" AS DOUBLE PRECISION), 'y', "T"."col_float64") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[map-03]
-- Construct a map literal referencing columns for keys
SELECT OBJECT("T"."col_string", "T"."col_int32") AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

--#[map-04]
-- Construct an empty map literal
SELECT OBJECT() AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- map-05 (integer keys) is not supported — Redshift SUPER OBJECT requires string keys

-- map-06 (expression as key with integer type) is not supported — Redshift SUPER OBJECT requires string keys

-- ----------------------------------------
--  Select map columns
-- ----------------------------------------

--#[map-07]
-- Select map alongside other columns
SELECT "T"."col_int32", "T"."col_string", "T"."col_map_str_key" FROM "default"."T_ALL_TYPES" AS "T";

--#[map-08]
-- Select map column with float key type
SELECT "T"."col_map_float_key" FROM "default"."T_ALL_TYPES" AS "T";

-- map-09 (bracket lookup with contains_key guard) is not supported — Redshift does not have contains_key

-- map-10 (float key bracket lookup) is not supported — Redshift SUPER uses dot notation for string keys only

-- map-11 (column as key bracket lookup) is not supported — Redshift SUPER does not support dynamic key access

-- map-12 to map-17 (map_keys, map_values, map_entries) are not supported — Redshift does not have these functions for SUPER type

-- map-18 to map-21 (size, cardinality, exists) are not supported — Redshift does not have equivalent functions for SUPER map type

-- map-22 to map-24 (contains_key) are not supported — Redshift does not have equivalent functions for SUPER map type

-- ----------------------------------------
--  Map functions — map_get
-- ----------------------------------------

--#[map-25]
-- map_get with string key
SELECT "T"."col_map_str_key"."a" AS "_1" FROM "default"."T_ALL_TYPES" AS "T";

-- map-26 (map_get with column reference as key) is not supported — Redshift only supports string literal keys
-- map-27 (map_get with float key) is not supported — Redshift only supports string literal keys

-- ----------------------------------------
--  Filtering
-- ----------------------------------------

--#[map-31]
-- Filter with map lookup comparison
SELECT "T"."col_int32" FROM "default"."T_ALL_TYPES" AS "T" WHERE "T"."col_map_str_key"."a" > 10;

-- map-32 (filter with contains_key) is not supported — Redshift does not have equivalent function

-- map-33 (filter with size) is not supported — Redshift does not have equivalent function

-- ----------------------------------------
--  Aggregation
-- ----------------------------------------

--#[map-34]
-- Group by map lookup
SELECT "T"."col_map_str_key"."a", count(1) AS "_1" FROM "default"."T_ALL_TYPES" AS "T" GROUP BY "T"."col_map_str_key"."a";
