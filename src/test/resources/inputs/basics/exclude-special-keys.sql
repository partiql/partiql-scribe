-- Field names with a backslash or a double quote, which Redshift OBJECT_TRANSFORM paths must escape.

--#[exclude-special-keys-00]
SELECT * EXCLUDE t.flds.j FROM EXCLUDE_T_SPECIAL_KEYS AS t;
