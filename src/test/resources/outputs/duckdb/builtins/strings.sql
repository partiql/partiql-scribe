--#[trim-00]
SELECT TRIM(BOTH FROM "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[trim-01]
SELECT TRIM(BOTH FROM "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[trim-02]
SELECT TRIM(LEADING FROM "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[trim-03]
SELECT TRIM(TRAILING FROM "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[trim-04]
SELECT TRIM(BOTH 'xxx' FROM "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[trim-05]
SELECT TRIM(LEADING 'xxx' FROM "T"."c") AS "_1" FROM "default"."T" AS "T";

--#[trim-06]
SELECT TRIM(TRAILING 'xxx' FROM "T"."c") AS "_1" FROM "default"."T" AS "T";

-- --#[substring-00] TODO(duckdb)
SELECT SUBSTRING("T"."c", 2) AS "_1" FROM "default"."T" AS "T";

-- --#[substring-01] TODO(duckdb)
SELECT SUBSTRING("T"."c", 2, 3) AS "_1" FROM "default"."T" AS "T";

-- --#[substring-02] TODO(duckdb)
SELECT SUBSTRING("T"."c", 2) AS "_1" FROM "default"."T" AS "T";

-- --#[substring-03] TODO(duckdb)
SELECT SUBSTRING("T"."c", 2, 3) AS "_1" FROM "default"."T" AS "T";

-- --#[substring-10] TODO(duckdb)
[ScribeException{code=UNSUPPORTED_OPERATION, message="Scribe rejects DuckDB substring with a literal start less than 1 because DuckDB accepts start values less than 1 with different semantics. Non-literal start/length expressions are passed through unchanged."}];

-- --#[substring-11] TODO(duckdb)
[ScribeException{code=UNSUPPORTED_OPERATION, message="Scribe rejects DuckDB substring with a literal start less than 1 because DuckDB accepts start values less than 1 with different semantics. Non-literal start/length expressions are passed through unchanged."}];

-- --#[substring-12] TODO(duckdb)
[ScribeException{code=UNSUPPORTED_OPERATION, message="Scribe rejects DuckDB substring with a literal start less than 1 because DuckDB accepts start values less than 1 with different semantics. Non-literal start/length expressions are passed through unchanged."}];

-- --#[substring-13] TODO(duckdb)
[ScribeException{code=UNSUPPORTED_OPERATION, message="Scribe rejects DuckDB substring with a literal start less than 1 because DuckDB accepts start values less than 1 with different semantics. Non-literal start/length expressions are passed through unchanged."}];

-- --#[substring-14] TODO(duckdb)
[ScribeException{code=UNSUPPORTED_OPERATION, message="Scribe rejects DuckDB substring with a literal start less than 1 because DuckDB accepts start values less than 1 with different semantics. Non-literal start/length expressions are passed through unchanged."}];

--#[position-00]
SELECT POSITION('a' IN "T"."c") AS "_1" FROM "default"."T" AS "T";

-- --#[char-length-00] TODO(duckdb)
SELECT length("T"."c") AS "_1" FROM "default"."T" AS "T";

-- --#[char-length-01] TODO(duckdb)
SELECT length("T"."d"."e") AS "_1" FROM "default"."T" AS "T";

-- --#[char-length-02] TODO(duckdb)
SELECT length("T_ALL_TYPES"."col_list_string"[2]) AS "_1" FROM "default"."T_ALL_TYPES" AS "T_ALL_TYPES";

--#[replace-00]
SELECT REPLACE("T"."c", 'a', 'b') AS "_1" FROM "default"."T" AS "T";

-- --#[split-00] TODO(duckdb)
SELECT string_split("T"."c", ',') AS "_1" FROM "default"."T" AS "T";

-- --#[split-01] TODO(duckdb)
SELECT string_split("T"."c", '.') AS "_1" FROM "default"."T" AS "T";

-- --#[split-02] TODO(duckdb)
SELECT string_split("T"."c", '|') AS "_1" FROM "default"."T" AS "T";

-- --#[split-03] TODO(duckdb)
SELECT string_split("T"."c", '\') AS "_1" FROM "default"."T" AS "T";

-- --#[split-04] TODO(duckdb)
SELECT string_split("T"."c", '::') AS "_1" FROM "default"."T" AS "T";

-- --#[split-05] TODO(duckdb)
SELECT string_split("T"."c", "T"."z") AS "_1" FROM "default"."T" AS "T";

-- --#[split-06] TODO(duckdb)
SELECT string_split("T"."c", '[a-z]+') AS "_1" FROM "default"."T" AS "T";

-- --#[split-07] TODO(duckdb)
SELECT string_split("T"."z", "T"."c") AS "_1" FROM "default"."T" AS "T";

-- This will be runtime exception
-- --#[split-08] TODO(duckdb)
SELECT string_split("T"."c", '') AS "_1" FROM "default"."T" AS "T";
