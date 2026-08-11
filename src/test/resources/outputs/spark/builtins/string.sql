--#[trim-00]
SELECT trim(`T`.`c`) AS `_1` FROM `default`.`T` AS `T`;

--#[trim-01]
SELECT trim(`T`.`c`) AS `_1` FROM `default`.`T` AS `T`;

--#[trim-02]
SELECT ltrim(`T`.`c`) AS `_1` FROM `default`.`T` AS `T`;

--#[trim-03]
SELECT rtrim(`T`.`c`) AS `_1` FROM `default`.`T` AS `T`;

--#[trim-04]
SELECT trim(BOTH 'xxx' FROM `T`.`c`) AS `_1` FROM `default`.`T` AS `T`;

--#[trim-05]
SELECT trim(LEADING 'xxx' FROM `T`.`c`) AS `_1` FROM `default`.`T` AS `T`;

--#[trim-06]
SELECT trim(TRAILING 'xxx' FROM `T`.`c`) AS `_1` FROM `default`.`T` AS `T`;

--#[substring-00]
SELECT `SUBSTRING`(`T`.`c`, 2) AS `_1` FROM `default`.`T` AS `T`;

--#[substring-01]
SELECT `SUBSTRING`(`T`.`c`, 2, 3) AS `_1` FROM `default`.`T` AS `T`;

--#[substring-02]
SELECT `SUBSTRING`(`T`.`c`, 2) AS `_1` FROM `default`.`T` AS `T`;

--#[substring-03]
SELECT `SUBSTRING`(`T`.`c`, 2, 3) AS `_1` FROM `default`.`T` AS `T`;

--#[substring-10]
[ScribeException{code=UNSUPPORTED_OPERATION, message="Scribe rejects Spark substring with a literal start less than 1 because Spark accepts start values less than 1 with different semantics. Non-literal start/length expressions are passed through unchanged."}];

--#[substring-11]
[ScribeException{code=UNSUPPORTED_OPERATION, message="Scribe rejects Spark substring with a literal start less than 1 because Spark accepts start values less than 1 with different semantics. Non-literal start/length expressions are passed through unchanged."}];

--#[substring-12]
[ScribeException{code=UNSUPPORTED_OPERATION, message="Scribe rejects Spark substring with a literal start less than 1 because Spark accepts start values less than 1 with different semantics. Non-literal start/length expressions are passed through unchanged."}];

--#[substring-13]
[ScribeException{code=UNSUPPORTED_OPERATION, message="Scribe rejects Spark substring with a literal start less than 1 because Spark accepts start values less than 1 with different semantics. Non-literal start/length expressions are passed through unchanged."}];

--#[substring-14]
[ScribeException{code=UNSUPPORTED_OPERATION, message="Scribe rejects Spark substring with a literal start less than 1 because Spark accepts start values less than 1 with different semantics. Non-literal start/length expressions are passed through unchanged."}];

--#[position-00]
SELECT POSITION('a' IN `T`.`c`) AS `_1` FROM `default`.`T` AS `T`;

--#[char-length-00]
SELECT `CHAR_LENGTH`(`T`.`c`) AS `_1` FROM `default`.`T` AS `T`;

--#[replace-00]
SELECT `REPLACE`(`T`.`c`, 'a', 'b') AS `_1` FROM `default`.`T` AS `T`;

--#[split-00]
SELECT `SPLIT`(`T`.`c`, ',') AS `_1` FROM `default`.`T` AS `T`;

--#[split-01]
SELECT `SPLIT`(`T`.`c`, '\\.') AS `_1` FROM `default`.`T` AS `T`;

--#[split-02]
SELECT `SPLIT`(`T`.`c`, '\\|') AS `_1` FROM `default`.`T` AS `T`;

--#[split-03]
SELECT `SPLIT`(`T`.`c`, '\\\\') AS `_1` FROM `default`.`T` AS `T`;

--#[split-04]
SELECT `SPLIT`(`T`.`c`, '::') AS `_1` FROM `default`.`T` AS `T`;

--#[split-05]
SELECT `SPLIT`(`T`.`c`, `CONCAT`('\\Q', `REPLACE`(`T`.`z`, '\\E', '\\E\\\\E\\Q'), '\\E')) AS `_1` FROM `default`.`T` AS `T`;

--#[split-06]
SELECT `SPLIT`(`T`.`c`, '\\[a\\-z\\]\\+') AS `_1` FROM `default`.`T` AS `T`;

--#[split-07]
SELECT `SPLIT`(`T`.`z`, `CONCAT`('\\Q', `REPLACE`(`T`.`c`, '\\E', '\\E\\\\E\\Q'), '\\E')) AS `_1` FROM `default`.`T` AS `T`;

--#[split-08]
[ScribeException{code=UNSUPPORTED_OPERATION, message="Spark split with an empty string delimiter is unsupported because Spark splits between characters while PartiQL returns the original string as a single-element list."}];
