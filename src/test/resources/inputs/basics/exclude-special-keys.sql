-- Field names with characters that Redshift OBJECT_TRANSFORM paths must (\, ") or need not (., [, ') escape.

--#[exclude-special-keys-00]
SELECT * EXCLUDE t.flds.j FROM EXCLUDE_T_SPECIAL_KEYS AS t;
