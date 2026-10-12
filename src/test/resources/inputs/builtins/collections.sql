-- Collection functions (size, cardinality, exists) on an array
--#[collections-01]
-- Size of an array column
SELECT size(t."array") AS v FROM T AS t;

--#[collections-02]
-- Cardinality of an array column
SELECT cardinality(t."array") AS v FROM T AS t;

--#[collections-03]
-- Exists on an array column
SELECT exists(t."array") AS v FROM T AS t;
