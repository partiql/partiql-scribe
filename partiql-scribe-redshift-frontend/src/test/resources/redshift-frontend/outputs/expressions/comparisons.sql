--#[string-literal-date-00]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE "t_all_types"['col_date'] = DATE '2020-01-01';

--#[string-literal-date-01]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE "t_all_types"['col_date'] <> DATE '2020-01-01';

--#[string-literal-date-02]
[UNSUPPORTED{message="comparison of DATE with the character string '2008-01-04 10:00:00', which has a time of day"}];

--#[string-literal-date-03]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE DATE '2020-01-01' < "t_all_types"['col_date'];

--#[string-literal-timestamp-00]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE "t_all_types"['col_timestamp'] >= TIMESTAMP '2020-01-01 00:00:00';

--#[string-literal-timestamp-01]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE "t_all_types"['col_timestamp'] < TIMESTAMP '2020-01-01 10:20:30.5';

--#[string-literal-timestamp-02]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE "t_all_types"['col_timestamp'] = TIMESTAMP '2008-06-01 09:59:59';

--#[string-literal-boolean-00]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE "t_all_types"['col_bool'] = true;

--#[string-literal-boolean-01]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE "t_all_types"['col_bool'] <> false;

--#[string-literal-boolean-02]
[UNSUPPORTED{message="comparison of BOOLEAN with the character string 'maybe'"}];

--#[integer-literal-boolean-00]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE ("t_all_types"['col_bool'] = true) AND ("t_all_types"['col_bool'] <> false);

--#[integer-literal-boolean-01]
[UNSUPPORTED{message="comparison of BOOLEAN with 2"}];

--#[string-literal-integer-00]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE "t_all_types"['col_int32'] = 5;

--#[string-literal-integer-01]
[UNSUPPORTED{message="comparison of INTEGER with the character string '-0'"}];

--#[string-literal-integer-02]
[UNSUPPORTED{message="comparison of INTEGER with the character string '05'"}];

--#[string-literal-integer-03]
[UNSUPPORTED{message="ordering comparison of an integer with a character string '10'"}];

--#[string-literal-decimal-00]
SELECT "t"['foo']['bar'] AS "bar" FROM "default"."datatypes"."T_DECIMAL_5_2" AS "t" WHERE "t"['foo']['keep'] >= 1.5;

--#[string-literal-decimal-01]
[UNSUPPORTED{message="comparison of DECIMAL with the character string '1.505'"}];

--#[string-literal-float-00]
[UNSUPPORTED{message="comparison of DOUBLE PRECISION with the character string '1.5'"}];

--#[comparison-number-string-00]
[UNSUPPORTED{message="comparison of a number with a character string"}];

--#[comparison-number-string-01]
[UNSUPPORTED{message="comparison of a number with a character string"}];

--#[cross-family-00]
[UNSUPPORTED{message="comparison of DATE with VARCHAR"}];

--#[cross-family-01]
[UNSUPPORTED{message="comparison of BOOLEAN with INTEGER"}];

--#[comparison-char-00]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE TRIM(TRAILING FROM "t_all_types"['col_char']) = 'abc';

--#[comparison-char-01]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE TRIM(TRAILING FROM "t_all_types"['col_char']) = "t_all_types"['col_string'];

--#[comparison-varchar-00]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE ("t"['c'] = 'abc  ') AND ("t"['c'] <> "t"['z']);

--#[literals-00]
SELECT "t"['a'] AS "a" FROM "default"."T" AS "t" WHERE 'a' = 'a ';

--#[float-decimal-00]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE "t_all_types"['col_float64'] <= CAST(0.1 AS DOUBLE PRECISION);

--#[float-decimal-01]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE "t_all_types"['col_float64'] = CAST("t_all_types"['col_decimal'] AS DOUBLE PRECISION);

--#[float-decimal-02]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE CAST("t_all_types"['col_float32'] AS DOUBLE PRECISION) > CAST("t_all_types"['col_int32'] AS DOUBLE PRECISION);

--#[float-decimal-03]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE ("t_all_types"['col_float64'] > CAST("t_all_types"['col_int64'] AS DOUBLE PRECISION)) AND (CAST("t_all_types"['col_float32'] AS DOUBLE PRECISION) < "t_all_types"['col_float64']);

--#[float-decimal-04]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE (("t_all_types"['col_float64'] < 100.0) AND ("t_all_types"['col_float32'] >= 0.5)) AND (CAST("t_all_types"['col_float32'] AS DOUBLE PRECISION) < CAST(0.1 AS DOUBLE PRECISION));

--#[comparison-datetime-00]
SELECT "t_all_types"['col_int32'] AS "col_int32" FROM "default"."T_ALL_TYPES" AS "t_all_types" WHERE (CAST("t_all_types"['col_date'] AS TIMESTAMP WITH TIME ZONE) < "t_all_types"['col_timestampz']) AND ("t_all_types"['col_timestamp'] >= CAST("t_all_types"['col_date'] AS TIMESTAMP));

--#[interval-00]
SELECT "t"['col_date'] AS "col_date" FROM "default"."T_INTERVALS" AS "t" WHERE ("t"['col_y2mon'] = "t"['col_y2mon']) AND ("t"['col_d2s'] <> "t"['col_d2s']);

--#[interval-01]
[UNSUPPORTED{message="comparison `>` of INTERVAL values"}];

--#[interval-02]
[UNSUPPORTED{message="comparison of year-month and day-time INTERVAL values"}];

--#[comparison-super-00]
SELECT "t"['b'] AS "b" FROM "default"."T" AS "t" WHERE ("t"['x']['k'] = 5) AND ("t"['x']['s'] <> 'a');
