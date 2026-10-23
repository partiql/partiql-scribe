-- --#[str-len-00] TODO(redshift)
SELECT CHAR_LENGTH(TRIM(TRAILING FROM "t_all_types"['col_char'])) AS "v1", CHAR_LENGTH("t_all_types"['col_string']) AS "v2", CHAR_LENGTH(TRIM(TRAILING FROM "t_all_types"['col_char'])) AS "v3" FROM "default"."T_ALL_TYPES" AS "t_all_types";

-- --#[str-len-01] TODO(redshift)
SELECT CHAR_LENGTH("t"['c']) AS "v" FROM "default"."T" AS "t";

-- --#[str-len-02] TODO(redshift)
SELECT CHAR_LENGTH(CAST("t"['b'] AS STRING)) AS "v1", CHAR_LENGTH(CAST("t_all_types"['col_date'] AS STRING)) AS "v2" FROM "default"."T" AS "t" INNER JOIN "default"."T_ALL_TYPES" AS "t_all_types" ON true;

-- --#[str-len-03] TODO(redshift)
SELECT CHAR_LENGTH(CAST("t"['foo']['keep'] AS STRING)) AS "v" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t";

-- --#[str-len-04] TODO(redshift)
[UNSUPPORTED{message="implicit conversion of DECIMAL(38, 0) to VARCHAR in `LEN`"}];

-- --#[str-len-05] TODO(redshift)
[UNSUPPORTED{message="implicit conversion of TIMESTAMP to VARCHAR in `LEN`"}];

-- --#[str-len-06] TODO(redshift)
[UNSUPPORTED{message="implicit conversion of DOUBLE PRECISION to VARCHAR in `LEN`"}];

-- --#[str-len-07] TODO(redshift)
SELECT CHAR_LENGTH(COALESCE(TRIM(TRAILING FROM "t_all_types"['col_char']), "t_all_types"['col_string'])) AS "v" FROM "default"."T_ALL_TYPES" AS "t_all_types";

-- --#[str-octet-length-00] TODO(redshift)
SELECT "octet_length"("t_all_types"['col_char']) AS "v" FROM "default"."T_ALL_TYPES" AS "t_all_types";

-- --#[str-octet-length-01] TODO(redshift)
[UNSUPPORTED{message="`OCTET_LENGTH` of a conditional expression that mixes CHAR with other character values"}];

-- --#[str-upper-00] TODO(redshift)
SELECT "upper"("t_all_types"['col_char']) AS "v1", "lower"(CAST("t"['b'] AS STRING)) AS "v2" FROM "default"."T" AS "t" INNER JOIN "default"."T_ALL_TYPES" AS "t_all_types" ON true;

-- --#[str-substring-00] TODO(redshift)
SELECT SUBSTRING("t"['c'] FROM 2 FOR 3) AS "v1", SUBSTRING("t"['c'] FROM 2) AS "v2", SUBSTRING("t"['c'] FROM 2 FOR 3) AS "v3", SUBSTRING("t"['c'] FROM 1 FOR 3) AS "v4" FROM "default"."T" AS "t";

-- --#[str-substring-01] TODO(redshift)
SELECT SUBSTRING("t"['c'] FROM -2 FOR 6) AS "v" FROM "default"."T" AS "t";

-- --#[str-substring-02] TODO(redshift)
SELECT SUBSTRING(TRIM(TRAILING FROM "t_all_types"['col_char']) FROM 1 FOR 5) AS "v" FROM "default"."T_ALL_TYPES" AS "t_all_types";

-- --#[str-substring-03] TODO(redshift)
SELECT SUBSTRING(CAST("t"['b'] AS STRING) FROM 1 FOR 2) AS "v" FROM "default"."T" AS "t";

-- --#[str-substring-04] TODO(redshift)
[UNSUPPORTED{message="implicit conversion of TIMESTAMP to VARCHAR in `SUBSTRING`"}];

-- --#[str-substring-05] TODO(redshift)
[UNSUPPORTED{message="`SUBSTRING` with a start of type DECIMAL(2, 1)"}];

-- --#[str-trim-00] TODO(redshift)
SELECT TRIM(BOTH FROM "t"['c']) AS "v1", TRIM(BOTH FROM "t"['c']) AS "v2", TRIM(LEADING 'x' FROM "t"['c']) AS "v3", TRIM(BOTH 'CDG' FROM "t"['c']) AS "v4" FROM "default"."T" AS "t";

-- --#[str-trim-01] TODO(redshift)
SELECT TRIM(TRAILING FROM "t_all_types"['col_char']) AS "v1", TRIM(LEADING FROM TRIM(TRAILING FROM "t_all_types"['col_char'])) AS "v2", TRIM(BOTH 'x' FROM TRIM(TRAILING FROM "t_all_types"['col_char'])) AS "v3" FROM "default"."T_ALL_TYPES" AS "t_all_types";

-- --#[str-btrim-00] TODO(redshift)
SELECT TRIM(BOTH FROM "t"['c']) AS "v1", TRIM(BOTH 'xyz' FROM "t"['c']) AS "v2" FROM "default"."T" AS "t";

-- --#[str-ltrim-00] TODO(redshift)
SELECT TRIM(LEADING FROM "t"['c']) AS "v1", TRIM(LEADING '2008-' FROM "t"['c']) AS "v2" FROM "default"."T" AS "t";

-- --#[str-rtrim-00] TODO(redshift)
SELECT TRIM(TRAILING 'Park' FROM "t"['c']) AS "v1", TRIM(TRAILING 'x' FROM TRIM(TRAILING FROM "t_all_types"['col_char'])) AS "v2" FROM "default"."T" AS "t" INNER JOIN "default"."T_ALL_TYPES" AS "t_all_types" ON true;

-- --#[str-position-00] TODO(redshift)
SELECT POSITION('a' IN "t"['c']) AS "v" FROM "default"."T" AS "t";

-- --#[str-position-01] TODO(redshift)
[UNSUPPORTED{message="CHAR argument to `POSITION`"}];

-- --#[str-strpos-00] TODO(redshift)
SELECT POSITION('a' IN "t"['c']) AS "v" FROM "default"."T" AS "t";

-- --#[str-strpos-01] TODO(redshift)
[UNSUPPORTED{message="CHAR argument to `STRPOS`"}];

-- --#[str-charindex-00] TODO(redshift)
SELECT POSITION('a' IN "t"['c']) AS "v" FROM "default"."T" AS "t";

-- --#[str-replace-00] TODO(redshift)
SELECT REPLACE("t"['c'], 'a', 'b') AS "v" FROM "default"."T" AS "t";

-- --#[str-split-part-00] TODO(redshift)
SELECT CASE WHEN "cardinality"(SPLIT("t"['c'], '$')) >= 2 THEN SPLIT("t"['c'], '$')[1] WHEN "t"['c'] IS NOT NULL THEN '' ELSE NULL END AS "v" FROM "default"."T" AS "t";

-- --#[str-split-part-01] TODO(redshift)
SELECT CASE WHEN "cardinality"(SPLIT("t"['c'], "t"['z'])) >= 1 THEN SPLIT("t"['c'], "t"['z'])[0] WHEN "t"['c'] IS NOT NULL AND "t"['z'] IS NOT NULL THEN '' ELSE NULL END AS "v" FROM "default"."T" AS "t";

-- --#[str-split-part-02] TODO(redshift)
SELECT CASE WHEN "cardinality"(SPLIT("t"['c'], ',')) >= "t"['b'] THEN SPLIT("t"['c'], ',')["t"['b'] - 1] WHEN "t"['c'] IS NOT NULL AND "t"['b'] IS NOT NULL THEN '' ELSE NULL END AS "v" FROM "default"."T" AS "t";

-- --#[str-split-part-03] TODO(redshift)
[UNSUPPORTED{message="`SPLIT_PART` with a part number less than 1"}];

-- --#[str-left-right-00] TODO(redshift)
SELECT SUBSTRING("t"['c'] FROM 1 FOR 5) AS "v1", SUBSTRING("t"['c'] FROM CHAR_LENGTH("t"['c']) - 4) AS "v2", SUBSTRING("t"['c'] FROM CHAR_LENGTH("t"['c'])) AS "v3" FROM "default"."T" AS "t";

-- --#[str-left-right-01] TODO(redshift)
[UNSUPPORTED{message="`LEFT` with a non-literal length"}];

-- --#[str-left-right-02] TODO(redshift)
[UNSUPPORTED{message="`RIGHT` with a length less than 1"}];

-- --#[str-concat-00] TODO(redshift)
SELECT "t"['c'] || "t"['z'] AS "v1", "t"['c'] || CAST("t"['b'] AS STRING) AS "v2" FROM "default"."T" AS "t";

-- --#[str-concat-01] TODO(redshift)
SELECT TRIM(TRAILING FROM "t_all_types"['col_char']) || "t_all_types"['col_string'] AS "v" FROM "default"."T_ALL_TYPES" AS "t_all_types";

-- --#[str-concat-02] TODO(redshift)
[UNSUPPORTED{message="`CONCAT` of two CHAR arguments"}];

-- --#[str-pad-00] TODO(redshift)
SELECT CASE WHEN CHAR_LENGTH("t"['c']) >= 5 THEN SUBSTRING("t"['c'] FROM 1 FOR 5) ELSE SUBSTRING('00000' FROM 1 FOR 5 - CHAR_LENGTH("t"['c'])) || "t"['c'] END AS "v" FROM "default"."T" AS "t";

-- --#[str-pad-01] TODO(redshift)
SELECT CASE WHEN CHAR_LENGTH("t"['c']) >= 20 THEN SUBSTRING("t"['c'] FROM 1 FOR 20) ELSE "t"['c'] || SUBSTRING('01234567890123456789' FROM 1 FOR 20 - CHAR_LENGTH("t"['c'])) END AS "v" FROM "default"."T" AS "t";

-- --#[str-pad-02] TODO(redshift)
SELECT CASE WHEN CHAR_LENGTH("t"['c']) >= 3 THEN SUBSTRING("t"['c'] FROM 1 FOR 3) ELSE SUBSTRING('   ' FROM 1 FOR 3 - CHAR_LENGTH("t"['c'])) || "t"['c'] END AS "v1", SUBSTRING("t"['c'] FROM 1 FOR 0) AS "v2" FROM "default"."T" AS "t";

-- --#[str-pad-03] TODO(redshift)
SELECT CASE WHEN CHAR_LENGTH(CAST("t"['b'] AS STRING)) >= 5 THEN SUBSTRING(CAST("t"['b'] AS STRING) FROM 1 FOR 5) ELSE SUBSTRING('00000' FROM 1 FOR 5 - CHAR_LENGTH(CAST("t"['b'] AS STRING))) || CAST("t"['b'] AS STRING) END AS "v" FROM "default"."T" AS "t";

-- --#[str-pad-04] TODO(redshift)
[UNSUPPORTED{message="`LPAD` with a non-literal length"}];

-- --#[str-pad-05] TODO(redshift)
[UNSUPPORTED{message="`RPAD` with an empty fill"}];

-- --#[str-repeat-00] TODO(redshift)
SELECT ("t"['c'] || "t"['c']) || "t"['c'] AS "v1", CAST("t"['b'] AS STRING) || CAST("t"['b'] AS STRING) AS "v2" FROM "default"."T" AS "t";

-- --#[str-repeat-01] TODO(redshift)
[UNSUPPORTED{message="`REPEAT` with a count other than 1 to 10"}];

-- --#[str-translate-00] TODO(redshift)
SELECT REPLACE(REPLACE("t"['c'], ' ', '_'), '.', '') AS "v" FROM "default"."T" AS "t";

-- --#[str-translate-01] TODO(redshift)
SELECT REPLACE(REPLACE(REPLACE(REPLACE("t"['c'], 'i', 'o'), 'n', 's'), 'e', 'i'), 'a', 'n') AS "v" FROM "default"."T" AS "t";

-- --#[str-translate-02] TODO(redshift)
SELECT REPLACE(REPLACE("t"['c'], 'a', 'x'), 'b', 'z') AS "v" FROM "default"."T" AS "t";

-- --#[str-translate-03] TODO(redshift)
[UNSUPPORTED{message="`TRANSLATE` with a cyclic character mapping"}];

-- --#[str-octetindex-00] TODO(redshift)
SELECT CASE WHEN POSITION('b' IN "t"['c']) = 0 THEN 0 ELSE "octet_length"(SUBSTRING("t"['c'] FROM 1 FOR CAST(POSITION('b' IN "t"['c']) - CAST(1 AS BIGINT) AS INT))) + 1 END AS "v" FROM "default"."T" AS "t";

-- --#[str-chr-00] TODO(redshift)
SELECT 'A' AS "v1", "t"['c'] || '''' AS "v2", NULL AS "v3", NULL AS "v4" FROM "default"."T" AS "t";

-- --#[str-chr-01] TODO(redshift)
[UNSUPPORTED{message="`CHR(32)` (a CHAR blank)"}];

-- --#[str-chr-02] TODO(redshift)
[UNSUPPORTED{message="`CHR` with a non-literal code"}];

-- --#[str-quote-literal-00] TODO(redshift)
SELECT ('''' || REPLACE(REPLACE("t"['c'], '\', '\\'), '''', '''''')) || '''' AS "v1", ('''' || CAST("t"['b'] AS STRING)) || '''' AS "v2" FROM "default"."T" AS "t";

-- --#[str-quote-literal-01] TODO(redshift)
[UNSUPPORTED{message="`QUOTE_LITERAL` of an argument of type CHAR(10)"}];

-- --#[str-regexp-00] TODO(redshift)
SELECT (CHAR_LENGTH("t"['c']) - CHAR_LENGTH(REPLACE("t"['c'], 'ab', ''))) / 2 AS "v1", CHAR_LENGTH("t"['c']) - CHAR_LENGTH(REPLACE("t"['c'], 'a', '')) AS "v2", POSITION('ab' IN "t"['c']) AS "v3" FROM "default"."T" AS "t";

-- --#[str-regexp-01] TODO(redshift)
SELECT REPLACE("t"['c'], ',', '') AS "v1", REPLACE("t"['c'], 'x', 'y') AS "v2" FROM "default"."T" AS "t";

-- --#[str-regexp-02] TODO(redshift)
SELECT CASE WHEN POSITION('ab' IN "t"['c']) > CAST(0 AS BIGINT) THEN 'ab' WHEN "t"['c'] IS NOT NULL THEN '' ELSE NULL END AS "v" FROM "default"."T" AS "t";

-- --#[str-regexp-03] TODO(redshift)
[UNSUPPORTED{message="`REGEXP_REPLACE` with a regular expression pattern; PartiQL has no regular expressions"}];

-- --#[str-regexp-04] TODO(redshift)
[UNSUPPORTED{message="`REGEXP_COUNT` with 3 argument(s); PartiQL has no regular expressions"}];

-- --#[str-unsupported-00] TODO(redshift)
[UNSUPPORTED{message="function `INITCAP`; PartiQL has no word capitalization"}];

-- --#[str-unsupported-01] TODO(redshift)
[UNSUPPORTED{message="function `SUBSTR`; a leader node-only function, which fails in a query that references a table"}];

-- --#[str-unsupported-02] TODO(redshift)
[UNSUPPORTED{message="function `REVERSE`; PartiQL has no string reversal"}];

-- --#[str-unsupported-03] TODO(redshift)
[UNSUPPORTED{message="function `ASCII`; PartiQL has no character code function"}];

-- --#[str-unsupported-04] TODO(redshift)
[UNSUPPORTED{message="function `STRTOL`; PartiQL has no base conversion"}];
