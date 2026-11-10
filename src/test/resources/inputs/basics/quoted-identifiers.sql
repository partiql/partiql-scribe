-- A quote character inside a quoted identifier must be escaped in the target dialect.

--#[quoted-identifiers-00]
SELECT t.c AS "a`b" FROM T AS t;

--#[quoted-identifiers-01]
SELECT t.c AS "a""b" FROM T AS t;

--#[quoted-identifiers-02]
SELECT t.x['a`b'] AS v FROM T AS t;

--#[quoted-identifiers-03]
SELECT t.x['a"b'] AS v FROM T AS t;

--#[quoted-identifiers-04]
SELECT v."a`b" FROM (SELECT t.c AS "a`b" FROM T AS t) AS v;
