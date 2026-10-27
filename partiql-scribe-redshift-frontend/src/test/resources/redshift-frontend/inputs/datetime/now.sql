-- GETDATE() is a TIMESTAMP without fractional seconds.
--#[getdate-00]
SELECT GETDATE() AS v FROM T;

--#[sysdate-00]
SELECT SYSDATE AS v FROM T;

-- The cast keeps the INTEGER type of EXTRACT for a TIMESTAMP value.
--#[sysdate-01]
SELECT EXTRACT(hour FROM SYSDATE) AS v FROM T;

--#[current-date-00]
SELECT CURRENT_DATE AS v FROM T;

--#[current-date-01]
SELECT CURRENT_DATE - 1 AS v FROM T;

-- Deprecated leader node-only functions.
--#[current-timestamp-00]
SELECT CURRENT_TIMESTAMP AS v FROM T;

--#[current-time-00]
SELECT CURRENT_TIME AS v FROM T;

--#[localtimestamp-00]
SELECT LOCALTIMESTAMP AS v FROM T;

--#[localtime-00]
SELECT LOCALTIME AS v FROM T;

--#[now-00]
SELECT NOW() AS v FROM T;

-- TIMEOFDAY() returns a formatted string.
--#[timeofday-00]
SELECT TIMEOFDAY() AS v FROM T;
