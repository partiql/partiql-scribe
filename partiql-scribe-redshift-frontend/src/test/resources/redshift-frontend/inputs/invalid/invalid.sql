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
