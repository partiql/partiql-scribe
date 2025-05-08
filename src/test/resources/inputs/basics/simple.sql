-- ------------------
--  Globals
-- ------------------

--#[global-00]
my_global;

-- ------------------
--  Literals
-- ------------------

--#[sanity-lit-00]
true;

--#[sanity-lit-01]
1;

--#[sanity-lit-02]
1.0;

--#[sanity-lit-03]
'hello';

--#[sanity-lit-04]
[ 'a', 'b', 'c' ];

--#[sanity-lit-05]
<< 'a', 'b', 'c' >>;

--#[sanity-lit-06]
{ 'a': 1, 'b': 2, 'c': 3 };

-- ------------------
--  Basic Expressions
-- ------------------

--#[expr-00]
t1 OR t1;

--#[expr-01]
t1 AND t1;

--#[expr-02]
NOT t1;

--#[expr-03]
t1 < t1;

--#[expr-04]
t1 <= t1;

--#[expr-05]
t1 > t1;

--#[expr-06]
t1 >= t1;

--#[expr-07]
t1 = t1;

--#[expr-08]
t1 != t1;

--#[expr-09]
t1 <> t1;

--#[expr-10]
t1 IS NULL;

--#[expr-11]
t1 IS NOT NULL;

--#[expr-12]
t1 IS MISSING;

--#[expr-13]
t1 IS NOT MISSING;

--#[expr-14]
t1 IS INT2;

--#[expr-15]
t1 IS NOT INT2;

--#[expr-16]
t1 IS INT4;

--#[expr-17]
t1 IS NOT INT4;

--#[expr-18]
t1 IS INT8;

--#[expr-19]
t1 IS NOT INT8;

--#[expr-20]
t1 IS INT;

--#[expr-21]
t1 IS NOT INT;

--#[expr-22]
t1 IS DECIMAL;

--#[expr-23]
t1 IS NOT DECIMAL;

--#[expr-24]
t1 IS FLOAT;

--#[expr-25]
t1 IS NOT FLOAT;

--#[expr-26]
t1 IS BOOL;

--#[expr-27]
t1 IS NOT BOOL;

-- `IS SYMBOL` is not support in PLK 1.x
-- --#[expr-28]
-- t1 IS SYMBOL;
--
-- --#[expr-29]
-- t1 IS NOT SYMBOL;

--#[expr-30]
t1 IS DATE;

--#[expr-31]
t1 IS NOT DATE;

--#[expr-32]
t1 IS TIME;

--#[expr-33]
t1 IS NOT TIME;

--#[expr-34]
t1 IS TIMESTAMP;

--#[expr-35]
t1 IS NOT TIMESTAMP;

--#[expr-36]
t1 IS STRING;

--#[expr-37]
t1 IS NOT STRING;

--#[expr-38]
t1 IS CLOB;

--#[expr-39]
t1 IS NOT CLOB;

--#[expr-40]
t1 IS BLOB;

--#[expr-41]
t1 IS NOT BLOB;

--#[expr-42]
t1 IS LIST;

--#[expr-43]
t1 IS NOT LIST;

-- SEXP data type not in PLK 1.x
-- --#[expr-44]
-- t1 IS SEXP;
--
-- --#[expr-45]
-- t1 IS NOT SEXP;

--#[expr-46]
t1 IS STRUCT;

--#[expr-47]
t1 IS NOT STRUCT;

--#[expr-48]
t1 IS BAG;

--#[expr-49]
t1 IS NOT BAG;

--#[expr-50]
t1 IN ( true );

--#[expr-51]
t1 NOT IN ( false );

--#[expr-52]
t1 IN t1;

--#[expr-53]
t1 NOT IN t1;

--#[expr-54]
t1 LIKE t1;

--#[expr-55]
t1 NOT LIKE t1;

--#[expr-56]
t1 LIKE t1 ESCAPE t1;

--#[expr-57]
t1 BETWEEN t1 AND t1;

--#[expr-58]
t1 NOT BETWEEN t1 AND t1;

--#[expr-59]
t1 || t1;

--#[expr-60]
t1 & t1;

--#[expr-61]
t1 + t1;

--#[expr-62]
t1 - t1;

--#[expr-63]
t1 % t1;

--#[expr-64]
t1 * t1;

--#[expr-65]
t1 / t1;

--#[expr-66]
CURRENT_USER;

--#[expr-67]
CURRENT_DATE;

--#[expr-68]
'My name is ' || CURRENT_USER;

-- TO BE CONTINUED ....
