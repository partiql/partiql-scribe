--#[like-00]
'x' LIKE 'y';

-- #[like-01]
-- 'x' NOT LIKE 'y';

--#[like-02]
'x' LIKE '%y';

--#[like-03]
'x' LIKE 'y%';

--#[like-04]
'x' LIKE '%y%';

--#[like-05]
SELECT t.c AS c FROM T AS t WHERE t.c LIKE 'y';

-- #[like-06]
-- SELECT t.c AS c FROM T AS t WHERE t.c NOT LIKE 'y';

--#[like-07]
SELECT t.c AS c FROM T AS t WHERE t.c LIKE '%y';

--#[like-08]
SELECT t.c AS c FROM T AS t WHERE t.c LIKE 'y%';

--#[like-09]
SELECT t.c AS c FROM T AS t WHERE t.c LIKE '%y%';
