--#[select-00]
SELECT a, b, c FROM T;

--#[select-01]
SELECT * FROM T;

--#[select-02]
SELECT VALUE { 'a': a, 'b': b, 'c': c } FROM T;

--#[select-03]
SELECT VALUE a FROM T;

--#[select-04]
SELECT * FROM T AS t1, T AS t2;

--#[select-05]
SELECT t.d.* FROM T;

--#[select-06]
SELECT t, t.d.* FROM T;

--#[select-07]
SELECT t.d.*, t.d.* FROM T;

--#[select-08]
SELECT d.* FROM T;

--#[select-09]
SELECT t.* FROM T;

--#[select-10]
SELECT t.c || CURRENT_USER FROM T;

--#[select-11]
SELECT CURRENT_USER FROM T;

--#[select-12]
SELECT a FROM t;

--#[select-13]
SELECT VALUE {z: a} FROM T;

--#[select-14]
SELECT * FROM T WHERE T.b BETWEEN 0 AND 2;

--#[select-15]
SELECT * FROM T WHERE T.b NOT BETWEEN 0 AND 2;

--#[select-16]
SELECT * FROM T WHERE NOT T.b BETWEEN 0 AND 2;

--#[select-17]
SELECT * FROM T WHERE NOT T.b NOT BETWEEN 0 AND 2;

--#[select-18]
SELECT * FROM T WHERE T.b IN (0, 1, 2);

--#[select-19]
SELECT * FROM T WHERE T.b NOT IN (0, 1, 2);

--#[select-20]
SELECT * FROM T WHERE NOT T.b IN (0, 1, 2);

--#[select-21]
SELECT * FROM T WHERE NOT T.b NOT IN (0, 1, 2);

--#[select-22]
SELECT * FROM T WHERE T.b IS NULL;

--#[select-23]
SELECT * FROM T WHERE T.b IS NOT NULL;

--#[select-24]
SELECT * FROM T WHERE NOT T.b IS NULL;

--#[select-25]
SELECT * FROM T WHERE NOT T.b IS NOT NULL;

--#[select-26]
SELECT * FROM T WHERE T.c LIKE 'abc';

--#[select-27]
SELECT * FROM T WHERE T.c NOT LIKE 'abc';

--#[select-28]
SELECT * FROM T WHERE NOT T.c LIKE 'abc';

--#[select-29]
SELECT * FROM T WHERE NOT T.c NOT LIKE 'abc';

-- preserve aliases for A, "bB", and "C"; not for d or array index
--#[select-30]
SELECT a AS A, b AS "bB", "c" AS "C", d, "array"[1] FROM T;
