-- Lexical structure: identifiers, comments, literals, keywords, and the statement terminator.
-- https://docs.aws.amazon.com/redshift/latest/dg/r_names.html
-- https://docs.aws.amazon.com/redshift/latest/dg/r_Literals.html

--#[ident-unquoted]
SELECT a, _b, c1, d$e FROM t;

--#[ident-unquoted-case]
SELECT Column_Name, COLUMN_NAME FROM MySchema.MyTable;

--#[ident-unquoted-multibyte]
SELECT 列, naïve FROM 表;

--#[ident-quoted]
SELECT "a", "Mixed Case", "with space" FROM "t";

--#[ident-quoted-escape]
SELECT "say ""hi""" FROM t;

--#[ident-quoted-reserved-word]
SELECT "select", "from" FROM "where";

--#[ident-two-part]
SELECT t.a, s.t.b FROM s.t;

--#[ident-three-part]
SELECT a FROM db.schema.t;

--#[ident-three-part-column]
SELECT schema.t.a FROM schema.t;

--#[comment-line]
SELECT a -- the a column
FROM t; -- trailing

--#[comment-block]
SELECT /* inline */ a FROM t /* at
the end */;

--#[comment-block-between-tokens]
SELECT a/**/FROM/**/t;

--#[comment-only-before]
-- a leading comment
/* and a block */
SELECT a FROM t;

--#[literal-integer]
SELECT 0, 10, 123456789012345678 FROM t;

--#[literal-decimal]
SELECT 1.5, .5, 5., 0.001 FROM t;

--#[literal-string]
SELECT 'abc', '', 'it''s' FROM t;

--#[literal-string-backslash]
SELECT 'a\'b', 'line\nbreak', 'back\\slash' FROM t;

--#[literal-string-multiline]
SELECT 'two
lines' FROM t;

--#[literal-date]
SELECT DATE '2024-01-31' FROM t;

--#[literal-timestamp]
SELECT TIMESTAMP '2024-01-31 12:34:56', TIMESTAMP WITHOUT TIME ZONE '2024-01-31 12:34:56.789' FROM t;

--#[literal-boolean-null]
SELECT TRUE, FALSE, NULL FROM t;

--#[literal-boolean-case]
SELECT true, False FROM t;

--#[keyword-case]
select A from T where B is not null order by A desc;

--#[nonreserved-as-column]
SELECT date, time, year, month, day, first, last, count, max, min, sum, avg, text, int, upper, lower FROM t;

--#[nonreserved-as-table]
SELECT a FROM date, time, year, count;

--#[nonreserved-as-alias]
SELECT a AS date, b AS year, c count FROM t;

--#[reserved-as-alias-after-as]
SELECT CAST(test AS TIMESTAMP) AS timestamp, CAST(test AS DATE) AS date, a AS from, b AS select, c AS "group" FROM formattest;

--#[trailing-semicolon]
SELECT a FROM t;

--#[no-semicolon]
SELECT a FROM t

--#[whitespace]
SELECT
    a,
	b
FROM
    t
;
