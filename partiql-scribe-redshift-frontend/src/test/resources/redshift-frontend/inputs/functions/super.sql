--#[super-array-00]
SELECT ARRAY(1, b, NULL, c, 'abc') AS v1, ARRAY() AS v2, ARRAY(ARRAY(1, 2), ARRAY(3)) AS v3 FROM T;

-- An element of type SUPER is NULL, not MISSING, when it is missing; an element that is a COALESCE is not wrapped again.
--#[super-array-01]
SELECT ARRAY(x, b) AS v1, ARRAY(COALESCE(x)) AS v2 FROM T;

-- Redshift does not convert datetime values to SUPER.
--#[super-array-02]
SELECT ARRAY(timestamp_1) AS v FROM T;

--#[super-array-03]
SELECT ARRAY(col_char) AS v FROM T_ALL_TYPES;

--#[super-object-00]
SELECT OBJECT('a', 1, 'b', 'x', 'c', ARRAY(1, 2), 'd', 10 + 5) AS v1, OBJECT() AS v2 FROM T;

-- Whether a Redshift object keeps an attribute whose value is NULL is not documented.
--#[super-object-01]
SELECT OBJECT('a', 1, 'b', c) AS v FROM T;

--#[super-object-02]
SELECT OBJECT(c, 1) AS v FROM T;

--#[super-object-03]
SELECT OBJECT('a', 1, 'a', 2) AS v FROM T;

-- PartiQL CARDINALITY also counts the attributes of an object.
--#[super-get-array-length-00]
SELECT GET_ARRAY_LENGTH(t.x) AS v1, GET_ARRAY_LENGTH(t."array") AS v2 FROM T t;

--#[super-get-number-attributes-00]
SELECT GET_NUMBER_ATTRIBUTES(t.x) AS v FROM T t;

-- With null_match TRUE (the default), NULL values match each other.
--#[super-array-contains-00]
SELECT ARRAY_CONTAINS(t.x, 'red') AS v FROM T t;

--#[super-array-contains-01]
SELECT ARRAY_CONTAINS(t."array", t.b) AS v FROM T t;

--#[super-array-contains-02]
SELECT ARRAY_CONTAINS(t.x, c, FALSE) AS v FROM T t;

--#[super-array-contains-03]
SELECT b FROM T t WHERE ARRAY_CONTAINS(t.x, 'red');

--#[super-array-contains-04]
SELECT ARRAY_CONTAINS(t.x, c, a) AS v FROM T t;

--#[super-arrays-overlap-00]
SELECT ARRAYS_OVERLAP(t.x, t."array") AS v FROM T t;

-- Redshift does not guarantee the order of the elements; the PartiQL result is sorted.
--#[super-array-distinct-00]
SELECT ARRAY_DISTINCT(t.x) AS v FROM T t;

--#[super-array-sort-00]
SELECT ARRAY_SORT(t.x) AS v1, ARRAY_SORT(t.x, FALSE) AS v2, ARRAY_SORT(t.x, FALSE, FALSE) AS v3 FROM T t;

--#[super-array-union-00]
SELECT ARRAY_UNION(t.x, t."array") AS v FROM T t;

--#[super-array-intersection-00]
SELECT ARRAY_INTERSECTION(t.x, t."array", TRUE) AS v FROM T t;

-- Without the distinct argument, duplicate elements are kept, which needs element positions.
--#[super-array-intersection-01]
SELECT ARRAY_INTERSECTION(t.x, t."array") AS v FROM T t;

-- The PartiQL type predicates are NULL for NULL, and the Redshift type tests are false.
--#[super-is-00]
SELECT IS_ARRAY(t.x) AS v1, IS_OBJECT(t.x) AS v2, IS_SCALAR(t.x) AS v3, IS_BOOLEAN(t.x) AS v4 FROM T t;

-- IS_FLOAT is true for REAL and DOUBLE PRECISION values; IS_DECIMAL is true for all numbers that are not floating point.
--#[super-is-01]
SELECT IS_FLOAT(t.x) AS v1, IS_DECIMAL(t.x) AS v2 FROM T t;

--#[super-is-integer-00]
SELECT IS_INTEGER(b) AS v1, IS_SMALLINT(b) AS v2, IS_BIGINT(b) AS v3 FROM T;

--#[super-is-integer-01]
SELECT IS_INTEGER(col_int64) AS v1, IS_INTEGER(col_string) AS v2 FROM T_ALL_TYPES;

-- A DECIMAL value has scale 0 only for a DECIMAL(p, 0) type.
--#[super-is-integer-02]
SELECT IS_INTEGER(de) AS v1, IS_BIGINT(de) AS v2 FROM T_DECIMALS;

--#[super-is-integer-03]
SELECT IS_INTEGER(t.foo.keep) AS v FROM datatypes.T_DECIMAL_5_2 t;

-- The PartiQL test for a DECIMAL value of scale 0 is not printed correctly.
--#[super-is-integer-04]
SELECT IS_INTEGER(t.x) AS v FROM T t;

--#[super-is-varchar-00]
SELECT IS_VARCHAR(t.x) AS v FROM T t;

--#[super-is-varchar-01]
SELECT IS_VARCHAR(col_char) AS v1, IS_VARCHAR(col_int32) AS v2 FROM T_ALL_TYPES;

-- IS_CHAR is true for strings of ASCII characters, which have one byte each in UTF-8.
--#[super-is-char-00]
SELECT IS_CHAR(t.x) AS v1, IS_CHAR(t.b) AS v2 FROM T t;

--#[super-unsupported-00]
SELECT JSON_TYPEOF(t.x) AS v FROM T t;

--#[super-unsupported-01]
SELECT SPLIT_TO_ARRAY(c, '|') AS v FROM T;

--#[super-unsupported-02]
SELECT JSON_PARSE(c) AS v FROM T;

--#[super-unsupported-03]
SELECT JSON_EXTRACT_PATH_TEXT(c, 'a', 'b') AS v FROM T;

--#[super-unsupported-04]
SELECT ARRAY_CONCAT(t.x, t."array") AS v FROM T t;

--#[super-unsupported-05]
SELECT SUBARRAY(t.x, 1, 2) AS v FROM T t;

--#[super-unsupported-06]
SELECT SIZE(t.x) AS v FROM T t;
