-- The output columns have the names of the first query: the items of the later queries are aliased with them.
--#[union-all]
SELECT b AS x FROM T UNION ALL SELECT col_int32 FROM T_ALL_TYPES;

--#[union-all-names]
SELECT c FROM T UNION ALL SELECT col_string FROM T_ALL_TYPES;

--#[union]
SELECT a FROM T UNION SELECT a FROM SIMPLE_T;

--#[union-distinct]
SELECT a FROM T UNION DISTINCT SELECT a FROM SIMPLE_T;

-- INTERSECT binds more tightly than UNION and EXCEPT.
--#[intersect-precedence]
SELECT b FROM T UNION SELECT b FROM SIMPLE_T INTERSECT SELECT col_int32 FROM T_ALL_TYPES;

-- UNION and EXCEPT are left-associative.
--#[union-except-left-associative]
SELECT b FROM T UNION SELECT b FROM SIMPLE_T EXCEPT SELECT col_int32 FROM T_ALL_TYPES;

-- MINUS is EXCEPT. EXCEPT returns the distinct rows of the left query.
--#[minus]
SELECT b FROM T MINUS SELECT b FROM SIMPLE_T;

--#[except]
SELECT b FROM T EXCEPT SELECT b FROM SIMPLE_T;

--#[intersect-all]
SELECT b FROM T INTERSECT ALL SELECT b FROM SIMPLE_T;

--#[except-all]
SELECT b FROM T EXCEPT ALL SELECT b FROM SIMPLE_T;

--#[set-operation-column-count]
SELECT b, c FROM T UNION SELECT b FROM SIMPLE_T;

-- ORDER BY, LIMIT, and OFFSET at the end apply to the result of the set operation.
--#[set-operation-order-by-position]
SELECT b FROM T UNION ALL SELECT b FROM SIMPLE_T ORDER BY 1 LIMIT 5;

--#[set-operation-order-by-name]
SELECT b AS x FROM T UNION SELECT b FROM SIMPLE_T ORDER BY x DESC LIMIT 3 OFFSET 1;

--#[set-operation-order-by-expression]
SELECT b FROM T UNION SELECT b FROM SIMPLE_T ORDER BY b + 1;

--#[set-operation-order-by-unknown]
SELECT b FROM T UNION SELECT b FROM SIMPLE_T ORDER BY c;

-- A parenthesized query has its own ORDER BY and LIMIT.
--#[set-operation-parenthesized]
(SELECT b AS x FROM T ORDER BY b DESC LIMIT 1) UNION ALL (SELECT b FROM SIMPLE_T ORDER BY b LIMIT 1);

--#[set-operation-nested]
SELECT b FROM T UNION ALL (SELECT b FROM SIMPLE_T UNION SELECT col_int32 FROM T_ALL_TYPES);

--#[set-operation-top]
SELECT TOP 1 b FROM T UNION SELECT b FROM SIMPLE_T;

-- When names of the first query repeat, the set operation is in a subquery with positional column names.
--#[set-operation-repeated-names]
SELECT t.b, s.b FROM T t JOIN SIMPLE_T s ON t.c = s.c UNION SELECT t.b, t.b FROM T t;

--#[set-operation-repeated-names-order-by]
SELECT t.b, s.b FROM T t JOIN SIMPLE_T s ON t.c = s.c UNION ALL SELECT t.b, t.b FROM T t ORDER BY 2 DESC;

--#[set-operation-star]
SELECT * FROM SIMPLE_T UNION SELECT a, b, c FROM SIMPLE_T;

-- PartiQL does not plan a set operation over SUPER values.
--#[set-operation-super]
SELECT x FROM T UNION SELECT x FROM T;
