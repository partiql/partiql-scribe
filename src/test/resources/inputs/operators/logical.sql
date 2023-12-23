--#[and-00]
SELECT a AND a FROM T;

--#[and-01]
SELECT a AND true FROM T;

--#[and-02]
SELECT false AND a FROM T;

--#[and-03]
SELECT a OR a FROM T;

--#[and-04]
SELECT a OR true FROM T;

--#[and-05]
SELECT false OR a FROM T;

--#[and-06]
SELECT NOT a FROM T;

--#[and-07]
SELECT NOT true FROM T;

--#[and-08]
SELECT NOT false FROM T;
