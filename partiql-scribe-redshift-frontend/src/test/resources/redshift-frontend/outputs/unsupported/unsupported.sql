--#[upper-of-number]
[PLANNER{message="FUNCTION_TYPE_MISMATCH"}];

--#[concat-of-number-column]
[PLANNER{message="FUNCTION_TYPE_MISMATCH"}];

--#[concat-of-power]
[PLANNER{message="FUNCTION_TYPE_MISMATCH"}];

--#[power-of-string]
[PLANNER{message="FUNCTION_TYPE_MISMATCH"}];

--#[bitwise-and-of-string]
[PLANNER{message="FUNCTION_TYPE_MISMATCH"}];

--#[subquery-as-caret-operand]
[UNSUPPORTED_OPERATION{message="subquery as a function argument"}];

--#[subquery-as-upper-argument]
[UNSUPPORTED_OPERATION{message="subquery as a function argument"}];

--#[subquery-as-concat-argument]
[UNSUPPORTED_OPERATION{message="subquery as a function argument"}];

--#[subquery-as-power-argument]
[UNSUPPORTED_OPERATION{message="subquery as a function argument"}];

--#[function-dateadd]
[UNSUPPORTED_OPERATION{message="function DATEADD is not translated yet"}];

--#[function-datediff]
[UNSUPPORTED_OPERATION{message="function DATEDIFF is not translated yet"}];

--#[function-getdate]
[UNSUPPORTED_OPERATION{message="function GETDATE is not translated yet"}];

--#[function-sysdate]
[UNSUPPORTED_OPERATION{message="function SYSDATE is not translated yet"}];

--#[function-current-date]
[UNSUPPORTED_OPERATION{message="function CURRENT_DATE is not translated yet"}];

--#[function-extract-epoch]
[UNSUPPORTED_OPERATION{message="function EXTRACT is not translated yet"}];

--#[interval-literal]
[UNSUPPORTED_OPERATION{message="INTERVAL literal"}];

--#[from-column-aliases]
[UNSUPPORTED_OPERATION{message="column aliases of a FROM item"}];

--#[type-mismatch-arith]
[PLANNER{message="FUNCTION_TYPE_MISMATCH"}];

--#[subquery-as-nvl-argument]
[UNSUPPORTED_OPERATION{message="subquery as a function argument"}];

--#[subquery-as-nullif-argument]
[UNSUPPORTED_OPERATION{message="subquery as a function argument"}];

--#[subquery-as-greatest-argument]
[UNSUPPORTED_OPERATION{message="subquery as a function argument"}];

--#[subquery-as-decode-search-value]
[UNSUPPORTED_OPERATION{message="subquery as a function argument"}];

--#[subquery-as-parenthesized-argument]
[UNSUPPORTED_OPERATION{message="subquery as a function argument"}];

--#[subquery-as-aggregate-argument]
[UNSUPPORTED_OPERATION{message="subquery as a function argument"}];
--#[subquery-as-coalesce-argument]
[UNSUPPORTED_OPERATION{message="subquery as a function argument"}];

--#[subquery-as-nvl2-argument]
[UNSUPPORTED_OPERATION{message="subquery as a function argument"}];

--#[subquery-as-least-argument]
[UNSUPPORTED_OPERATION{message="subquery as a function argument"}];

--#[is-null-of-boolean-connective]
[UNSUPPORTED_OPERATION{message="a comparison or a boolean connective as the operand of an IS test"}];

--#[isnull-of-boolean-connective]
[UNSUPPORTED_OPERATION{message="a comparison or a boolean connective as the operand of an IS test"}];

--#[notnull-of-comparison]
[UNSUPPORTED_OPERATION{message="a comparison or a boolean connective as the operand of an IS test"}];

--#[is-true-of-boolean-connective]
[UNSUPPORTED_OPERATION{message="a comparison or a boolean connective as the operand of an IS test"}];

--#[is-null-of-between]
[UNSUPPORTED_OPERATION{message="a comparison or a boolean connective as the operand of an IS test"}];

--#[nvl2-of-boolean-connective]
[UNSUPPORTED_OPERATION{message="a comparison or a boolean connective as the operand of an IS test"}];

--#[nvl2-of-not]
[UNSUPPORTED_OPERATION{message="a comparison or a boolean connective as the operand of an IS test"}];

--#[nvl2-of-comparison]
[UNSUPPORTED_OPERATION{message="a comparison or a boolean connective as the operand of an IS test"}];

--#[decode-of-boolean-connective-null-search]
[UNSUPPORTED_OPERATION{message="a comparison or a boolean connective as the operand of an IS test"}];

--#[decode-of-boolean-connective-column-search]
[UNSUPPORTED_OPERATION{message="a comparison or a boolean connective as the operand of an IS test"}];
