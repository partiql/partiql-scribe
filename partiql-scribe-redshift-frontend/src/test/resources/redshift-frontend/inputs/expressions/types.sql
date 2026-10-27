--#[integer-00]
SELECT b::int2 AS v1, b::int4 AS v2, b::int8 AS v3, b::smallint AS v4 FROM T;

-- DECIMAL is DECIMAL(18,0), and DECIMAL(p) is DECIMAL(p,0).
--#[decimal-00]
SELECT b::numeric(5) AS v1, b::decimal AS v2 FROM T;

--#[decimal-01]
SELECT b::decimal(5,7) AS v FROM T;

--#[decimal-02]
SELECT b::decimal(39,2) AS v FROM T;

--#[decimal-03]
SELECT b::decimal(0) AS v FROM T;

-- FLOAT is DOUBLE PRECISION; FLOAT(p) is REAL for p from 1 to 24.
--#[float-00]
SELECT b::float AS v1, b::float8 AS v2, b::float4 AS v3, b::float(24) AS v4, b::float(53) AS v5 FROM T;

--#[float-01]
SELECT b::float(0) AS v FROM T;

--#[boolean-00]
SELECT b::bool AS v FROM T;

--#[char-00]
SELECT b::character(5) AS v1, b::nchar AS v2, b::bpchar(5) AS v3, b::char(max) AS v4, b::national char(3) AS v5 FROM T;

-- BPCHAR is CHAR(256) for a column; its meaning without a length in a cast is not documented.
--#[char-01]
SELECT b::bpchar AS v FROM T;

--#[char-02]
SELECT b::char(5000) AS v FROM T;

--#[varchar-00]
SELECT b::char varying AS v1, b::nchar varying(10) AS v2, b::national character varying(10) AS v3, b::character varying(5) AS v4 FROM T;

--#[varchar-01]
SELECT b::varchar(0) AS v FROM T;

--#[varchar-02]
SELECT b::varchar(70000) AS v FROM T;

--#[varchar-03]
SELECT b::varchar(99999999999) AS v FROM T;

--#[datetime-00]
SELECT CAST(timestamp_1 AS DATE) AS v1, timestamp_1::time AS v2, timestamp_1::timestamptz AS v3 FROM T;

--#[unsupported-00]
SELECT b::binary varying AS v FROM T;

--#[unsupported-01]
SELECT b::varbyte AS v FROM T;

--#[unsupported-02]
SELECT b::time(3) AS v FROM T;
