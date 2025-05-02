-- DATE
--#[datetime-00]
SELECT DATE '0001-02-03' FROM T;

--#[datetime-01]
SELECT DATE '2020-02-03' FROM T;

-- TIME
--#[datetime-02]
SELECT TIME '01:02:03' FROM T;

--#[datetime-03]
SELECT TIME '01:02:03.456' FROM T;

-- --#[datetime-04]
-- previous test includes timezone offset for TIME
-- SELECT TIME '01:02:03.456-00:00' FROM T;

--#[datetime-05]
SELECT TIME WITH TIME ZONE '01:02:03.456+00:00' FROM T;

--#[datetime-06]
SELECT TIME WITH TIME ZONE '01:02:03.456+00:30' FROM T;

--#[datetime-07]
SELECT TIME WITH TIME ZONE '01:02:03.456-00:30' FROM T;

-- TIMESTAMP
--#[datetime-08]
SELECT TIMESTAMP '0001-02-03 04:05:06.78' FROM T;

-- --#[datetime-09]
-- previous test includes timezone offset for TIMESTAMP
-- SELECT TIMESTAMP '0001-02-03 04:05:06.78-00:00' FROM T;

--#[datetime-10]
SELECT TIMESTAMP WITH TIME ZONE '0001-02-03 04:05:06.78+00:00' FROM T;

--#[datetime-11]
SELECT TIMESTAMP WITH TIME ZONE '0001-02-03 04:05:06.78+00:30' FROM T;

--#[datetime-12]
SELECT TIMESTAMP WITH TIME ZONE '0001-02-03 04:05:06.78-00:30' FROM T;

-- Ion Timestamp
-- not yet supported in Scribe-vNext
-- --#[datetime-13]
-- SELECT `2007-01-01T` FROM T;
--
-- --#[datetime-14]
-- SELECT `2007-02-23T12:14:33.079-08:00` FROM T;
