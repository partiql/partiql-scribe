-- Standard identifiers can contain UTF-8 multibyte characters and dollar signs; Redshift folds only ASCII letters.
--#[identifier-multibyte]
SELECT b AS café FROM T;

--#[identifier-quoted-folds-ascii-only]
SELECT b AS "CAFÉ" FROM T;

--#[identifier-dollar]
SELECT b AS a$b FROM T;

-- A double quote in a delimited identifier is written twice, also in the output.
--#[identifier-double-quote]
SELECT b AS "x""y" FROM T;

--#[identifier-double-quote-alias]
SELECT "q""r".b FROM T AS "q""r";

-- Reserved words are not identifiers; the niladic functions USER, CURRENT_USER, and SESSION_USER are unsupported.
--#[reserved-word-alias]
SELECT b AS user FROM T;

--#[niladic-user]
SELECT user FROM T;

--#[niladic-current-user]
SELECT c || CURRENT_USER AS v FROM T;

--#[reserved-word-quoted]
SELECT b AS "user" FROM T;

--#[non-reserved-word]
SELECT b AS qualify, c AS start FROM T;

-- Block comments that contain the start of another block comment are unsupported: Redshift does not document whether
-- they nest.
--#[comment-nested]
SELECT b /* x /* y */ , '*/' AS c FROM T;

--#[comment-block]
SELECT b /* a comment */ FROM T -- a line comment
;
