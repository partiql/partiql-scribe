-- The string functions: UPPER, LOWER, and CONCAT, and the concatenation operator `||`.

--#[upper-lower]
SELECT UPPER(c), LOWER(c) FROM SIMPLE_T;

--#[upper-lower-of-literal]
SELECT UPPER('abc'), LOWER('ABC') FROM SIMPLE_T;

--#[upper-in-where]
SELECT a FROM SIMPLE_T WHERE UPPER(c) = 'X';

--#[upper-of-conditional]
SELECT UPPER(NVL(c, 'none')), NVL(LOWER(c), 'none') FROM SIMPLE_T;

--#[upper-in-group-by]
SELECT UPPER(c) AS u, COUNT(*) FROM SIMPLE_T GROUP BY UPPER(c);

--#[lower-of-upper]
SELECT LOWER(UPPER(c)) FROM SIMPLE_T;

--#[concat]
SELECT CONCAT(c, 'x') FROM SIMPLE_T;

-- Redshift's own example: CONCAT nests to concatenate more than two strings.
--#[concat-nested]
SELECT CONCAT('Thursday, ', CONCAT('December 25, ', '2008')) FROM SIMPLE_T;

--#[concat-and-operator]
SELECT CONCAT(c, '-') || c, c || CONCAT('-', c) FROM SIMPLE_T;

-- A numeric literal operand converts to its digits: an integer loses its leading zeros, a decimal keeps its scale.
--#[concat-numeric-literal]
SELECT c || 1, 'x' || 007, CONCAT('v', 1.50), c || .5 || 1. FROM SIMPLE_T;

--#[concat-signed-literal]
SELECT c || -1, c || +2, c || -0.50, CONCAT(- -3, c) FROM SIMPLE_T;

--#[concat-null]
SELECT c || NULL, CONCAT(NULL, c) FROM SIMPLE_T;

--#[concat-in-like]
SELECT a FROM SIMPLE_T WHERE c LIKE CONCAT(c, '%');

--#[string-functions-mixed-case-names]
SELECT Concat(Upper(C), lower(c)) FROM simple_t;

-- CONCAT's arguments are the operands of `||`, which prints a subquery with its parentheses, so a subquery is allowed.
--#[concat-subquery-argument]
SELECT CONCAT(c, (SELECT MAX(c) FROM T)), (SELECT MAX(c) FROM T) || c FROM SIMPLE_T;

--#[string-functions-null]
SELECT UPPER(NULL), LOWER(NULL::varchar), NULL || NULL, CONCAT(NULL, NULL) FROM SIMPLE_T;
