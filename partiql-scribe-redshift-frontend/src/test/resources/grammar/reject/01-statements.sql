-- Statements other than queries. The grammar covers SELECT (with WITH and set operations) only; every other statement
-- is a syntax error at its first token.

--#[insert] 1:1
INSERT INTO t (a, b) VALUES (1, 2);

--#[insert-select] 1:1
INSERT INTO t SELECT a, b FROM u;

--#[update] 1:1
UPDATE t SET a = 1 WHERE b = 2;

--#[delete] 1:1
DELETE FROM t WHERE a = 1;

--#[merge] 1:1
MERGE INTO t USING u ON t.id = u.id WHEN MATCHED THEN UPDATE SET a = u.a WHEN NOT MATCHED THEN INSERT VALUES (u.id, u.a);

--#[truncate] 1:1
TRUNCATE t;

--#[create-table] 1:1
CREATE TABLE t (a INT, b VARCHAR(10));

--#[create-table-as] 1:1
CREATE TABLE t2 AS SELECT a FROM t;

--#[create-view] 1:1
CREATE VIEW v AS SELECT a FROM t;

--#[create-materialized-view] 1:1
CREATE MATERIALIZED VIEW mv AS SELECT a FROM t;

--#[create-temp-table] 1:1
CREATE TEMP TABLE #tmp AS SELECT a FROM t;

--#[alter-table] 1:1
ALTER TABLE t ADD COLUMN c INT;

--#[drop-table] 1:1
DROP TABLE t;

--#[copy] 1:1
COPY t FROM 's3://bucket/key' IAM_ROLE 'arn:aws:iam::123456789012:role/r' FORMAT AS CSV;

--#[unload] 1:1
UNLOAD ('SELECT a FROM t') TO 's3://bucket/prefix' IAM_ROLE 'arn:aws:iam::123456789012:role/r';

--#[grant] 1:1
GRANT SELECT ON t TO u;

--#[begin] 1:1
BEGIN;

--#[commit] 1:1
COMMIT;

--#[set] 1:1
SET search_path TO myschema;

--#[show] 1:1
SHOW search_path;

--#[explain] 1:1
EXPLAIN SELECT a FROM t;

--#[analyze] 1:1
ANALYZE t;

--#[vacuum] 1:1
VACUUM t;

--#[call] 1:1
CALL my_procedure(1, 'a');

--#[declare-cursor] 1:1
DECLARE c CURSOR FOR SELECT a FROM t;

--#[fetch] 1:1
FETCH 10 FROM c;

--#[prepare] 1:1
PREPARE p (INT) AS SELECT a FROM t WHERE b = $1;

--#[execute] 1:1
EXECUTE p (1);

--#[create-function] 1:1
CREATE FUNCTION f (INT) RETURNS INT STABLE AS $$ SELECT $1 $$ LANGUAGE SQL;

--#[create-procedure] 1:1
CREATE PROCEDURE p () AS $$ BEGIN SELECT 1; END; $$ LANGUAGE plpgsql;

--#[values] 1:1
VALUES (1, 2), (3, 4);

--#[table] 1:1
TABLE t;

--#[with-insert] 1:29
WITH c AS (SELECT a FROM t) INSERT INTO u SELECT a FROM c;

--#[select-then-insert] 1:18
SELECT a FROM t; INSERT INTO u VALUES (1);
