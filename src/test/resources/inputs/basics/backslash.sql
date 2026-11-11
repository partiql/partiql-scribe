-- PartiQL string literals have no backslash escapes, and LIKE without ESCAPE has no escape character.

--#[backslash-00]
SELECT 'C:\temp\n' AS v FROM T;

--#[backslash-01]
SELECT '\' AS v FROM T;

--#[backslash-02]
SELECT 'it''s \' AS v FROM T;

--#[backslash-03]
SELECT t.c AS c FROM T AS t WHERE t.c LIKE 'a\_%';

--#[backslash-04]
SELECT t.c AS c FROM T AS t WHERE t.c LIKE '%\';

--#[backslash-05]
SELECT t.c AS c FROM T AS t WHERE t.c LIKE t.z;

--#[backslash-06]
SELECT t.c AS c FROM T AS t WHERE NOT t.c LIKE '%\%';

--#[backslash-07]
SELECT t.c AS c FROM T AS t WHERE t.c LIKE 'a\!_%' ESCAPE '!';

--#[backslash-08]
SELECT t.c AS c FROM T AS t WHERE t.c LIKE 'a\_%' ESCAPE '\';

--#[backslash-09]
SELECT REPLACE(t.c, '\', '/') AS v FROM T AS t;

--#[backslash-10]
SELECT TRIM(BOTH '\' FROM t.c) AS v FROM T AS t;

--#[backslash-11]
SELECT POSITION('\' IN t.c) AS v FROM T AS t;

--#[backslash-12]
SELECT {'k\': t.c} AS v FROM T AS t;

--#[backslash-13]
SELECT 'a\''' || t.c AS v FROM T AS t;

--#[backslash-14]
SELECT t.c AS c FROM T AS t WHERE t.c LIKE 'a\''%';

--#[backslash-15]
SELECT t.c AS c FROM T AS t WHERE NOT t.c LIKE t.z;

--#[backslash-16]
SELECT t.c AS c FROM T AS t WHERE t.c IN ('a\', 'b');

--#[backslash-17]
SELECT t.c AS c FROM T AS t WHERE t.c LIKE t.d.e;

--#[backslash-18]
SELECT t.col_string AS c FROM T_ALL_TYPES AS t WHERE t.col_string LIKE t.col_list_string[1];

--#[backslash-19]
SELECT t.c AS c FROM T AS t WHERE t.c LIKE NULL;

--#[backslash-20]
SELECT ['a\', t.c] AS v FROM T AS t;

--#[backslash-21]
SELECT t.c AS c FROM T AS t WHERE t.c LIKE t.x.y;
