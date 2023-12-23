--#[in-00]
SELECT * FROM T WHERE b IN (1, 2);

--#[in-01]
SELECT * FROM T WHERE b IN [1, 2];

--#[in-02]
SELECT * FROM T WHERE b IN << 1, 2 >>;
