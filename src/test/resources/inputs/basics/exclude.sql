--#[exclude-00]
SELECT * EXCLUDE a, b, c FROM T;

--#[exclude-01]
SELECT * EXCLUDE d.e FROM T;

--#[exclude-02]
SELECT * EXCLUDE a, b FROM T WHERE NOT(d.e = 123);
