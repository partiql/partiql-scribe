--#[with-recursive-00]
-- Counting with UNION ALL
WITH RECURSIVE cnt(n) AS (
    SELECT 1 AS n FROM SIMPLE_T AS t WHERE t.b = 0
    UNION ALL
    SELECT cnt.n + 1 AS n FROM cnt WHERE cnt.n < 5
)
SELECT cnt.n FROM cnt;

--#[with-recursive-01]
-- Hierarchy traversal with a join
WITH RECURSIVE reports(id, name) AS (
    SELECT e.id, e.name FROM EMPLOYEE_T AS e WHERE e.manager_id IS NULL
    UNION ALL
    SELECT e.id, e.name FROM EMPLOYEE_T AS e INNER JOIN reports AS r ON e.manager_id = r.id
)
SELECT r.id, r.name FROM reports AS r;

--#[with-recursive-02]
-- UNION (DISTINCT) and a column list renaming the columns of each operand
WITH RECURSIVE managers(x) AS (
    SELECT e.id AS id FROM EMPLOYEE_T AS e WHERE e.id = 5
    UNION
    SELECT e.manager_id FROM EMPLOYEE_T AS e, managers AS m WHERE e.id = m.x
)
SELECT m.x FROM managers AS m;

--#[with-recursive-03]
-- A recursive element seeded from a non-recursive element that is defined later
WITH RECURSIVE
    tree(id, depth) AS (
        SELECT b.id, 0 FROM roots AS b
        UNION ALL
        SELECT e.id, t.depth + 1 FROM EMPLOYEE_T AS e INNER JOIN tree AS t ON e.manager_id = t.id
    ),
    roots(id) AS (
        SELECT e.id FROM EMPLOYEE_T AS e WHERE e.manager_id IS NULL
    )
SELECT t.id, t.depth FROM tree AS t;

--#[with-recursive-04]
-- A recursive element referenced twice in the query
WITH RECURSIVE cnt(n) AS (
    SELECT 1 FROM SIMPLE_T AS t WHERE t.b = 0
    UNION ALL
    SELECT cnt.n + 1 FROM cnt WHERE cnt.n < 3
)
SELECT c1.n AS n1, c2.n AS n2 FROM cnt AS c1 INNER JOIN cnt AS c2 ON c1.n = c2.n;
