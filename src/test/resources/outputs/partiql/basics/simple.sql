-- ------------------
--  Globals
-- ------------------

--#[global-00]
"default"."my_global";

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
lhs OR rhs;

--#[expr-01]
lhs AND rhs;

--#[expr-02]
NOT v;

--#[expr-03]
lhs < rhs;

--#[expr-04]
lhs <= rhs;

--#[expr-05]
lhs > rhs;

--#[expr-06]
lhs >= rhs;

--#[expr-07]
lhs = rhs;

--#[expr-08]
lhs != rhs;

--#[expr-09]
lhs <> rhs;

--#[expr-10]
lhs IS NOT rhs;

--#[expr-11]
lhs IN ( true );

--#[expr-12]
lhs NOT IN ( false );

--#[expr-13]
lhs IN collection;

--#[expr-14]
lhs NOT IN collection;

--#[expr-15]
lhs LIKE rhs;

--#[expr-16]
lhs NOT LIKE rhs;

--#[expr-17]
lhs LIKE rhs ESCAPE esc;

--#[expr-18]
v BETWEEN lo AND hi;

--#[expr-19]
v NOT BETWEEN lo AND hi;

--#[expr-20]
lhs & rhs;

--#[expr-21]
lhs || rhs;

--#[expr-22]
lhs + rhs;

--#[expr-23]
lhs - rhs;

--#[expr-24]
lhs % rhs;

--#[expr-25]
lhs * rhs;

--#[expr-26]
lhs / rhs;

--#[expr-27]
CURRENT_USER;

--#[expr-28]
CURRENT_DATE;

--#[expr-29]
'My name is ' || CURRENT_USER;

-- TO BE CONTINUED ....
