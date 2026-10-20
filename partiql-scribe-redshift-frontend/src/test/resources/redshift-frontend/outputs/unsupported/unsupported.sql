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

--#[extract-week]
[UNSUPPORTED{message="EXTRACT datepart `week`"}];

--#[extract-timezone-hour]
[UNSUPPORTED{message="EXTRACT datepart `timezone_hour`"}];

--#[extract-epoch]
[UNSUPPORTED{message="EXTRACT datepart `epoch`"}];

--#[timetz-zone-name]
[UNSUPPORTED{message="TIMETZ literal format '12:30:45 PST'"}];

--#[timestamp-with-offset]
[UNSUPPORTED{message="TIMESTAMP literal format '2026-10-19 12:30:45+08:00'"}];

--#[time-precision-7]
[UNSUPPORTED{message="TIME literal format '12:30:45.1234567'"}];
