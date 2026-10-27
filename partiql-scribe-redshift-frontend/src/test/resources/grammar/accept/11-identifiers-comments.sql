-- Identifiers, keywords, comments, whitespace, and statement terminators.
-- https://docs.aws.amazon.com/redshift/latest/dg/r_names.html

--#[identifier-case]
SELECT Column_A, COLUMN_B, column_c FROM MySchema.MyTable;

--#[identifier-underscore-digits]
SELECT _a, a_1, a1b2, __x FROM t_1;

--#[identifier-dollar]
SELECT a$b, total$, _$x FROM t$1;

--#[identifier-unicode]
SELECT café, 名前, straße FROM tëst;

--#[identifier-quoted]
SELECT "a", "Mixed Case", "with space", "with-dash", "123", "select" FROM "T";

--#[identifier-quoted-escaped-quote]
SELECT "say ""hi""" FROM t;

--#[identifier-quoted-qualified]
SELECT "t"."a", "s"."t"."a" FROM "s"."t";

--#[identifier-quoted-keyword]
SELECT "select", "from", "where", "group" FROM "order";

--#[keywords-case-insensitive]
select a from t where b = 1 group by a having count(*) > 0 order by a limit 1;

--#[keywords-mixed-case]
Select a From t Where b = 1 Order By a;

--#[non-reserved-as-column]
SELECT date, time, year, month, day, hour, minute, second, count, position, zone, first, last, rows, range, over FROM t;

--#[non-reserved-as-table]
SELECT a FROM date, time AS t2, count c;

--#[non-reserved-as-alias]
SELECT a AS date, b AS count, c AS year, d AS first, e AS last FROM t;

--#[non-reserved-as-function]
SELECT date(d), time(ts), count(a), position(s), max(a), char(65), exists(a) FROM t;

--#[niladic-as-alias]
SELECT a AS user, b AS current_user FROM t;

--#[comment-line]
SELECT a -- the column
FROM t -- the table
;

--#[comment-line-at-start]
-- a leading comment
SELECT a FROM t;

--#[comment-line-at-end]
SELECT a FROM t
-- a trailing comment

--#[comment-block]
SELECT /* the column */ a FROM /* the table */ t;

--#[comment-block-multiline]
SELECT a
/*
 * A multi-line
 * comment.
 */
FROM t;

--#[comment-block-in-expression]
SELECT a +/* plus */b, a ||/* concat */ b, a/*x*/-/*y*/b FROM t;

--#[comment-with-quotes]
SELECT a FROM t -- it's "quoted"
WHERE b = 'x' /* and 'y' */;

--#[comment-only-content]
SELECT 'not -- a comment', 'not /* a comment */' FROM t;

--#[whitespace]
SELECT
    a,
	b
FROM
    t
WHERE
    a = 1;

--#[no-whitespace]
SELECT a,b FROM t WHERE a=1 AND b<>2 OR c>=3;

--#[terminator]
SELECT a FROM t;

--#[terminator-none]
SELECT a FROM t

--#[terminator-several]
SELECT a FROM t;;;

--#[terminator-whitespace-after]
SELECT a FROM t;

--#[two-statements]
SELECT a FROM t; SELECT b FROM u;

--#[temp-table-names]
SELECT a FROM #t, #tmp_1 AS x, #T$2;
