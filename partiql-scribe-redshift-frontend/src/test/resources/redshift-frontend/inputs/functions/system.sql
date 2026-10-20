-- The system information functions return properties of the Redshift cluster and session.
--#[sys-00]
SELECT current_database() AS d FROM T;

--#[sys-01]
SELECT CURRENT_SCHEMA() AS s FROM T;

--#[sys-02]
SELECT VERSION() AS v FROM T;

--#[sys-03]
SELECT HAS_TABLE_PRIVILEGE('t', 'select') AS p FROM T;
