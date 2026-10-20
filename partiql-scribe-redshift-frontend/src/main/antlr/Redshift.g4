/*
 * Amazon Redshift query grammar for the Redshift-to-PartiQL frontend.
 *
 * Scope: the SELECT-FROM-WHERE subset that the frontend translates. Some constructs are recognized only so that the
 * translator can report them as unsupported with a location (for example QUALIFY and ILIKE), instead of a syntax error.
 *
 * Operator precedence follows PostgreSQL, from which Redshift derives: '.' and '[]', then '::', then unary sign,
 * then '* / %', then '+ -', then '||', then the predicates, then NOT, AND, OR.
 *
 * Reference: https://docs.aws.amazon.com/redshift/latest/dg/r_SELECT_synopsis.html
 */
grammar Redshift;

options { caseInsensitive = true; }

root
    : query SEMICOLON? EOF
    ;

query
    : querySpecification orderByClause? limitClause?
    ;

querySpecification
    : SELECT top=topClause? setQuantifier? selectList
      fromClause?
      whereClause?
      groupByClause?
      havingClause?
      qualifyClause?
    ;

topClause
    : TOP INTEGER_LITERAL
    ;

setQuantifier
    : DISTINCT
    | ALL
    ;

selectList
    : ASTERISK                                  # selectAll
    | selectItem (COMMA selectItem)*            # selectItems
    ;

selectItem
    : expr (AS? alias=identifier)?              # selectItemExpr
    | primaryExpr DOT ASTERISK                  # selectItemStar
    ;

fromClause
    : FROM tableReference (COMMA tableReference)*
    ;

tableReference
    : lhs=tableReference joinType? JOIN rhs=tablePrimary ON condition=expr   # tableJoin
    | lhs=tableReference CROSS JOIN rhs=tablePrimary                       # tableCrossJoin
    | tablePrimary                                                         # tableBase
    ;

joinType
    : INNER
    | LEFT OUTER?
    | RIGHT OUTER?
    | FULL OUTER?
    ;

tablePrimary
    : primaryExpr (AS? alias=identifier)?                        # tableName
    | PAREN_LEFT query PAREN_RIGHT AS? alias=identifier          # tableSubquery
    ;

whereClause
    : WHERE expr
    ;

groupByClause
    : GROUP BY expr (COMMA expr)*
    ;

havingClause
    : HAVING expr
    ;

qualifyClause
    : QUALIFY expr
    ;

orderByClause
    : ORDER BY sortItem (COMMA sortItem)*
    ;

sortItem
    : expr dir=(ASC | DESC)? (NULLS nulls=(FIRST | LAST))?
    ;

limitClause
    : LIMIT limit=valueExpr (OFFSET offset=valueExpr)?
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

predicate
    : lhs=valueExpr op=comparisonOperator rhs=valueExpr                                 # predicateComparison
    | lhs=valueExpr IS NOT? NULL                                                        # predicateNull
    | lhs=valueExpr NOT? BETWEEN lower=valueExpr AND upper=valueExpr                    # predicateBetween
    | lhs=valueExpr NOT? IN PAREN_LEFT query PAREN_RIGHT                                # predicateInQuery
    | lhs=valueExpr NOT? IN PAREN_LEFT expr (COMMA expr)* PAREN_RIGHT                   # predicateInList
    | lhs=valueExpr NOT? op=(LIKE | ILIKE) rhs=valueExpr (ESCAPE escape=valueExpr)?     # predicateLike
    | valueExpr                                                                         # predicateBase
    ;

comparisonOperator
    : EQ | NEQ | LT | LT_EQ | GT | GT_EQ
    ;

valueExpr
    : primaryExpr                                               # valuePrimary
    | valueExpr DOUBLE_COLON dataType                           # valueCastShorthand
    | sign=(PLUS | MINUS) valueExpr                             # valueSigned
    | lhs=valueExpr op=(ASTERISK | SLASH | PERCENT) rhs=valueExpr # valueMultiplicative
    | lhs=valueExpr op=(PLUS | MINUS) rhs=valueExpr             # valueAdditive
    | lhs=valueExpr CONCAT rhs=valueExpr                        # valueConcat
    ;

primaryExpr
    : primaryExpr DOT identifier                                # pathField
    | primaryExpr BRACKET_LEFT expr BRACKET_RIGHT               # pathElement
    | atom                                                      # primaryAtom
    ;

atom
    : literal                                                                       # atomLiteral
    | CAST PAREN_LEFT expr AS dataType PAREN_RIGHT                                  # atomCast
    | CASE operand=expr? caseWhen+ (ELSE otherwise=expr)? END                       # atomCase
    | name=(DATEADD | DATEDIFF) PAREN_LEFT part=identifier COMMA expr COMMA expr PAREN_RIGHT # atomDateFunction
    | EXTRACT PAREN_LEFT part=datePart FROM expr PAREN_RIGHT                        # atomExtract
    | COUNT PAREN_LEFT ASTERISK PAREN_RIGHT                                         # atomCountStar
    | name=functionName PAREN_LEFT (setQuantifier? expr (COMMA expr)*)? PAREN_RIGHT # atomFunctionCall
    | SYSDATE                                                                       # atomSysdate
    | CURRENT_DATE                                                                  # atomCurrentDate
    | PAREN_LEFT query PAREN_RIGHT                                                  # atomSubquery
    | PAREN_LEFT expr PAREN_RIGHT                                                   # atomParenthesized
    | identifier                                                                    # atomVariable
    ;

datePart
    : identifier
    | STRING_LITERAL
    ;

caseWhen
    : WHEN condition=expr THEN result=expr
    ;

functionName
    : identifier
    | LEFT
    | RIGHT
    ;

literal
    : NULL                                      # literalNull
    | TRUE                                      # literalTrue
    | FALSE                                     # literalFalse
    | INTEGER_LITERAL                           # literalInteger
    | DECIMAL_LITERAL                           # literalDecimal
    | STRING_LITERAL                            # literalString
    | DATE STRING_LITERAL                       # literalDate
    | TIME (WITHOUT TIME ZONE)? STRING_LITERAL                          # literalTime
    | (TIME WITH TIME ZONE | TIMETZ) STRING_LITERAL                     # literalTimeTz
    | TIMESTAMP (WITHOUT TIME ZONE)? STRING_LITERAL                     # literalTimestamp
    | (TIMESTAMP WITH TIME ZONE | TIMESTAMPTZ) STRING_LITERAL           # literalTimestampTz
    ;

/*
 * A data type is a name with optional parameters. The translator maps the name to a PartiQL type. The multi-word
 * names are listed explicitly.
 */
dataType
    : DOUBLE PRECISION                                                      # typeDoublePrecision
    | CHARACTER VARYING (PAREN_LEFT length=INTEGER_LITERAL PAREN_RIGHT)?    # typeCharacterVarying
    | TIME (WITHOUT TIME ZONE)?                                             # typeTime
    | (TIME WITH TIME ZONE | TIMETZ)                                        # typeTimeTz
    | TIMESTAMP (WITHOUT TIME ZONE)?                                        # typeTimestamp
    | (TIMESTAMP WITH TIME ZONE | TIMESTAMPTZ)                              # typeTimestampTz
    | DATE                                                                  # typeDate
    | name=identifier (PAREN_LEFT arg0=INTEGER_LITERAL (COMMA arg1=INTEGER_LITERAL)? PAREN_RIGHT)? # typeNamed
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
    : CHARACTER | COUNT | DATE | DATEADD | DATEDIFF | DOUBLE | ESCAPE | EXTRACT | FIRST | LAST | NULLS | PRECISION
    | TIME | TIMESTAMP | TIMESTAMPTZ | TIMETZ | VARYING | WITHOUT | ZONE
    ;

/*
 * Keywords
 */

ALL: 'ALL';
AND: 'AND';
AS: 'AS';
ASC: 'ASC';
BETWEEN: 'BETWEEN';
BY: 'BY';
CASE: 'CASE';
CAST: 'CAST';
CHARACTER: 'CHARACTER';
COUNT: 'COUNT';
CROSS: 'CROSS';
CURRENT_DATE: 'CURRENT_DATE';
DATE: 'DATE';
DATEADD: 'DATEADD';
DATEDIFF: 'DATEDIFF';
DESC: 'DESC';
DISTINCT: 'DISTINCT';
DOUBLE: 'DOUBLE';
ELSE: 'ELSE';
END: 'END';
ESCAPE: 'ESCAPE';
EXTRACT: 'EXTRACT';
FALSE: 'FALSE';
FIRST: 'FIRST';
FROM: 'FROM';
FULL: 'FULL';
GROUP: 'GROUP';
HAVING: 'HAVING';
ILIKE: 'ILIKE';
IN: 'IN';
INNER: 'INNER';
IS: 'IS';
JOIN: 'JOIN';
LAST: 'LAST';
LEFT: 'LEFT';
LIKE: 'LIKE';
LIMIT: 'LIMIT';
NOT: 'NOT';
NULL: 'NULL';
NULLS: 'NULLS';
OFFSET: 'OFFSET';
ON: 'ON';
OR: 'OR';
ORDER: 'ORDER';
OUTER: 'OUTER';
PRECISION: 'PRECISION';
QUALIFY: 'QUALIFY';
RIGHT: 'RIGHT';
SELECT: 'SELECT';
SYSDATE: 'SYSDATE';
THEN: 'THEN';
TIME: 'TIME';
TIMESTAMP: 'TIMESTAMP';
TIMESTAMPTZ: 'TIMESTAMPTZ';
TIMETZ: 'TIMETZ';
TOP: 'TOP';
TRUE: 'TRUE';
VARYING: 'VARYING';
WHEN: 'WHEN';
WHERE: 'WHERE';
WITH: 'WITH';
WITHOUT: 'WITHOUT';
ZONE: 'ZONE';

/*
 * Operators and punctuation
 */

DOUBLE_COLON: '::';
CONCAT: '||';
EQ: '=';
NEQ: '<>' | '!=';
LT_EQ: '<=';
GT_EQ: '>=';
LT: '<';
GT: '>';
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
STRING_LITERAL: '\'' ( ~'\'' | '\'\'' )* '\'';
IDENTIFIER: [A-Z_] [A-Z_0-9$]*;
IDENTIFIER_QUOTED: '"' ( ~'"' | '""' )* '"';

fragment DIGIT: [0-9];

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
