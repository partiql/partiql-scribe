--#[in-00]
SELECT * FROM T WHERE b IN (1, 2);

--#[in-01]
SELECT * FROM T WHERE b IN [1, 2];

--#[in-02]
SELECT * FROM T WHERE b IN << 1, 2 >>;

--#[in-03]
SELECT * FROM T WHERE b IN SEXP (1, 2);

-- #[in-04]
-- SELECT * FROM T WHERE b IN VALUES (1, 2);
