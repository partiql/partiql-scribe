--#[cast-00]
CAST('1' AS INT);

--#[cast-01]
CAST('1' AS INT);

--#[cast-02]
CAST('1' AS BIGINT);

--#[cast-03]
CAST('1' AS BIGINT);

--#[cast-04]
CAST(1 AS DECIMAL);

-- TODO PartiQL planner does not reify type casts in PartiQL 0.14
-- #[cast-05]
-- CAST(1 AS DECIMAL(1, 0));

-- TODO PartiQL planner does not reify type casts in PartiQL 0.14
-- #[cast-06]
-- CAST(1 AS DECIMAL(1, 1));
