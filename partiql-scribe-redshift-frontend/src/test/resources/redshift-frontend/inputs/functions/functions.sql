--#[nvl-00]
SELECT NVL(c, 'none') AS v FROM T;

--#[coalesce-00]
SELECT COALESCE(c, z, 'none') AS v FROM T;

--#[nullif-00]
SELECT NULLIF(b, 0) AS v FROM T;

--#[len-00]
SELECT LEN(c) AS v FROM T;

--#[upper-00]
SELECT UPPER(c) AS v1, lower(z) AS v2 FROM T;

--#[dateadd-00]
SELECT DATEADD(day, 5, timestamp_1) AS v FROM T;

--#[dateadd-01]
SELECT DATEADD(mon, 1, timestamp_1) AS v FROM T;

--#[datediff-00]
SELECT DATEDIFF(hour, timestamp_1, timestamp_2) AS v FROM T;

--#[getdate-00]
SELECT GETDATE() AS v FROM T;

--#[sysdate-00]
SELECT SYSDATE AS v FROM T;

--#[current-date-00]
SELECT CURRENT_DATE AS v FROM T;
