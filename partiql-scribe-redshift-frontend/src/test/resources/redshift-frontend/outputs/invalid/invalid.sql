--#[syntax-00]
[INVALID_INPUT{message="extraneous input 'FROM'"}];

--#[syntax-01]
[INVALID_INPUT{message="mismatched input ';'"}];

--#[syntax-02]
[INVALID_INPUT{message="extraneous input '@'"}];

--#[unknown-table]
[INVALID_INPUT{message="VAR_REF_NOT_FOUND"}];

--#[unknown-column]
[INVALID_INPUT{message="column `nope` does not exist"}];

--#[top-and-limit]
[INVALID_INPUT{message="TOP and LIMIT can't be used in the same query"}];

--#[qualify-without-alias]
[INVALID_INPUT{message="the FROM item before QUALIFY needs an alias"}];
