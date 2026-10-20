/*
 * Amazon Redshift query grammar for the Redshift-to-PartiQL frontend.
 *
 * Scope: the queries that the frontend translates: SELECT with joins, grouping, QUALIFY, and window functions, WITH, and
 * set operations. Some constructs are recognized only so that the translator can report them as unsupported with a
 * location (for example PIVOT and CONNECT BY), instead of a syntax error.
 *
 * Operator precedence follows PostgreSQL, from which Redshift derives: '.' and '[]', then '::', then unary sign,
 * then '^', then '* / %', then '+ -', then the other operators ('||', '&', '|', '<<', '>>', and the prefix operators
 * '@', '|/', '||/', '~'), then the predicates, then NOT, AND, OR.
 *
 * Reference: https://docs.aws.amazon.com/redshift/latest/dg/r_SELECT_synopsis.html
 */
grammar Redshift;

options { caseInsensitive = true; }

/*
 * A statement, with any number of trailing semicolons. A second statement is recognized only to report it.
 */
root
    : query (SEMICOLON+ query)* SEMICOLON* EOF
    ;

/*
 * Queries
 *
 * Set operations: INTERSECT binds more tightly than UNION and EXCEPT (MINUS); operators of the same precedence are
 * left-associative. ORDER BY, LIMIT, and OFFSET at the end of a query apply to the result of the set operation.
 * https://docs.aws.amazon.com/redshift/latest/dg/r_UNION.html
 */
query
    : withClause? queryTerm orderByClause? limitClause?
    ;

withClause
    : WITH RECURSIVE? commonTableExpression (COMMA commonTableExpression)*
    ;

commonTableExpression
    : name=identifier columnAliases? AS PAREN_LEFT query PAREN_RIGHT
    ;

queryTerm
    : queryPrimary                                                                      # queryTermPrimary
    | lhs=queryTerm op=INTERSECT setQuantifier? rhs=queryTerm                           # queryTermSetOp
    | lhs=queryTerm op=(UNION | EXCEPT | MINUS_KEYWORD) setQuantifier? rhs=queryTerm    # queryTermSetOp
    ;

queryPrimary
    : querySpecification                        # queryPrimarySpecification
    | PAREN_LEFT query PAREN_RIGHT              # queryPrimaryParenthesized
    ;

querySpecification
    : SELECT top=topClause? setQuantifier? selectList
      excludeClause?
      intoClause?
      fromClause?
      whereClause?
      connectByClause?
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
    : selectItem (COMMA selectItem)*
    ;

selectItem
    : ASTERISK                                  # selectItemAll
    | primaryExpr DOT ASTERISK                  # selectItemStar
    | expr (AS? alias=identifier)?              # selectItemExpr
    ;

/*
 * https://docs.aws.amazon.com/redshift/latest/dg/r_EXCLUDE_list.html
 */
excludeClause
    : EXCLUDE (identifier (COMMA identifier)* | PAREN_LEFT identifier (COMMA identifier)* PAREN_RIGHT)
    ;

/*
 * Recognized only to report it: https://docs.aws.amazon.com/redshift/latest/dg/r_SELECT_INTO.html
 */
intoClause
    : INTO (identifier | TEMP_TABLE_NAME)*
    ;

fromClause
    : FROM tableReference (COMMA tableReference)*
    ;

tableReference
    : lhs=tableReference NATURAL joinType? JOIN rhs=tablePrimary                        # tableNaturalJoin
    | lhs=tableReference CROSS JOIN rhs=tablePrimary                                  # tableCrossJoin
    | lhs=tableReference joinType? JOIN rhs=tablePrimary joinSpecification?           # tableJoin
    | lhs=tableReference PIVOT balanced tableAlias?                                     # tablePivot
    | lhs=tableReference UNPIVOT ((INCLUDE | EXCLUDE) NULLS)? balanced tableAlias?    # tableUnpivot
    | tablePrimary                                                                    # tableBase
    ;

joinType
    : INNER
    | LEFT OUTER?
    | RIGHT OUTER?
    | FULL OUTER?
    ;

joinSpecification
    : ON condition=expr                                                         # joinOn
    | USING PAREN_LEFT identifier (COMMA identifier)* PAREN_RIGHT                # joinUsing
    ;

/*
 * A subquery, a parenthesized join, UNNEST, or a table, view, common table expression, or SUPER expression
 * (`t.items AS i`). A parenthesized subquery or name is also a primary expression, so the alternatives that ANTLR
 * prefers for them come first. https://docs.aws.amazon.com/redshift/latest/dg/r_FROM_clause30.html
 */
tablePrimary
    : PAREN_LEFT query PAREN_RIGHT tableAlias?                                      # tableSubquery
    | PAREN_LEFT tableReference PAREN_RIGHT tableAlias? (AT index=identifier)?      # tableParenthesized
    | UNNEST PAREN_LEFT expr PAREN_RIGHT (WITH OFFSET)? tableAlias?                 # tableUnnest
    | UNPIVOT expr AS value=identifier (AT attribute=identifier)?                   # tableObjectUnpivot
    | TEMP_TABLE_NAME tableAlias?                                                   # tableTemporary
    | primaryExpr ASTERISK? tableAlias? (AT index=identifier)?                      # tableName
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
 * Recognized only to report it: https://docs.aws.amazon.com/redshift/latest/dg/r_CONNECT_BY_clause.html
 */
connectByClause
    : (START WITH opaque)? CONNECT BY opaque
    ;

groupByClause
    : GROUP BY ALL                                                  # groupByAll
    | GROUP BY groupingElement (COMMA groupingElement)*             # groupByList
    ;

/*
 * https://docs.aws.amazon.com/redshift/latest/dg/r_GROUP_BY_aggregation-extensions.html
 */
groupingElement
    : ROLLUP balanced                                               # groupingRollup
    | CUBE balanced                                                 # groupingCube
    | GROUPING SETS balanced                                        # groupingSets
    | PAREN_LEFT PAREN_RIGHT                                        # groupingEmpty
    | expr                                                          # groupingExpr
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

/*
 * LIMIT { count | ALL } and OFFSET start, in either order.
 * https://docs.aws.amazon.com/redshift/latest/dg/r_ORDER_BY_clause.html
 */
limitClause
    : LIMIT limit=limitValue (OFFSET offset=valueExpr)?
    | OFFSET offset=valueExpr (LIMIT limit=limitValue)?
    ;

limitValue
    : ALL
    | valueExpr
    ;

/*
 * Token sequences of constructs that are recognized only to report them: parentheses with any balanced content, and
 * a run of tokens up to the next clause keyword.
 */
balanced
    : PAREN_LEFT (~(PAREN_LEFT | PAREN_RIGHT) | balanced)* PAREN_RIGHT
    ;

opaque
    : (~(PAREN_LEFT | PAREN_RIGHT | CONNECT | GROUP | HAVING | QUALIFY | ORDER | LIMIT | OFFSET | UNION | INTERSECT
        | EXCEPT | MINUS_KEYWORD | SEMICOLON) | balanced)+
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
 * Predicates. Some forms are recognized only so that the translator can report them as unsupported with a location:
 * the quantified comparisons other than `= ANY`, `= SOME`, and `<> ALL`, `IS [NOT] DISTINCT FROM`, `BETWEEN SYMMETRIC`,
 * and the case-insensitive POSIX operators.
 * Reference: https://docs.aws.amazon.com/redshift/latest/dg/r_conditions.html
 */
predicate
    : lhs=valueExpr op=comparisonOperator quantifier=(ANY | SOME | ALL) PAREN_LEFT query PAREN_RIGHT # predicateQuantified
    | lhs=valueExpr op=comparisonOperator rhs=valueExpr                                 # predicateComparison
    | lhs=valueExpr IS NOT? NULL                                                        # predicateNull
    | lhs=valueExpr IS NOT? truth=(TRUE | FALSE | UNKNOWN)                              # predicateTruth
    | lhs=valueExpr IS NOT? DISTINCT FROM rhs=valueExpr                                 # predicateDistinct
    | lhs=valueExpr NOT? BETWEEN symmetry=(SYMMETRIC | ASYMMETRIC)? lower=valueExpr AND upper=valueExpr # predicateBetween
    | lhs=valueExpr NOT? IN PAREN_LEFT query PAREN_RIGHT                                # predicateInQuery
    | lhs=valueExpr NOT? IN PAREN_LEFT expr (COMMA expr)* PAREN_RIGHT                   # predicateInList
    | row=rowValue NOT? IN PAREN_LEFT query PAREN_RIGHT                                 # predicateRowInQuery
    | row=rowValue NOT? IN PAREN_LEFT rowValue (COMMA rowValue)* PAREN_RIGHT            # predicateRowInList
    | lhs=valueExpr NOT? op=(LIKE | ILIKE) rhs=valueExpr (ESCAPE escape=valueExpr)?     # predicateLike
    | lhs=valueExpr NOT? SIMILAR TO rhs=valueExpr (ESCAPE escape=valueExpr)?           # predicateSimilarTo
    | lhs=valueExpr op=(TILDE_TILDE | NOT_TILDE_TILDE | TILDE_TILDE_ASTERISK | NOT_TILDE_TILDE_ASTERISK)
      rhs=valueExpr                                                                     # predicateLikeOperator
    | lhs=valueExpr op=(TILDE | NOT_TILDE | TILDE_ASTERISK | NOT_TILDE_ASTERISK) rhs=valueExpr # predicatePosix
    | valueExpr                                                                         # predicateBase
    ;

comparisonOperator
    : EQ | NEQ | LT | LT_EQ | GT | GT_EQ
    ;

/*
 * A row value of two or more expressions, the left side of a row-value IN predicate.
 */
rowValue
    : PAREN_LEFT expr (COMMA expr)+ PAREN_RIGHT
    ;

/*
 * The operators without a keyword (`||`, `&`, `|`, `<<`, `>>`) and the prefix operators (`@`, `|/`, `||/`, `~`) have one
 * precedence level, below `+` and `-`, as in PostgreSQL.
 * Reference: https://docs.aws.amazon.com/redshift/latest/dg/r_OPERATOR_SYMBOLS.html
 */
valueExpr
    : primaryExpr                                               # valuePrimary
    | valueExpr DOUBLE_COLON dataType                           # valueCastShorthand
    | sign=(PLUS | MINUS) valueExpr                             # valueSigned
    | valueExpr AT TIME ZONE zone=primaryExpr                   # valueAtTimeZone
    | lhs=valueExpr op=CARET rhs=valueExpr                      # valueExponent
    | lhs=valueExpr op=(ASTERISK | SLASH | PERCENT) rhs=valueExpr # valueMultiplicative
    | lhs=valueExpr op=(PLUS | MINUS) rhs=valueExpr             # valueAdditive
    | op=(AT_SIGN | SQUARE_ROOT | CUBE_ROOT | TILDE) valueExpr  # valuePrefixOperator
    | lhs=valueExpr op=(CONCAT | AMPERSAND | PIPE | SHIFT_LEFT | SHIFT_RIGHT) rhs=valueExpr # valueOperator
    ;

primaryExpr
    : primaryExpr DOT identifier                                # pathField
    | primaryExpr BRACKET_LEFT expr BRACKET_RIGHT               # pathElement
    | atom                                                      # primaryAtom
    ;

atom
    : literal                                                                       # atomLiteral
    | CAST PAREN_LEFT expr AS dataType PAREN_RIGHT                                  # atomCast
    | TRY_CAST PAREN_LEFT expr AS dataType PAREN_RIGHT                              # atomTryCast
    | CONVERT PAREN_LEFT dataType COMMA expr PAREN_RIGHT                            # atomConvert
    | CASE operand=expr? caseWhen+ (ELSE otherwise=expr)? END                       # atomCase
    | EXISTS PAREN_LEFT query PAREN_RIGHT                                           # atomExists
    | name=(DATEADD | DATEDIFF) PAREN_LEFT part=datePart COMMA expr COMMA expr PAREN_RIGHT # atomDateFunction
    | EXTRACT PAREN_LEFT part=datePart FROM expr PAREN_RIGHT                        # atomExtract
    | POSITION PAREN_LEFT sub=valueExpr IN str=valueExpr PAREN_RIGHT                # atomPosition
    | SUBSTRING PAREN_LEFT value=expr FROM start=expr (FOR length=expr)? PAREN_RIGHT # atomSubstringFrom
    | SUBSTRING PAREN_LEFT value=expr FOR length=expr PAREN_RIGHT                   # atomSubstringFor
    | TRIM PAREN_LEFT spec=(BOTH | LEADING | TRAILING)? (chars=expr? FROM)? value=expr PAREN_RIGHT # atomTrim
    | APPROXIMATE atom                                                              # atomApproximate
    | COUNT PAREN_LEFT ASTERISK PAREN_RIGHT overClause?                             # atomCountStar
    | name=functionName PAREN_LEFT (setQuantifier? expr (COMMA expr)*)? PAREN_RIGHT
      withinGroup? nullTreatment? overClause?                                       # atomFunctionCall
    | SYSDATE                                                                       # atomSysdate
    | CURRENT_DATE                                                                  # atomCurrentDate
    | name=(CURRENT_TIMESTAMP | CURRENT_TIME | LOCALTIMESTAMP | LOCALTIME)          # atomCurrentDatetime
    | name=(CURRENT_USER | CURRENT_USER_ID | SESSION_USER | USER)                   # atomSessionUser
    | PAREN_LEFT query PAREN_RIGHT                                                  # atomSubquery
    | PAREN_LEFT expr PAREN_RIGHT                                                   # atomParenthesized
    | identifier                                                                    # atomVariable
    ;

/*
 * A date part of DATEADD, DATEDIFF, and EXTRACT: a name (`day`) or a character string (`'day'`).
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
 * Window functions, ordered-set aggregates, and approximate aggregates. The query translation decides which of
 * them have a PartiQL equivalent.
 */
nullTreatment
    : (IGNORE | RESPECT) NULLS
    ;

withinGroup
    : WITHIN GROUP PAREN_LEFT ORDER BY sortItem (COMMA sortItem)* PAREN_RIGHT
    ;

overClause
    : OVER PAREN_LEFT
      (PARTITION BY partition+=expr (COMMA partition+=expr)*)?
      (ORDER BY sortItem (COMMA sortItem)*)?
      frameClause?
      PAREN_RIGHT
    ;

frameClause
    : unit=(ROWS | RANGE) (frameBound | BETWEEN frameBound AND frameBound)
    ;

frameBound
    : UNBOUNDED (PRECEDING | FOLLOWING)
    | CURRENT ROW
    | INTEGER_LITERAL (PRECEDING | FOLLOWING)
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
    | FLOAT_LITERAL                             # literalFloat
    | STRING_LITERAL                            # literalString
    | DATE STRING_LITERAL                       # literalDate
    | TIME (WITHOUT TIME ZONE)? STRING_LITERAL                          # literalTime
    | (TIME WITH TIME ZONE | TIMETZ) STRING_LITERAL                     # literalTimeTz
    | TIMESTAMP (WITHOUT TIME ZONE)? STRING_LITERAL                     # literalTimestamp
    | (TIMESTAMP WITH TIME ZONE | TIMESTAMPTZ) STRING_LITERAL           # literalTimestampTz
    | INTERVAL STRING_LITERAL intervalQualifier?                        # literalInterval
    ;

/*
 * An interval qualifier: `YEAR`, `DAY TO SECOND`, `SECOND(3)`, ...
 */
intervalQualifier
    : start=intervalField (PAREN_LEFT precision=INTEGER_LITERAL PAREN_RIGHT)?
      (TO end=intervalField (PAREN_LEFT fraction=INTEGER_LITERAL PAREN_RIGHT)?)?
    ;

intervalField
    : YEAR | MONTH | DAY | HOUR | MINUTE | SECOND
    ;

/*
 * A data type is a name with optional parameters. The translator maps the name to a PartiQL type. The multi-word
 * names are listed explicitly; they come first, so that `CHAR VARYING` is one type and not the type CHAR followed by
 * the alias `varying`.
 * Reference: https://docs.aws.amazon.com/redshift/latest/dg/c_Supported_data_types.html
 */
dataType
    : DOUBLE PRECISION                                                      # typeDoublePrecision
    | (CHARACTER | CHAR | NCHAR | NATIONAL (CHARACTER | CHAR)) VARYING length=typeParameters? # typeCharacterVarying
    | NATIONAL (CHARACTER | CHAR) length=typeParameters?                    # typeNationalCharacter
    | BINARY VARYING? typeParameters?                                       # typeBinary
    | TIME (WITHOUT TIME ZONE)?                                             # typeTime
    | (TIME WITH TIME ZONE | TIMETZ)                                        # typeTimeTz
    | TIMESTAMP (WITHOUT TIME ZONE)?                                        # typeTimestamp
    | (TIMESTAMP WITH TIME ZONE | TIMESTAMPTZ)                              # typeTimestampTz
    | DATE                                                                  # typeDate
    | INTERVAL intervalQualifier?                                           # typeInterval
    | name=identifier typeParameters?                                       # typeNamed
    ;

/*
 * The parameters of a data type: a length, a precision and a scale, or MAX.
 */
typeParameters
    : PAREN_LEFT arg0=typeParameter (COMMA arg1=typeParameter)? PAREN_RIGHT
    ;

typeParameter
    : INTEGER_LITERAL
    | MAX
    ;

/*
 * An identifier. The reserved niladic functions (USER, ...) are listed so that the translator can report them where
 * Redshift reads them as a function, instead of a syntax error; they are not names.
 */
identifier
    : IDENTIFIER                                # identifierUnquoted
    | IDENTIFIER_QUOTED                         # identifierQuoted
    | nonReserved                               # identifierNonReserved
    | name=(USER | CURRENT_USER | CURRENT_USER_ID | SESSION_USER) # identifierNiladic
    ;

/*
 * Keywords of this grammar that Redshift does not reserve, so they can also be identifiers.
 * Reference: https://docs.aws.amazon.com/redshift/latest/dg/r_pg_keywords.html
 */
nonReserved
    : APPROXIMATE | ASYMMETRIC | AT | CHAR | CHARACTER | CONNECT | CONVERT | COUNT | CUBE | CURRENT | DATE | DATEADD
    | DATEDIFF | DAY | DOUBLE | ESCAPE | EXCLUDE | EXISTS | EXTRACT | FIRST | FOLLOWING | GROUPING | HOUR | INCLUDE
    | LAST | MAX | MINUTE | MONTH | NATIONAL | NCHAR | OVER | POSITION | PRECEDING | PRECISION | QUALIFY | RANGE
    | RECURSIVE | ROLLUP | ROW | ROWS | SECOND | SETS | START | SUBSTRING | SYMMETRIC | TIME | TIMESTAMPTZ | TIMETZ
    | TRIM | TRY_CAST | UNBOUNDED | UNKNOWN | VARYING | WITHIN | YEAR | ZONE
    ;

/*
 * Keywords
 */

ALL: 'ALL';
AND: 'AND';
ANY: 'ANY';
APPROXIMATE: 'APPROXIMATE';
AS: 'AS';
ASC: 'ASC';
ASYMMETRIC: 'ASYMMETRIC';
AT: 'AT';
BETWEEN: 'BETWEEN';
BINARY: 'BINARY';
BOTH: 'BOTH';
BY: 'BY';
CASE: 'CASE';
CAST: 'CAST';
CHAR: 'CHAR';
CHARACTER: 'CHARACTER';
CONVERT: 'CONVERT';
COUNT: 'COUNT';
CROSS: 'CROSS';
CURRENT: 'CURRENT';
CURRENT_DATE: 'CURRENT_DATE';
CURRENT_TIME: 'CURRENT_TIME';
CURRENT_TIMESTAMP: 'CURRENT_TIMESTAMP';
CURRENT_USER: 'CURRENT_USER';
CURRENT_USER_ID: 'CURRENT_USER_ID';
DATE: 'DATE';
DATEADD: 'DATEADD';
DATEDIFF: 'DATEDIFF';
DAY: 'DAY';
DESC: 'DESC';
DISTINCT: 'DISTINCT';
DOUBLE: 'DOUBLE';
ELSE: 'ELSE';
END: 'END';
ESCAPE: 'ESCAPE';
EXISTS: 'EXISTS';
EXTRACT: 'EXTRACT';
FALSE: 'FALSE';
FIRST: 'FIRST';
FOLLOWING: 'FOLLOWING';
FOR: 'FOR';
FROM: 'FROM';
FULL: 'FULL';
GROUP: 'GROUP';
HAVING: 'HAVING';
HOUR: 'HOUR';
IGNORE: 'IGNORE';
ILIKE: 'ILIKE';
IN: 'IN';
INNER: 'INNER';
INTERVAL: 'INTERVAL';
IS: 'IS';
JOIN: 'JOIN';
LAST: 'LAST';
LEADING: 'LEADING';
LEFT: 'LEFT';
LIKE: 'LIKE';
LIMIT: 'LIMIT';
LOCALTIME: 'LOCALTIME';
LOCALTIMESTAMP: 'LOCALTIMESTAMP';
MAX: 'MAX';
MINUTE: 'MINUTE';
MONTH: 'MONTH';
NATIONAL: 'NATIONAL';
NCHAR: 'NCHAR';
NOT: 'NOT';
NULL: 'NULL';
NULLS: 'NULLS';
OFFSET: 'OFFSET';
ON: 'ON';
OR: 'OR';
ORDER: 'ORDER';
OUTER: 'OUTER';
OVER: 'OVER';
PARTITION: 'PARTITION';
POSITION: 'POSITION';
PRECEDING: 'PRECEDING';
PRECISION: 'PRECISION';
QUALIFY: 'QUALIFY';
RANGE: 'RANGE';
RESPECT: 'RESPECT';
RIGHT: 'RIGHT';
ROW: 'ROW';
ROWS: 'ROWS';
SECOND: 'SECOND';
SELECT: 'SELECT';
SESSION_USER: 'SESSION_USER';
SIMILAR: 'SIMILAR';
SOME: 'SOME';
SUBSTRING: 'SUBSTRING';
SYMMETRIC: 'SYMMETRIC';
SYSDATE: 'SYSDATE';
THEN: 'THEN';
TIME: 'TIME';
TIMESTAMP: 'TIMESTAMP';
TIMESTAMPTZ: 'TIMESTAMPTZ';
TIMETZ: 'TIMETZ';
TO: 'TO';
TOP: 'TOP';
TRAILING: 'TRAILING';
TRIM: 'TRIM';
TRUE: 'TRUE';
TRY_CAST: 'TRY_CAST';
UNBOUNDED: 'UNBOUNDED';
UNKNOWN: 'UNKNOWN';
USER: 'USER';
VARYING: 'VARYING';
WHEN: 'WHEN';
WHERE: 'WHERE';
WITH: 'WITH';
WITHIN: 'WITHIN';
WITHOUT: 'WITHOUT';
YEAR: 'YEAR';
ZONE: 'ZONE';

/*
 * Keywords of the query clauses (set operations, WITH, FROM, GROUP BY).
 */

CONNECT: 'CONNECT';
CUBE: 'CUBE';
EXCEPT: 'EXCEPT';
EXCLUDE: 'EXCLUDE';
GROUPING: 'GROUPING';
INCLUDE: 'INCLUDE';
INTERSECT: 'INTERSECT';
INTO: 'INTO';
MINUS_KEYWORD: 'MINUS';
NATURAL: 'NATURAL';
PIVOT: 'PIVOT';
RECURSIVE: 'RECURSIVE';
ROLLUP: 'ROLLUP';
SETS: 'SETS';
START: 'START';
UNION: 'UNION';
UNNEST: 'UNNEST';
UNPIVOT: 'UNPIVOT';
USING: 'USING';

/*
 * Operators and punctuation
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
// A numeric literal with an exponent is one token, so that `1e3` is not read as `1` with the alias `e3`.
FLOAT_LITERAL: (DIGIT+ ('.' DIGIT*)? | '.' DIGIT+) 'E' [+-]? DIGIT+;
// A backslash escapes the next character, including a quote; see RedshiftExpressions.stringValue.
STRING_LITERAL: '\'' ( ~['\\] | '\'\'' | '\\' . )* '\'';
/*
 * A standard identifier begins with an ASCII letter, an underscore, or a UTF-8 multibyte character, and also contains
 * digits and dollar signs. A temporary table name begins with `#`.
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
