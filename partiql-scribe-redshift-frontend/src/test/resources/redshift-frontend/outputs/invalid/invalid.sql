--#[syntax-error]
[UNSUPPORTED_OPERATION{message="Syntax error: mismatched input ';'"}];

--#[syntax-error-no-from]
[UNSUPPORTED_OPERATION{message="Syntax error: mismatched input ';'"}];

--#[syntax-error-unknown-function]
[UNSUPPORTED_OPERATION{message="Syntax error: mismatched input '('"}];

--#[syntax-error-two-statements]
[UNSUPPORTED_OPERATION{message="Syntax error: mismatched input 'SELECT'"}];

--#[unknown-column]
[PLANNER{message="VAR_REF_NOT_FOUND"}];

--#[unknown-table]
[PLANNER{message="VAR_REF_NOT_FOUND"}];

--#[unknown-qualifier]
[PLANNER{message="VAR_REF_NOT_FOUND"}];

--#[ambiguous-column]
[PLANNER{message="VAR_REF_NOT_FOUND"}];

--#[ordinal-out-of-range]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: ordinal 2 is out of range of the select list"}];

--#[top-and-limit]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: TOP and LIMIT in the same query"}];

--#[alias-not-visible-in-outer-query]
[PLANNER{message="VAR_REF_NOT_FOUND"}];

--#[nullif-one-argument]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: NULLIF takes 2 arguments, not 1"}];

--#[nvl2-two-arguments]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: NVL2 takes 3 arguments, not 2"}];

--#[decode-two-arguments]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: DECODE takes at least 3 arguments, not 2"}];

--#[identifier-longer-than-127-bytes]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: identifier longer than 127 bytes"}];

--#[upper-two-arguments]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: UPPER takes 1 argument, not 2"}];

--#[concat-one-argument]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: CONCAT takes 2 arguments, not 1"}];

--#[concat-three-arguments]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: CONCAT takes 2 arguments, not 3"}];

--#[power-one-argument]
[UNSUPPORTED_OPERATION{message="Invalid Redshift query: POWER takes 2 arguments, not 1"}];
