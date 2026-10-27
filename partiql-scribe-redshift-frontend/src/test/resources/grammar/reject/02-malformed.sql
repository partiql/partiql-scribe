-- Malformed queries: incomplete clauses, unbalanced delimiters, misplaced tokens. The expected position is that of
-- the first token the parser could not accept (or the end of the input).

--#[empty] 1:1

--#[semicolon-only] 1:1
;

--#[select-only] 1:7
SELECT

--#[select-no-items] 1:8
SELECT FROM t;

--#[select-trailing-comma] 1:11
SELECT a, FROM t;

--#[select-double-comma] 1:10
SELECT a,, b FROM t;

--#[from-no-table] 1:14
SELECT a FROM;

--#[from-trailing-comma] 1:18
SELECT a FROM t, WHERE a = 1;

--#[where-no-condition] 1:22
SELECT a FROM t WHERE;

--#[where-incomplete] 1:27
SELECT a FROM t WHERE a = ;

--#[where-at-end-of-input] 1:22
SELECT a FROM t WHERE

--#[and-dangling] 1:32
SELECT a FROM t WHERE a = 1 AND;

--#[group-by-empty] 1:25
SELECT a FROM t GROUP BY;

--#[group-by-trailing-comma] 1:29
SELECT a FROM t GROUP BY a, HAVING COUNT(*) > 1;

--#[having-no-condition] 1:34
SELECT a FROM t GROUP BY a HAVING;

--#[order-by-empty] 1:25
SELECT a FROM t ORDER BY;

--#[order-by-direction-only] 1:26
SELECT a FROM t ORDER BY DESC;

--#[limit-no-value] 1:22
SELECT a FROM t LIMIT;

--#[limit-comma] 1:25
SELECT a FROM t LIMIT 10, 20;

--#[offset-no-value] 1:34
SELECT a FROM t ORDER BY a OFFSET;

--#[paren-unclosed] 1:29
SELECT a FROM t WHERE (a = 1;

--#[paren-extra] 1:28
SELECT a FROM t WHERE a = 1);

--#[paren-empty-expression] 1:9
SELECT () FROM t;

--#[subquery-unclosed] 1:37
SELECT a FROM (SELECT a FROM t WHERE;

--#[string-unterminated] 1:27
SELECT a FROM t WHERE b = 'abc;

--#[quoted-identifier-unterminated] 1:8
SELECT "abc FROM t;

--#[block-comment-unterminated] 1:17
SELECT a FROM t /* comment;

--#[operator-double] 1:27
SELECT a FROM t WHERE a = = 1;

--#[operator-unknown] 1:26
SELECT a FROM t WHERE a === 1;

--#[operator-dangling] 1:12
SELECT a + FROM t;

--#[case-no-end] 1:34
SELECT CASE WHEN a THEN 1 ELSE 0 FROM t;

--#[case-no-when] 1:15
SELECT CASE a THEN 1 END FROM t;

--#[case-no-then] 1:20
SELECT CASE WHEN a 1 END FROM t;

--#[cast-no-type] 1:17
SELECT CAST(a AS) FROM t;

--#[cast-no-as] 1:15
SELECT CAST(a INT) FROM t;

--#[function-unclosed] 1:16
SELECT COUNT(a FROM t;

--#[function-trailing-comma] 1:20
SELECT COALESCE(a, ) FROM t;

--#[between-no-and] 1:35
SELECT a FROM t WHERE a BETWEEN 1 10;

--#[in-empty-list] 1:29
SELECT a FROM t WHERE a IN ();

--#[in-no-parens] 1:28
SELECT a FROM t WHERE a IN 1, 2;

--#[is-no-operand] 1:28
SELECT a FROM t WHERE a IS 1;

--#[join-no-table] 1:22
SELECT a FROM t JOIN ON t.a = 1;

--#[join-on-no-condition] 1:26
SELECT a FROM t JOIN u ON;

--#[with-no-query] 1:28
WITH c AS (SELECT a FROM t);

--#[with-no-parens] 1:11
WITH c AS SELECT a FROM t SELECT a FROM c;

--#[union-no-right] 1:22
SELECT a FROM t UNION;

--#[union-no-left] 1:1
UNION SELECT a FROM t;

--#[interval-no-string] 1:17
SELECT INTERVAL 1 DAY;

--#[date-no-string] 1:13
SELECT DATE 20240101;

--#[reserved-word-as-column] 1:8
SELECT select FROM t;

--#[reserved-word-as-table] 1:15
SELECT a FROM where;

--#[reserved-word-as-alias] 1:13
SELECT a AS from FROM t;

--#[alias-numeric] 1:10
SELECT a 1 FROM t;

--#[unexpected-character] 1:10
SELECT a ? FROM t;

--#[unexpected-character-backtick] 1:8
SELECT `a` FROM t;

--#[unexpected-character-bracket-table] 1:15
SELECT a FROM [t];

