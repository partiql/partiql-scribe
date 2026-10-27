-- --#[integer-00] TODO(redshift)
SELECT CAST("t"['b'] AS SMALLINT) AS "v1", CAST("t"['b'] AS INT) AS "v2", CAST("t"['b'] AS BIGINT) AS "v3", CAST("t"['b'] AS SMALLINT) AS "v4" FROM "default"."T" AS "t";

-- --#[decimal-00] TODO(redshift)
SELECT CAST("t"['b'] AS DECIMAL(5,0)) AS "v1", CAST("t"['b'] AS DECIMAL(18,0)) AS "v2" FROM "default"."T" AS "t";

-- --#[decimal-01] TODO(redshift)
[INVALID_INPUT{message="DECIMAL scale 7 is greater than the precision 5"}];

-- --#[decimal-02] TODO(redshift)
[INVALID_INPUT{message="DECIMAL precision 39 is not between 1 and 38"}];

-- --#[decimal-03] TODO(redshift)
[INVALID_INPUT{message="DECIMAL precision 0 is not between 1 and 38"}];

-- --#[float-00] TODO(redshift)
SELECT CAST("t"['b'] AS DOUBLE PRECISION) AS "v1", CAST("t"['b'] AS DOUBLE PRECISION) AS "v2", CAST("t"['b'] AS REAL) AS "v3", CAST("t"['b'] AS REAL) AS "v4", CAST("t"['b'] AS DOUBLE PRECISION) AS "v5" FROM "default"."T" AS "t";

-- --#[float-01] TODO(redshift)
[INVALID_INPUT{message="FLOAT precision 0 is not between 1 and 53"}];

-- --#[boolean-00] TODO(redshift)
SELECT CAST("t"['b'] AS BOOL) AS "v" FROM "default"."T" AS "t";

-- --#[char-00] TODO(redshift)
SELECT CAST("t"['b'] AS CHAR(5)) AS "v1", CAST("t"['b'] AS CHAR(1)) AS "v2", CAST("t"['b'] AS CHAR(5)) AS "v3", CAST("t"['b'] AS CHAR(4096)) AS "v4", CAST("t"['b'] AS CHAR(3)) AS "v5" FROM "default"."T" AS "t";

-- --#[char-01] TODO(redshift)
[UNSUPPORTED{message="data type `bpchar` without a length"}];

-- --#[char-02] TODO(redshift)
[INVALID_INPUT{message="CHAR length 5000 is not between 1 and 4096"}];

-- --#[varchar-00] TODO(redshift)
SELECT CAST("t"['b'] AS STRING) AS "v1", CAST("t"['b'] AS VARCHAR(10)) AS "v2", CAST("t"['b'] AS VARCHAR(10)) AS "v3", CAST("t"['b'] AS VARCHAR(5)) AS "v4" FROM "default"."T" AS "t";

-- --#[varchar-01] TODO(redshift)
[INVALID_INPUT{message="VARCHAR length 0 is not between 1 and 65535"}];

-- --#[varchar-02] TODO(redshift)
[INVALID_INPUT{message="VARCHAR length 70000 is not between 1 and 65535"}];

-- --#[varchar-03] TODO(redshift)
[INVALID_INPUT{message="VARCHAR length 99999999999 is not between 1 and 65535"}];

-- --#[datetime-00] TODO(redshift)
SELECT CAST("t"['timestamp_1'] AS DATE) AS "v1", CAST("t"['timestamp_1'] AS TIME (6)) AS "v2", CAST("t"['timestamp_1'] AS TIMESTAMP (6) WITH TIME ZONE) AS "v3" FROM "default"."T" AS "t";

-- --#[unsupported-00] TODO(redshift)
[UNSUPPORTED{message="data type `binary varying`"}];

-- --#[unsupported-01] TODO(redshift)
[UNSUPPORTED{message="data type `varbyte`"}];

-- --#[unsupported-02] TODO(redshift)
[UNSUPPORTED{message="data type `time`"}];
