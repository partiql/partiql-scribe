-- Constructs that cannot be part of a view query, other than the statements in
-- 01-statements.sql: session tables, procedure syntax, parameters, SELECT INTO, VALUES, and the types and functions
-- the target cannot hold (VARBYTE, spatial, HyperLogLog, machine learning, system information).

--#[temporary-table] 1:15
SELECT a FROM #tmp;

--#[temporary-table-join] 1:22
SELECT a FROM t JOIN #tmp ON t.id = #tmp.id;

--#[dollar-quoted-string] 1:8
SELECT $$text$$ FROM t;

--#[dollar-quoted-string-tagged] 1:8
SELECT $tag$text$tag$ FROM t;

--#[positional-parameter] 1:27
SELECT a FROM t WHERE b = $1;

--#[select-into] 1:10
SELECT a INTO new_table FROM t;

--#[select-into-temp] 1:10
SELECT a INTO TEMP TABLE tmp FROM t;

--#[values] 1:1
VALUES (1, 2), (3, 4);

--#[type-varbyte] 1:18
SELECT CAST(a AS VARBYTE), CAST(a AS VARBINARY(10)), CAST(a AS BINARY VARYING(10)) FROM t;

--#[type-geometry] 1:18
SELECT CAST(a AS GEOMETRY), CAST(a AS GEOGRAPHY) FROM t;

--#[type-hllsketch] 1:18
SELECT CAST(a AS HLLSKETCH) FROM t;

--#[spatial-functions] 1:17
SELECT ST_AsText(g), ST_Distance(g, h), ST_Point(1, 2) FROM t;

--#[hll-functions] 1:11
SELECT HLL(a), HLL_CARDINALITY(s), HLL_CREATE_SKETCH(a) FROM t;

--#[varbyte-functions] 1:16
SELECT FROM_HEX(s), TO_HEX(a), FROM_VARBYTE(v, 'utf8') FROM t;

--#[machine-learning] 1:21
SELECT EXPLAIN_MODEL('my_model') FROM t;

--#[system-information] 1:24
SELECT CURRENT_DATABASE(), CURRENT_SCHEMA(), CURRENT_NAMESPACE, VERSION(), PG_BACKEND_PID() FROM t;

--#[system-administration] 1:29
SELECT CHANGE_QUERY_PRIORITY(1, 'high') FROM t;

--#[system-table-function] 1:27
SELECT HAS_TABLE_PRIVILEGE('u', 't', 'select') FROM t;

--#[prepare] 1:1
PREPARE p (INT) AS SELECT a FROM t WHERE b = $1;

--#[execute] 1:1
EXECUTE p (1);
