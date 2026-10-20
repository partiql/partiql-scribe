--#[qualify]
[UNSUPPORTED{message="QUALIFY clause"}];

--#[ilike]
[UNSUPPORTED{message="ILIKE"}];

--#[unknown-function]
[UNSUPPORTED{message="function `MD5`"}];

--#[datepart-week]
[UNSUPPORTED{message="datepart `week`"}];

--#[group-by-position]
[UNSUPPORTED{message="GROUP BY column position"}];

--#[order-by-position]
[UNSUPPORTED{message="ORDER BY column position"}];

--#[no-from]
[UNSUPPORTED{message="SELECT without FROM"}];

--#[super-type]
[UNSUPPORTED{message="data type `super`"}];

--#[top-and-limit]
[UNSUPPORTED{message="TOP together with LIMIT"}];
