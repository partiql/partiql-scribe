--#[split-00]
split(str, str);

--#[split-01]
split(str, str)[0];

--#[split-02]
split(str, str)[0];

--#[split-sfw-00]
SELECT split(v, '.') AS chars FROM T;

--#[split-sfw-01]
SELECT split(v, '.')[0] AS chars FROM T;

--#[split-sfw-02]
SELECT split(v, '.')[0 + 1] AS chars FROM T;
