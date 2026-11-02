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
