--#[eq-00]
SELECT a = true FROM T;

--#[eq-01]
SELECT b = 1 FROM T;

--#[eq-02]
SELECT c = 'a' FROM T;

--#[eq-03]
SELECT c = z FROM T;

--#[eq-04]
SELECT c = d.e FROM T;

--#[eq-05]
SELECT c = x FROM T;

--#[neq-00]
SELECT a != true FROM T;

--#[neq-01]
SELECT b != 1 FROM T;

--#[neq-02]
SELECT c != 'a' FROM T;

--#[neq-03]
SELECT c != z FROM T;

--#[neq-04]
SELECT c != d.e FROM T;

--#[neq-05]
SELECT c != x FROM T;

--#[lt-00]
SELECT b < 1 FROM T;

--#[lt-01]
SELECT b < x FROM T;

--#[lte-00]
SELECT b <= 1 FROM T;

--#[lte-01]
SELECT b <= x FROM T;

--#[gt-00]
SELECT b > 1 FROM T;

--#[gt-01]
SELECT b > x FROM T;

--#[gte-00]
SELECT b >= 1 FROM T;

--#[gte-01]
SELECT b >= x FROM T;
