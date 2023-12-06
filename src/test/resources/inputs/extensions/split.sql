--#[split-00]
split(v, d);

--#[split-01]
split(v, d)[n];

--#[split-02]
split(v, d)[n];

--#[split-sfw-00]
SELECT split(v, '.') AS chars FROM T;

--#[split-sfw-01]
SELECT split(v, '.')[0] AS chars FROM T;

--#[split-sfw-02]
SELECT split(v, '.')[0 + 1] AS chars FROM T;
