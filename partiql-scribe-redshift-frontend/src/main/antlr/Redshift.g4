/*
 * Amazon Redshift query grammar for the Redshift-to-PartiQL frontend.
 *
 * Scope: the Redshift constructs that map to a PartiQL construct the target view engine supports, directly or by an
 * exact rewrite. The query that defines a view is what the frontend translates, so the grammar accepts what the
 * translation can produce and nothing more: what parses is what translates. Everything else is a syntax error,
 * including
 *
 *  - every statement other than a single SELECT query (and a second statement after it);
 *  - the constructs the target does not accept yet: window functions, EXISTS, INTERSECT and EXCEPT, PIVOT and
 *    UNPIVOT, QUALIFY, JOIN USING and NATURAL JOIN, SELECT without FROM, SUPER navigation (`a.b[0].c`, unnesting in
 *    FROM), WITH RECURSIVE, ROLLUP, CUBE and GROUPING SETS, CONNECT BY, WITHIN GROUP;
 *  - every function without a mapping: a function call is not generic, only the functions listed in `aggregateName`,
 *    `scalarFunctionName`, and the special forms of `atom` are parsed, each with its argument form. Any other `name(`
 *    is a syntax error at the parenthesis;
 *  - the parts of an accepted construct that are refused: `%`, `|/`, `||/`, `@`, `|`, `#`, `~`, `<<`, `>>`, AT TIME
 *    ZONE, TRY_CAST, BETWEEN SYMMETRIC, quantified comparisons (`= ANY`), row values (`(a, b) IN`), SIMILAR TO and
 *    the POSIX operators, exponent literals (`1e3`), the TIME, TIMETZ and TIMESTAMPTZ literals and types, SUPER,
 *    VARBYTE and INTERVAL types, interval qualifiers (`INTERVAL '1' DAY TO SECOND`), LIMIT with an expression.
 *
 * Words that Redshift reserves stay keywords of the lexer even when no rule uses them, so that a query using them
 * (`INTERSECT`, `USING`, `PIVOT`, `CURRENT_USER`, ...) fails at that word. Words that Redshift does not reserve are
 * identifiers (see `nonReserved`), so a removed construct that starts with one (`QUALIFY`, `EXISTS`, `OVER`) fails at
 * the token after it.
 *
 * Operator precedence follows PostgreSQL, from which Redshift derives: '::', then unary sign, then '^', then '* /',
 * then '+ -', then '||' and '&', then the predicates, then NOT, AND, OR.
 *
 * Reference: https://docs.aws.amazon.com/redshift/latest/dg/r_SELECT_synopsis.html
 */
grammar Redshift;

options { caseInsensitive = true; }

@parser::members {
    private static final java.util.Set<String> INTERVAL_FIELDS = new java.util.HashSet<>(
        java.util.Arrays.asList("YEAR", "MONTH", "DAY", "HOUR", "MINUTE", "SECOND"));

    /**
     * True when the next token follows an INTERVAL literal and is an interval field name. Redshift reads
     * `INTERVAL '1' DAY` as a qualified interval literal, not as the literal with the alias `day`; the qualifier has no
     * rule here, so the alias reading is blocked and the word is a syntax error.
     */
    private boolean intervalQualifierFollows() {
        return _input.LT(-1).getType() == STRING_LITERAL
            && _input.LT(-2).getType() == INTERVAL
            && INTERVAL_FIELDS.contains(_input.LT(1).getText().toUpperCase(java.util.Locale.ROOT));
    }
}

/*
 * One query with an optional trailing semicolon. A view is one query: a second statement is a syntax error.
 */
root
    : query SEMICOLON? EOF
    ;

/*
 * Queries
 *
 * UNION is the only set operation; it is left-associative. ORDER BY, LIMIT, and OFFSET at the end of a query apply to
 * the result of the set operation. https://docs.aws.amazon.com/redshift/latest/dg/r_UNION.html
 */
query
    : withClause? queryTerm orderByClause? limitClause?
    ;

withClause
    : WITH commonTableExpression (COMMA commonTableExpression)*
    ;

commonTableExpression
    : name=identifier columnAliases? AS PAREN_LEFT query PAREN_RIGHT
    ;

queryTerm
    : queryPrimary                                              # queryTermPrimary
    | lhs=queryTerm UNION setQuantifier? rhs=queryTerm          # queryTermUnion
    ;

queryPrimary
    : querySpecification                        # queryPrimarySpecification
    | PAREN_LEFT query PAREN_RIGHT              # queryPrimaryParenthesized
    ;

/*
 * FROM is required: a view needs at least one source table, so `SELECT 1` is a syntax error.
 */
querySpecification
    : SELECT topClause? setQuantifier? selectList
      excludeClause?
      fromClause
      whereClause?
      groupByClause?
      havingClause?
    ;

topClause
    : TOP INTEGER_LITERAL
    ;

setQuantifier
    : DISTINCT
    | ALL
    ;

selectList
    : selectItem (COMMA selectItem)*
    ;

selectItem
    : ASTERISK                                  # selectItemAll
    | qualifiedName DOT ASTERISK                # selectItemStar
    | expr (AS alias=identifier | {!intervalQualifierFollows()}? alias=identifier)?  # selectItemExpr
    ;

/*
 * https://docs.aws.amazon.com/redshift/latest/dg/r_EXCLUDE_list.html
 */
excludeClause
    : EXCLUDE (identifier (COMMA identifier)* | PAREN_LEFT identifier (COMMA identifier)* PAREN_RIGHT)
    ;

fromClause
    : FROM tableReference (COMMA tableReference)*
    ;

/*
 * Joins: CROSS JOIN, and INNER, LEFT, RIGHT, and FULL [OUTER] JOIN with an ON condition. Left-associative.
 * https://docs.aws.amazon.com/redshift/latest/dg/r_FROM_clause30.html
 */
tableReference
    : lhs=tableReference CROSS JOIN rhs=tablePrimary                                  # tableCrossJoin
    | lhs=tableReference joinType? JOIN rhs=tablePrimary ON condition=expr            # tableJoin
    | tablePrimary                                                                    # tableBase
    ;

joinType
    : INNER
    | LEFT OUTER?
    | RIGHT OUTER?
    | FULL OUTER?
    ;

/*
 * A subquery, a parenthesized join, or a table, view, or common table expression by its (qualified) name.
 */
tablePrimary
    : PAREN_LEFT query PAREN_RIGHT tableAlias?                                      # tableSubquery
    | PAREN_LEFT tableReference PAREN_RIGHT tableAlias?                             # tableParenthesized
    | qualifiedName tableAlias?                                                     # tableName
    ;

tableAlias
    : AS? alias=identifier columnAliases?
    ;

columnAliases
    : PAREN_LEFT identifier (COMMA identifier)* PAREN_RIGHT
    ;

whereClause
    : WHERE expr
    ;

/*
 * Expressions and ordinals (`GROUP BY 1`); an ordinal is an integer literal expression.
 */
groupByClause
    : GROUP BY expr (COMMA expr)*
    ;

havingClause
    : HAVING expr
    ;

orderByClause
    : ORDER BY sortItem (COMMA sortItem)*
    ;

sortItem
    : expr dir=(ASC | DESC)? (NULLS nulls=(FIRST | LAST))?
    ;

/*
 * LIMIT { count | ALL } and OFFSET start, in either order; both take an integer constant.
 * https://docs.aws.amazon.com/redshift/latest/dg/r_ORDER_BY_clause.html
 */
limitClause
    : LIMIT limit=limitValue (OFFSET offset=INTEGER_LITERAL)?
    | OFFSET offset=INTEGER_LITERAL (LIMIT limit=limitValue)?
    ;

limitValue
    : ALL
    | INTEGER_LITERAL
    ;

/*
 * Expressions
 */

expr
    : exprOr
    ;

exprOr
    : lhs=exprOr OR rhs=exprAnd                 # or
    | exprAnd                                   # orBase
    ;

exprAnd
    : lhs=exprAnd AND rhs=exprNot               # and
    | exprNot                                   # andBase
    ;

exprNot
    : NOT exprNot                               # not
    | predicate                                 # notBase
    ;

/*
 * Predicates. Reference: https://docs.aws.amazon.com/redshift/latest/dg/r_conditions.html
 */
predicate
    : lhs=valueExpr op=comparisonOperator rhs=valueExpr                                 # predicateComparison
    | lhs=valueExpr IS NOT? NULL                                                        # predicateNull
    | lhs=valueExpr op=(ISNULL | NOTNULL)                                               # predicateNullPostfix
    | lhs=valueExpr IS NOT? truth=(TRUE | FALSE | UNKNOWN)                              # predicateTruth
    | lhs=valueExpr NOT? BETWEEN lower=valueExpr AND upper=valueExpr                    # predicateBetween
    | lhs=valueExpr NOT? IN PAREN_LEFT query PAREN_RIGHT                                # predicateInQuery
    | lhs=valueExpr NOT? IN PAREN_LEFT expr (COMMA expr)* PAREN_RIGHT                   # predicateInList
    | lhs=valueExpr NOT? op=(LIKE | ILIKE) rhs=valueExpr (ESCAPE escape=valueExpr)?     # predicateLike
    | valueExpr                                                                         # predicateBase
    ;

comparisonOperator
    : EQ | NEQ | LT | LT_EQ | GT | GT_EQ
    ;

/*
 * `||` (concatenation) and `&` (bitwise AND) have one precedence level, below `+` and `-`, as in PostgreSQL.
 * Reference: https://docs.aws.amazon.com/redshift/latest/dg/r_OPERATOR_SYMBOLS.html
 */
valueExpr
    : atom                                                      # valuePrimary
    | valueExpr DOUBLE_COLON dataType                           # valueCastShorthand
    | sign=(PLUS | MINUS) valueExpr                             # valueSigned
    | lhs=valueExpr op=CARET rhs=valueExpr                      # valueExponent
    | lhs=valueExpr op=(ASTERISK | SLASH) rhs=valueExpr         # valueMultiplicative
    | lhs=valueExpr op=(PLUS | MINUS) rhs=valueExpr             # valueAdditive
    | lhs=valueExpr op=(CONCAT | AMPERSAND) rhs=valueExpr       # valueOperator
    ;

/*
 * The functions are listed one by one, each with its argument form. The aggregates take an optional DISTINCT or
 * ALL; `COUNT(*)` is its own form. DATEADD and DATEDIFF take a date part first; EXTRACT takes EPOCH only. The
 * niladic functions are CURRENT_DATE and SYSDATE (no parentheses) and GETDATE() (empty parentheses).
 */
atom
    : literal                                                                       # atomLiteral
    | CAST PAREN_LEFT expr AS dataType PAREN_RIGHT                                  # atomCast
    | CONVERT PAREN_LEFT dataType COMMA expr PAREN_RIGHT                            # atomConvert
    | CASE operand=expr? caseWhen+ (ELSE otherwise=expr)? END                       # atomCase
    | name=(DATEADD | DATEDIFF) PAREN_LEFT part=datePart COMMA expr COMMA expr PAREN_RIGHT # atomDateFunction
    | EXTRACT PAREN_LEFT EPOCH FROM expr PAREN_RIGHT                                # atomExtractEpoch
    | COUNT PAREN_LEFT ASTERISK PAREN_RIGHT                                         # atomCountStar
    | name=aggregateName PAREN_LEFT setQuantifier? expr PAREN_RIGHT                 # atomAggregate
    | name=scalarFunctionName PAREN_LEFT expr (COMMA expr)* PAREN_RIGHT             # atomFunctionCall
    | GETDATE PAREN_LEFT PAREN_RIGHT                                                # atomGetdate
    | SYSDATE                                                                       # atomSysdate
    | CURRENT_DATE                                                                  # atomCurrentDate
    | PAREN_LEFT query PAREN_RIGHT                                                  # atomSubquery
    | PAREN_LEFT expr PAREN_RIGHT                                                   # atomParenthesized
    | qualifiedName                                                                 # atomName
    ;

aggregateName
    : AVG | COUNT | MAX | MIN | SUM
    ;

scalarFunctionName
    : COALESCE | CONCAT_FUNCTION | DECODE | GREATEST | LEAST | LOWER | NULLIF | NVL | NVL2 | POWER | UPPER
    ;

/*
 * A date part of DATEADD and DATEDIFF: a name (`day`) or a character string (`'day'`).
 * Reference: https://docs.aws.amazon.com/redshift/latest/dg/r_Dateparts_for_datetime_functions.html
 */
datePart
    : identifier
    | STRING_LITERAL
    ;

caseWhen
    : WHEN condition=expr THEN result=expr
    ;

/*
 * A column, table, or schema-qualified name of up to three parts: `col`, `t.col`, `schema.t.col`, `db.schema.t`.
 * A fourth part, or a bracket, would be SUPER navigation, which is a syntax error.
 */
qualifiedName
    : identifier (DOT identifier (DOT identifier)?)?
    ;

/*
 * An INTERVAL literal has no qualifier; it is accepted only inside datetime arithmetic (`ts + INTERVAL '1 day'`),
 * which the translation checks.
 */
literal
    : NULL                                      # literalNull
    | TRUE                                      # literalTrue
    | FALSE                                     # literalFalse
    | INTEGER_LITERAL                           # literalInteger
    | DECIMAL_LITERAL                           # literalDecimal
    | STRING_LITERAL                            # literalString
    | DATE STRING_LITERAL                       # literalDate
    | TIMESTAMP (WITHOUT TIME ZONE)? STRING_LITERAL # literalTimestamp
    | INTERVAL STRING_LITERAL                   # literalInterval
    ;

/*
 * The data types with a counterpart in the target, with their Redshift aliases. The multi-word names come first, so
 * that `CHAR VARYING` is one type and not the type CHAR followed by the alias `varying`.
 * Reference: https://docs.aws.amazon.com/redshift/latest/dg/c_Supported_data_types.html
 */
dataType
    : ((CHARACTER | CHAR) VARYING | VARCHAR | NVARCHAR | TEXT) length=typeLength?           # typeVarchar
    | name=(CHAR | CHARACTER | NCHAR | BPCHAR) length=typeLength?                           # typeChar
    | DOUBLE PRECISION                                                                      # typeDouble
    | name=(FLOAT8 | FLOAT)                                                                 # typeDouble
    | name=(REAL | FLOAT4)                                                                  # typeReal
    | name=(DECIMAL | NUMERIC)
      (PAREN_LEFT precision=INTEGER_LITERAL (COMMA scale=INTEGER_LITERAL)? PAREN_RIGHT)?    # typeDecimal
    | name=(SMALLINT | INT2 | INTEGER | INT | INT4 | BIGINT | INT8)                         # typeInteger
    | TIMESTAMP (WITHOUT TIME ZONE)?                                                        # typeTimestamp
    | DATE                                                                                  # typeDate
    | name=(BOOLEAN | BOOL)                                                                 # typeBoolean
    ;

/*
 * The length of a character type: an integer or MAX.
 */
typeLength
    : PAREN_LEFT (INTEGER_LITERAL | MAX) PAREN_RIGHT
    ;

identifier
    : IDENTIFIER                                # identifierUnquoted
    | IDENTIFIER_QUOTED                         # identifierQuoted
    | nonReserved                               # identifierNonReserved
    ;

/*
 * Keywords of this grammar that Redshift does not reserve, so they can also be identifiers.
 * Reference: https://docs.aws.amazon.com/redshift/latest/dg/r_pg_keywords.html
 */
nonReserved
    : AVG | BIGINT | BOOL | BOOLEAN | BPCHAR | CHAR | CHARACTER | COALESCE | CONCAT_FUNCTION | CONVERT | COUNT | DATE
    | DATEADD | DATEDIFF | DECIMAL | DECODE | DOUBLE | EPOCH | ESCAPE | EXCLUDE | EXTRACT | FIRST | FLOAT | FLOAT4
    | FLOAT8 | GETDATE | GREATEST | INT | INT2 | INT4 | INT8 | INTEGER | INTERVAL | LAST | LEAST | LOWER | MAX | MIN
    | NCHAR | NULLIF | NUMERIC | NVARCHAR | NVL | NVL2 | POWER | PRECISION | REAL | SMALLINT | SUM | TEXT | TIME
    | UNKNOWN | UPPER | VARCHAR | VARYING | ZONE
    ;

/*
 * Keywords
 */

ALL: 'ALL';
AND: 'AND';
AS: 'AS';
ASC: 'ASC';
AVG: 'AVG';
BETWEEN: 'BETWEEN';
BIGINT: 'BIGINT';
BOOL: 'BOOL';
BOOLEAN: 'BOOLEAN';
BPCHAR: 'BPCHAR';
BY: 'BY';
CASE: 'CASE';
CAST: 'CAST';
CHAR: 'CHAR';
CHARACTER: 'CHARACTER';
COALESCE: 'COALESCE';
CONCAT_FUNCTION: 'CONCAT';
CONVERT: 'CONVERT';
COUNT: 'COUNT';
CROSS: 'CROSS';
CURRENT_DATE: 'CURRENT_DATE';
DATE: 'DATE';
DATEADD: 'DATEADD';
DATEDIFF: 'DATEDIFF';
DECIMAL: 'DECIMAL';
DECODE: 'DECODE';
DESC: 'DESC';
DISTINCT: 'DISTINCT';
DOUBLE: 'DOUBLE';
ELSE: 'ELSE';
END: 'END';
EPOCH: 'EPOCH';
ESCAPE: 'ESCAPE';
EXCLUDE: 'EXCLUDE';
EXTRACT: 'EXTRACT';
FALSE: 'FALSE';
FIRST: 'FIRST';
FLOAT: 'FLOAT';
FLOAT4: 'FLOAT4';
FLOAT8: 'FLOAT8';
FROM: 'FROM';
FULL: 'FULL';
GETDATE: 'GETDATE';
GREATEST: 'GREATEST';
GROUP: 'GROUP';
HAVING: 'HAVING';
ILIKE: 'ILIKE';
IN: 'IN';
INNER: 'INNER';
INT: 'INT';
INT2: 'INT2';
INT4: 'INT4';
INT8: 'INT8';
INTEGER: 'INTEGER';
INTERVAL: 'INTERVAL';
IS: 'IS';
ISNULL: 'ISNULL';
JOIN: 'JOIN';
LAST: 'LAST';
LEAST: 'LEAST';
LEFT: 'LEFT';
LIKE: 'LIKE';
LIMIT: 'LIMIT';
LOWER: 'LOWER';
MAX: 'MAX';
MIN: 'MIN';
NCHAR: 'NCHAR';
NOT: 'NOT';
NOTNULL: 'NOTNULL';
NULL: 'NULL';
NULLIF: 'NULLIF';
NULLS: 'NULLS';
NUMERIC: 'NUMERIC';
NVARCHAR: 'NVARCHAR';
NVL: 'NVL';
NVL2: 'NVL2';
OFFSET: 'OFFSET';
ON: 'ON';
OR: 'OR';
ORDER: 'ORDER';
OUTER: 'OUTER';
POWER: 'POWER';
PRECISION: 'PRECISION';
REAL: 'REAL';
RIGHT: 'RIGHT';
SELECT: 'SELECT';
SMALLINT: 'SMALLINT';
SUM: 'SUM';
SYSDATE: 'SYSDATE';
TEXT: 'TEXT';
THEN: 'THEN';
TIME: 'TIME';
TIMESTAMP: 'TIMESTAMP';
TOP: 'TOP';
TRUE: 'TRUE';
UNION: 'UNION';
UNKNOWN: 'UNKNOWN';
UPPER: 'UPPER';
VARCHAR: 'VARCHAR';
VARYING: 'VARYING';
WHEN: 'WHEN';
WHERE: 'WHERE';
WITH: 'WITH';
WITHOUT: 'WITHOUT';
ZONE: 'ZONE';

/*
 * Words that Redshift reserves and that no rule of this grammar uses. They stay keywords so that they cannot be
 * identifiers: a query that uses one of them fails at that word with a syntax error.
 * Reference: https://docs.aws.amazon.com/redshift/latest/dg/r_pg_keywords.html
 */

ANY: 'ANY';
ARRAY: 'ARRAY';
BINARY: 'BINARY';
BOTH: 'BOTH';
COLLATE: 'COLLATE';
CURRENT_TIME: 'CURRENT_TIME';
CURRENT_TIMESTAMP: 'CURRENT_TIMESTAMP';
CURRENT_USER: 'CURRENT_USER';
CURRENT_USER_ID: 'CURRENT_USER_ID';
EXCEPT: 'EXCEPT';
FOR: 'FOR';
IGNORE: 'IGNORE';
INTERSECT: 'INTERSECT';
INTO: 'INTO';
LEADING: 'LEADING';
LOCALTIME: 'LOCALTIME';
LOCALTIMESTAMP: 'LOCALTIMESTAMP';
MINUS_KEYWORD: 'MINUS';
NATURAL: 'NATURAL';
OVERLAPS: 'OVERLAPS';
PARTITION: 'PARTITION';
PIVOT: 'PIVOT';
RESPECT: 'RESPECT';
SESSION_USER: 'SESSION_USER';
SIMILAR: 'SIMILAR';
SOME: 'SOME';
TABLE: 'TABLE';
TRAILING: 'TRAILING';
UNNEST: 'UNNEST';
UNPIVOT: 'UNPIVOT';
USER: 'USER';
USING: 'USING';

/*
 * Operators and punctuation. The operators without a rule (`%`, `|`, `<<`, `>>`, `@`, `|/`, `||/`, `~` and the POSIX
 * operators, `[` and `]`) are tokens so that a query using them fails at the operator.
 */

DOUBLE_COLON: '::';
// The root operators must not take the slash of a comment start: `a ||/* c */ b` is a concatenation.
CUBE_ROOT: '||/' {_input.LA(1) != '*'}?;
SQUARE_ROOT: '|/' {_input.LA(1) != '*'}?;
CONCAT: '||';
PIPE: '|';
EQ: '=';
NEQ: '<>' | '!=';
LT_EQ: '<=';
GT_EQ: '>=';
SHIFT_LEFT: '<<';
SHIFT_RIGHT: '>>';
LT: '<';
GT: '>';
TILDE_TILDE_ASTERISK: '~~*';
TILDE_TILDE: '~~';
TILDE_ASTERISK: '~*';
TILDE: '~';
NOT_TILDE_TILDE_ASTERISK: '!~~*';
NOT_TILDE_TILDE: '!~~';
NOT_TILDE_ASTERISK: '!~*';
NOT_TILDE: '!~';
AMPERSAND: '&';
CARET: '^';
AT_SIGN: '@';
HASH: '#';
PLUS: '+';
MINUS: '-';
ASTERISK: '*';
SLASH: '/';
PERCENT: '%';
COMMA: ',';
DOT: '.';
SEMICOLON: ';';
PAREN_LEFT: '(';
PAREN_RIGHT: ')';
BRACKET_LEFT: '[';
BRACKET_RIGHT: ']';

/*
 * Literals and identifiers
 */

INTEGER_LITERAL: DIGIT+;
DECIMAL_LITERAL: DIGIT+ '.' DIGIT* | '.' DIGIT+;
// A numeric literal with an exponent is one token (so that `1e3` is not read as `1` with the alias `e3`); no rule
// uses it, so it is a syntax error at the literal.
FLOAT_LITERAL: (DIGIT+ ('.' DIGIT*)? | '.' DIGIT+) 'E' [+-]? DIGIT+;
// A backslash escapes the next character, including a quote.
STRING_LITERAL: '\'' ( ~['\\] | '\'\'' | '\\' . )* '\'';
/*
 * A standard identifier begins with an ASCII letter, an underscore, or a UTF-8 multibyte character, and also contains
 * digits and dollar signs. A temporary table name begins with `#`; no rule uses it (a session table cannot back a
 * view), so it is a syntax error at the name.
 * https://docs.aws.amazon.com/redshift/latest/dg/r_names.html
 */
IDENTIFIER: IDENTIFIER_START IDENTIFIER_PART*;
IDENTIFIER_QUOTED: '"' ( ~'"' | '""' )* '"';
TEMP_TABLE_NAME: '#' IDENTIFIER_PART+;

fragment DIGIT: [0-9];
fragment IDENTIFIER_START: [A-Z_] | ~[\u0000-\u007F];
fragment IDENTIFIER_PART: [A-Z_0-9$] | ~[\u0000-\u007F];

/*
 * Whitespace and comments
 */

WS: [ \t\r\n]+ -> channel(HIDDEN);
COMMENT_LINE: '--' ~[\r\n]* -> channel(HIDDEN);
COMMENT_BLOCK: '/*' .*? '*/' -> channel(HIDDEN);

/*
 * Any other character is a syntax error that the parser reports with its location.
 */
UNRECOGNIZED: .;
