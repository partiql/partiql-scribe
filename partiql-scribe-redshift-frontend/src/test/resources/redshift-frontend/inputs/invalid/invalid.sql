--#[syntax-00]
SELECT FROM T;

--#[syntax-01]
SELECT a FROM T WHERE;

--#[syntax-02]
SELECT a FROM T @;

--#[unknown-table]
SELECT a FROM nope;

--#[unknown-column]
SELECT nope FROM T;

-- TOP and LIMIT can't be used in the same query.
--#[top-and-limit]
SELECT TOP 5 b FROM T LIMIT 3;

-- Directly after the FROM clause, Redshift reads QUALIFY as the alias of the table.
--#[qualify-without-alias]
SELECT b FROM T QUALIFY b > 1;
