package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprAnd
import org.partiql.ast.Ast.exprArray
import org.partiql.ast.Ast.exprBetween
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCase
import org.partiql.ast.Ast.exprCaseBranch
import org.partiql.ast.Ast.exprCoalesce
import org.partiql.ast.Ast.exprInCollection
import org.partiql.ast.Ast.exprIsType
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprNullPredicate
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.exprOr
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.exprStruct
import org.partiql.ast.Ast.exprStructField
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.from
import org.partiql.ast.Ast.fromExpr
import org.partiql.ast.Ast.orderBy
import org.partiql.ast.Ast.queryBodySFW
import org.partiql.ast.Ast.selectItemExpr
import org.partiql.ast.Ast.selectList
import org.partiql.ast.Ast.selectValue
import org.partiql.ast.Ast.sort
import org.partiql.ast.AstNode
import org.partiql.ast.AstRewriter
import org.partiql.ast.DataType
import org.partiql.ast.FromTableRef
import org.partiql.ast.FromType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.Nulls
import org.partiql.ast.Order
import org.partiql.ast.QueryBody
import org.partiql.ast.SetQuantifier
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprCoalesce
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.ExprVarRef
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isDatetime
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isDecimal
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isDynamic
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isInteger
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isText
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isUnknown
import org.partiql.scribe.frontend.redshift.internal.RedshiftFunctions.Rule
import org.partiql.spi.types.PType
import java.math.BigInteger
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * The rules that translate the Redshift functions on SUPER values; see [RedshiftFunctions].
 *
 * Redshift navigates SUPER values laxly: a missing attribute or element is NULL. In PartiQL it is MISSING, which a
 * constructor drops (`[MISSING]` and `{'a': MISSING}` differ from `[NULL]` and `{'a': NULL}`). So the constructors wrap
 * an element of the dynamic type in `COALESCE(x)`, which is NULL for MISSING. The type tests use the PartiQL type
 * predicates (`x IS LIST`), which are NULL for NULL, where Redshift returns false.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/c_Type_Info_functions.html
 * https://docs.aws.amazon.com/redshift/latest/dg/c_Array_Functions.html
 */
internal class RedshiftSuperFunctions(
    private val functions: RedshiftFunctions,
) {
    val rules: Map<String, Rule> =
        mapOf(
            "array" to Rule(::array),
            "object" to Rule(::obj),
            "get_array_length" to Rule { name, args, setq -> cardinality(name, args, setq, DataType.LIST(), "LIST") },
            "get_number_attributes" to Rule { name, args, setq -> cardinality(name, args, setq, DataType.STRUCT(), "STRUCT") },
            "array_contains" to Rule(::arrayContains),
            "arrays_overlap" to Rule(::arraysOverlap),
            "array_distinct" to Rule(::arrayDistinct),
            "array_sort" to Rule(::arraySort),
            "array_union" to Rule(::arrayUnion),
            "array_intersection" to Rule(::arrayIntersection),
            "is_array" to typeTest("x IS LIST") { isType(it, DataType.LIST()) },
            "is_object" to typeTest("x IS STRUCT") { isType(it, DataType.STRUCT()) },
            "is_boolean" to typeTest("x IS BOOL") { isType(it, DataType.BOOL()) },
            // IS FLOAT is the PartiQL REAL test, which is false for negative values and 0.
            "is_float" to typeTest("x IS DOUBLE PRECISION") { isType(it, DataType.DOUBLE_PRECISION()) },
            // IS DECIMAL is true for DECIMAL values only, and Redshift IS_DECIMAL is also true for integers.
            "is_decimal" to typeTest("x IS DECIMAL OR x IS INT") { exprOr(isType(it, DataType.DECIMAL()), isType(it, DataType.INT())) },
            "is_scalar" to Rule(::isScalar),
            "is_smallint" to Rule { name, args, setq -> isInteger(name, args, setq, Short.MIN_VALUE.toLong(), Short.MAX_VALUE.toLong()) },
            "is_integer" to Rule { name, args, setq -> isInteger(name, args, setq, Int.MIN_VALUE.toLong(), Int.MAX_VALUE.toLong()) },
            "is_bigint" to Rule { name, args, setq -> isInteger(name, args, setq, Long.MIN_VALUE, Long.MAX_VALUE) },
            "is_varchar" to Rule(::isVarchar),
            "is_char" to Rule(::isChar),
            // https://docs.aws.amazon.com/redshift/latest/dg/r_json_typeof.html
            "json_typeof" to functions.unsupported("the result for a NULL value is not documented"),
            // https://docs.aws.amazon.com/redshift/latest/dg/split_to_array.html
            "split_to_array" to functions.unsupported("the result for an empty string is not documented"),
            "array_concat" to functions.unsupported("PartiQL has no array concatenation that keeps the order"),
            "array_flatten" to functions.unsupported("PartiQL has no array flattening that keeps the order"),
            "array_except" to functions.unsupported(NO_POSITIONS),
            "array_position" to functions.unsupported(NO_POSITIONS),
            "array_positions" to functions.unsupported(NO_POSITIONS),
            "subarray" to functions.unsupported(NO_POSITIONS),
            "object_transform" to functions.unsupported("PartiQL has no object transformation"),
            "lower_attribute_names" to functions.unsupported(NO_ATTRIBUTE_NAMES),
            "upper_attribute_names" to functions.unsupported(NO_ATTRIBUTE_NAMES),
            "decimal_precision" to functions.unsupported("PartiQL has no function for the precision of a value"),
            "decimal_scale" to functions.unsupported("PartiQL has no function for the scale of a value"),
            "size" to functions.unsupported("PartiQL SIZE is the number of elements, not the size in bytes"),
            "json_size" to functions.unsupported(NO_JSON),
            "json_parse" to functions.unsupported(NO_JSON),
            "can_json_parse" to functions.unsupported(NO_JSON),
            "json_serialize" to functions.unsupported(NO_JSON),
            "json_serialize_to_varbyte" to functions.unsupported(NO_JSON),
            "is_valid_json" to functions.unsupported(NO_JSON),
            "is_valid_json_array" to functions.unsupported(NO_JSON),
            "json_array_length" to functions.unsupported(NO_JSON),
            "json_extract_array_element_text" to functions.unsupported(NO_JSON),
            "json_extract_path_text" to functions.unsupported(NO_JSON),
        )

    /*
     * Constructors
     */

    /**
     * `ARRAY(e1, ..., en)` is the PartiQL array constructor `[e1, ..., en]`. An element of the dynamic type is wrapped in
     * `COALESCE(x)`, so that a missing value is NULL, as in Redshift. Redshift does not convert datetime values to SUPER,
     * and the conversion of a CHAR(n) value (with or without the padding) is not documented.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_array.html
     */
    private fun array(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.scalar(name, setq)
        functions.note(name, "Redshift `ARRAY(...)` was replaced by the PartiQL array constructor `[...]`")
        return functions.resolve(args) { exprs, types -> exprArray(exprs.indices.map { element(name, args[it], exprs[it], types[it]) }) }
    }

    /**
     * `OBJECT(k1, v1, ..., kn, vn)` is the PartiQL struct constructor `{k1: v1, ..., kn: vn}`, for string literal keys
     * that are all different, and values that cannot be NULL (literals and constructors): Redshift does not document
     * whether an object keeps an attribute whose value is NULL, or what duplicate keys do.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_object_function.html
     */
    private fun obj(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.scalar(name, setq)
        if (args.size % 2 != 0) throw UnsupportedConstructException.at(name, "`OBJECT` with an odd number of arguments")
        val keys = args.indices.filter { it % 2 == 0 }.map { functions.stringLiteral(args[it].expr) }
        if (keys.any { it == null }) throw UnsupportedConstructException.at(name, "`OBJECT` with a key that is not a string literal")
        if (keys.toSet().size != keys.size) throw UnsupportedConstructException.at(name, "`OBJECT` with duplicate keys")
        val values = args.indices.filter { it % 2 == 1 }.map { args[it] }
        if (values.any { !nonNull(it.ctx) }) {
            throw UnsupportedConstructException.at(
                name,
                "`OBJECT` with a value that can be NULL (Redshift does not document whether it keeps the attribute)",
            )
        }
        functions.note(name, "Redshift `OBJECT(...)` was replaced by the PartiQL struct constructor `{...}`")
        return functions.resolve(values) { exprs, types ->
            exprStruct(
                keys.indices.map { exprStructField(exprLit(Literal.string(keys[it]!!)), element(name, values[it], exprs[it], types[it])) },
            )
        }
    }

    /**
     * True if [ctx] cannot be NULL: a literal other than NULL, an ARRAY or OBJECT constructor, or arithmetic, a
     * concatenation, or a CAST of such operands.
     */
    private fun nonNull(ctx: ParserRuleContext): Boolean =
        when (val inner = functions.unwrap(ctx)) {
            is P.AtomLiteralContext -> inner.literal() !is P.LiteralNullContext
            is P.AtomFunctionCallContext -> inner.name.text.lowercase() in setOf("array", "object")
            is P.ValueSignedContext -> nonNull(inner.valueExpr())
            is P.ValueAdditiveContext -> nonNull(inner.lhs) && nonNull(inner.rhs)
            is P.ValueMultiplicativeContext -> nonNull(inner.lhs) && nonNull(inner.rhs)
            is P.ValueOperatorContext -> inner.op.type == P.CONCAT && nonNull(inner.lhs) && nonNull(inner.rhs)
            is P.ValueCastShorthandContext -> nonNull(inner.valueExpr())
            is P.AtomCastContext -> nonNull(inner.expr())
            else -> false
        }

    /** An element of an ARRAY or OBJECT constructor; see [array]. */
    private fun element(
        name: Token,
        operand: Operand,
        x: Expr,
        type: PType,
    ): Expr =
        when {
            type.code() == PType.CHAR -> throw UnsupportedConstructException.at(name, "`${name.text.uppercase()}` with a CHAR argument")
            isDatetime(type) ->
                throw UnsupportedConstructException.at(
                    name,
                    "`${name.text.uppercase()}` with a ${FunctionTypes.name(type)} argument (Redshift does not convert it to SUPER)",
                )
            // A SUPER path or an array subscript out of range is MISSING, which Redshift returns as NULL.
            (isDynamic(type) || containsSubscript(operand.expr, functions)) && operand.expr !is ExprCoalesce -> exprCoalesce(listOf(x))
            else -> x
        }

    /*
     * Arrays and objects
     */

    /**
     * `GET_ARRAY_LENGTH(x)` is the number of elements of an array, and `GET_NUMBER_ATTRIBUTES(x)` the number of
     * attributes of an object: `CASE WHEN x IS LIST THEN CARDINALITY(x) END` (IS STRUCT for objects). PartiQL CARDINALITY
     * also counts the attributes of an object and the elements of an array, so the type test is needed.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/get_array_length.html
     * https://docs.aws.amazon.com/redshift/latest/dg/get_number_attributes.html
     */
    private fun cardinality(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
        type: DataType,
        typeName: String,
    ): Expr {
        functions.arity(name, args, 1)
        functions.scalar(name, setq)
        functions.note(name, "Redshift `${name.text.uppercase()}(x)` was replaced by `CASE WHEN x IS $typeName THEN CARDINALITY(x) END`")
        return functions.resolve(args) { exprs, _ ->
            functions.nonMissing(
                exprCase(null, listOf(exprCaseBranch(isType(exprs[0], type), functions.fn("cardinality", exprs[0]))), null),
            )
        }
    }

    /**
     * `ARRAY_CONTAINS(a, v [, null_match])` is true if the array `a` contains `v`. With `null_match` TRUE (the default),
     * NULL values are searchable and the result is true or false; with FALSE, a NULL `v`, or no match in an array with
     * NULL elements, gives NULL, as PartiQL `v IN a` does. `null_match` must be a literal.
     *
     * ```
     * CASE WHEN a IS LIST THEN COALESCE(v IN a, false) END                         -- TRUE, v a literal
     * CASE WHEN a IS LIST THEN CASE WHEN v IS NULL THEN (SELECT COUNT(*) FROM a AS e WHERE e IS NULL) > 0
     *                               ELSE COALESCE(v IN a, false) END END           -- TRUE
     * CASE WHEN a IS LIST THEN v IN a END                                          -- FALSE
     * ```
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/array_contains.html
     */
    private fun arrayContains(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 2..3)
        functions.scalar(name, setq)
        val nullMatch =
            if (args.size == 3) {
                functions.boolLiteral(args[2].expr)
                    ?: throw UnsupportedConstructException.at(name, "`ARRAY_CONTAINS` with a non-literal null_match argument")
            } else {
                true
            }
        val literal = args[1].expr is ExprLit && !functions.isNullLiteral(args[1].expr)
        functions.note(name, "Redshift `ARRAY_CONTAINS` was replaced by a NULL-safe membership test over the array")
        return functions.resolve(args.take(2)) { exprs, types ->
            if (types[1].code() == PType.CHAR) throw UnsupportedConstructException.at(name, "`ARRAY_CONTAINS` of a CHAR value")
            val (a, v) = exprs
            val member = exprInCollection(v, a, false)
            val test =
                when {
                    !nullMatch -> member
                    literal -> exprCoalesce(listOf(member, bool(false)))
                    else -> {
                        val e = element(1)
                        val nulls = count(subquery(listOf(source(a, 1)), exprNullPredicate(e, false)))
                        exprCase(
                            null,
                            listOf(exprCaseBranch(exprNullPredicate(v, false), exprOperator(">", nulls, functions.int(0)))),
                            exprCoalesce(listOf(member, bool(false))),
                        )
                    }
                }
            ifArrays(listOf(a), test)
        }
    }

    /**
     * `ARRAYS_OVERLAP(a, b)` is true if the arrays have an element in common; NULL elements match each other:
     *
     * ```
     * CASE WHEN a IS LIST AND b IS LIST THEN
     *   (SELECT COUNT(*) FROM a AS e1 WHERE (SELECT COUNT(*) FROM b AS e2 WHERE e1 = e2 OR (e1 IS NULL AND e2 IS NULL)) > 0) > 0
     * END
     * ```
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/arrays_overlap.html
     */
    private fun arraysOverlap(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 2)
        functions.scalar(name, setq)
        functions.note(name, "Redshift `ARRAYS_OVERLAP` was replaced by a NULL-safe subquery over both arrays")
        val (a, b) = args.map { it.expr }
        val shared = count(subquery(listOf(source(a, 1)), contains(b)))
        return ifArrays(listOf(a, b), exprOperator(">", shared, functions.int(0)))
    }

    /**
     * `ARRAY_DISTINCT(a)` returns the distinct elements of the array; NULL appears once. Redshift does not guarantee the
     * order of the elements, and the translation sorts them:
     * `CASE WHEN a IS LIST THEN (SELECT DISTINCT VALUE e FROM a AS e ORDER BY e ASC NULLS LAST) END`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/array_distinct.html
     */
    private fun arrayDistinct(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1)
        functions.scalar(name, setq)
        functions.note(name, "Redshift does not guarantee the element order of `ARRAY_DISTINCT`; the PartiQL result is sorted")
        val a = args[0].expr
        return ifArrays(listOf(a), sorted(subquery(listOf(source(a, 1)), null, distinct = true), true, nullsFirst = false))
    }

    /**
     * `ARRAY_SORT(a [, sort_ascending [, nulls_first]])` sorts the elements of the array, by type (BOOLEAN, numbers,
     * strings, arrays, objects) and then by value, as PartiQL ORDER BY does. The flags must be literals; NULL values
     * come last when the order is ascending, unless `nulls_first` says otherwise:
     * `CASE WHEN a IS LIST THEN (SELECT VALUE e FROM a AS e ORDER BY e ASC NULLS LAST) END`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/array_sort.html
     */
    private fun arraySort(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1..3)
        functions.scalar(name, setq)
        val flags =
            args.drop(1).map {
                functions.boolLiteral(it.expr) ?: throw UnsupportedConstructException.at(name, "`ARRAY_SORT` with non-literal flags")
            }
        val ascending = flags.getOrElse(0) { true }
        val nullsFirst = flags.getOrElse(1) { !ascending }
        functions.note(name, "Redshift `ARRAY_SORT` was replaced by an ordered PartiQL subquery over the array")
        val a = args[0].expr
        return ifArrays(listOf(a), sorted(subquery(listOf(source(a, 1)), null), ascending, nullsFirst))
    }

    /**
     * `ARRAY_UNION(a, b)` returns the distinct elements of both arrays. Redshift does not guarantee the order of the
     * elements, and the translation sorts them:
     * `CASE WHEN a IS LIST AND b IS LIST THEN (SELECT DISTINCT VALUE e FROM [a, b] AS p, p AS e ORDER BY e ASC NULLS LAST) END`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/array_union.html
     */
    private fun arrayUnion(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 2)
        functions.scalar(name, setq)
        functions.note(name, "Redshift does not guarantee the element order of `ARRAY_UNION`; the PartiQL result is sorted")
        val (a, b) = args.map { it.expr }
        val pair = fromExpr(qualified(exprArray(listOf(a, b))), FromType.SCAN(), alias("p1"), null)
        val elements = fromExpr(exprVarRef(Identifier.delimited(name("p1")), true), FromType.SCAN(), alias("e1"), null)
        return ifArrays(listOf(a, b), sorted(subquery(listOf(pair, elements), null, distinct = true), true, nullsFirst = false))
    }

    /**
     * `ARRAY_INTERSECTION(a, b, TRUE)` returns the distinct elements that are in both arrays; NULL elements match each
     * other. Redshift does not guarantee the order of the elements, and the translation sorts them. Without the literal
     * TRUE, the result keeps duplicate elements as often as both arrays have them, which needs element positions:
     *
     * ```
     * CASE WHEN a IS LIST AND b IS LIST THEN (SELECT DISTINCT VALUE e1 FROM a AS e1
     *   WHERE (SELECT COUNT(*) FROM b AS e2 WHERE e1 = e2 OR (e1 IS NULL AND e2 IS NULL)) > 0 ORDER BY e1 ASC NULLS LAST) END
     * ```
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/array_intersection.html
     */
    private fun arrayIntersection(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 2..3)
        functions.scalar(name, setq)
        if (args.size != 3 || functions.boolLiteral(args[2].expr) != true) {
            throw UnsupportedConstructException.at(
                name,
                "`ARRAY_INTERSECTION` without a literal TRUE distinct argument (duplicate elements)",
            )
        }
        functions.note(name, "Redshift does not guarantee the element order of `ARRAY_INTERSECTION`; the PartiQL result is sorted")
        val (a, b) = args.map { it.expr }
        val common = subquery(listOf(source(a, 1)), contains(b), distinct = true)
        return ifArrays(listOf(a, b), sorted(common, true, nullsFirst = false))
    }

    /*
     * Subqueries over arrays
     *
     * A variable in a FROM source is resolved as a table first, so the roots of the arrays are qualified variables
     * (`@t.x`), which are resolved as local variables first.
     */

    /** `CASE WHEN a1 IS LIST AND ... THEN [then] END`. */
    private fun ifArrays(
        arrays: List<Expr>,
        then: Expr,
    ): Expr {
        val test = arrays.map { isType(it, DataType.LIST()) }.reduce<Expr, Expr> { x, y -> exprAnd(x, y) }
        return functions.nonMissing(exprCase(null, listOf(exprCaseBranch(test, then)), null))
    }

    /** `(SELECT COUNT(*) FROM b AS e2 WHERE e1 = e2 OR (e1 IS NULL AND e2 IS NULL)) > 0`, with the element e1. */
    private fun contains(b: Expr): Expr {
        val e1 = element(1)
        val e2 = element(2)
        val equal = exprOr(exprOperator("=", e1, e2), exprAnd(exprNullPredicate(e1, false), exprNullPredicate(e2, false)))
        return exprOperator(">", count(subquery(listOf(source(b, 2)), equal)), functions.int(0))
    }

    /** `a AS e<i>`. */
    private fun source(
        a: Expr,
        i: Int,
    ): FromTableRef = fromExpr(qualified(a), FromType.SCAN(), alias("e$i"), null)

    /** `SELECT [DISTINCT] VALUE e1 FROM [from] [WHERE where]`. */
    private fun subquery(
        from: List<FromTableRef>,
        where: Expr?,
        distinct: Boolean = false,
    ): QueryBody.SFW =
        queryBodySFW(
            selectValue(element(1), if (distinct) SetQuantifier.DISTINCT() else null),
            null,
            from(from),
            null,
            where,
            null,
            null,
            null,
        )

    /** `(SELECT COUNT(*) FROM ... WHERE ...)` for the FROM and WHERE of [body]. */
    private fun count(body: QueryBody.SFW): Expr {
        val select = selectList(listOf(selectItemExpr(exprCall(Identifier.regular("count"), emptyList(), null), null)), null)
        return exprQuerySet(queryBodySFW(select, null, body.from, null, body.where, null, null, null), null, null, null)
    }

    /** [body] ordered by its element. */
    private fun sorted(
        body: QueryBody.SFW,
        ascending: Boolean,
        nullsFirst: Boolean,
    ): Expr {
        val order = if (ascending) Order.ASC() else Order.DESC()
        val nulls = if (nullsFirst) Nulls.FIRST() else Nulls.LAST()
        return exprQuerySet(body, orderBy(listOf(sort(element(1), order, nulls))), null, null)
    }

    private fun element(i: Int): Expr = exprVarRef(Identifier.delimited(name("e$i")), false)

    private fun alias(name: String): Identifier.Simple = Identifier.Simple.delimited(name(name))

    /** The names of the variables of the subqueries, which cannot be Redshift identifiers. */
    private fun name(name: String): String = "\$$name"

    /** [expr] with each unqualified variable outside of a subquery qualified (`@x`). */
    private fun qualified(expr: Expr): Expr = QUALIFIER.visitExpr(expr, Unit) as Expr

    private object QUALIFIER : AstRewriter<Unit>() {
        override fun visitExprVarRef(
            node: ExprVarRef,
            ctx: Unit,
        ): AstNode = if (node.isQualified) node else exprVarRef(node.identifier, true)

        override fun visitExprQuerySet(
            node: ExprQuerySet,
            ctx: Unit,
        ): AstNode = node
    }

    /*
     * Type tests
     */

    /**
     * The type tests IS_ARRAY, IS_OBJECT, IS_BOOLEAN, IS_FLOAT, and IS_DECIMAL are false for NULL, and the PartiQL type
     * predicates are NULL for NULL: the translation is `COALESCE(test, false)`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_is_array.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_is_object.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_is_boolean.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_is_float.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_is_decimal.html
     */
    private fun typeTest(
        description: String,
        test: (Expr) -> Expr,
    ): Rule =
        Rule { name, args, setq ->
            functions.arity(name, args, 1)
            functions.scalar(name, setq)
            functions.note(name, "Redshift `${name.text.uppercase()}(x)` was replaced by `COALESCE($description, false)`")
            functions.resolve(args) { exprs, _ -> exprCoalesce(listOf(test(exprs[0]), bool(false))) }
        }

    /**
     * `IS_SCALAR(x)` is true for a value that is not an array or an object, and false for NULL: `CASE WHEN x IS NULL OR x
     * IS LIST OR x IS STRUCT THEN false ELSE true END`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_is_scalar.html
     */
    private fun isScalar(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1)
        functions.scalar(name, setq)
        functions.note(name, "Redshift `IS_SCALAR(x)` was replaced by a CASE expression over `x IS NULL`, `x IS LIST`, and `x IS STRUCT`")
        return functions.resolve(args) { exprs, _ ->
            val x = exprs[0]
            val composite = exprOr(exprOr(exprNullPredicate(x, false), isType(x, DataType.LIST())), isType(x, DataType.STRUCT()))
            exprCase(null, listOf(exprCaseBranch(composite, bool(false))), bool(true))
        }
    }

    /**
     * `IS_SMALLINT(x)`, `IS_INTEGER(x)`, and `IS_BIGINT(x)` are true for a number of scale 0 in the range of the type,
     * and false otherwise (also for NULL). Redshift does not distinguish integers from DECIMAL values of scale 0 in a
     * SUPER value. For a static type, the test is:
     *  - integers and DECIMAL(p, 0): `x IS NOT NULL` when the type is within the range, else `COALESCE(x BETWEEN min AND
     *    max, false)`;
     *  - other types: false.
     *
     * A SUPER value is unsupported: the PartiQL test for a DECIMAL value of scale 0, `x IS DECIMAL(38, 0)`, is not
     * printed correctly by the Scribe PartiQL target.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_is_integer.html
     */
    private fun isInteger(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
        min: Long,
        max: Long,
    ): Expr {
        functions.arity(name, args, 1)
        functions.scalar(name, setq)
        val fn = name.text.uppercase()
        return functions.resolve(args) { exprs, types ->
            val x = exprs[0]
            val type = types[0]
            if (isDynamic(type)) {
                throw UnsupportedConstructException.at(
                    name,
                    "`$fn` of a SUPER value (the test for a DECIMAL value of scale 0 cannot be printed)",
                )
            }
            functions.note(name, "Redshift `$fn(x)` of ${FunctionTypes.describe(type, "value")} was replaced by a test of the value")
            val digits = FunctionTypes.integerDigits(type)
            if (digits == null || isUnknown(type) || (isDecimal(type) && type.scale != 0)) return@resolve bool(false)
            val (low, high) = integerRange(type)
            if (low >= BigInteger.valueOf(min) && high <= BigInteger.valueOf(max)) {
                exprNullPredicate(x, true)
            } else {
                exprCoalesce(listOf(exprBetween(x, functions.int(min), functions.int(max), false), bool(false)))
            }
        }
    }

    /** The smallest and largest value of the integer or DECIMAL(p, 0) type [type]. */
    private fun integerRange(type: PType): Pair<BigInteger, BigInteger> =
        when (type.code()) {
            PType.TINYINT -> BigInteger.valueOf(Byte.MIN_VALUE.toLong()) to BigInteger.valueOf(Byte.MAX_VALUE.toLong())
            PType.SMALLINT -> BigInteger.valueOf(Short.MIN_VALUE.toLong()) to BigInteger.valueOf(Short.MAX_VALUE.toLong())
            PType.INTEGER -> BigInteger.valueOf(Int.MIN_VALUE.toLong()) to BigInteger.valueOf(Int.MAX_VALUE.toLong())
            PType.BIGINT -> BigInteger.valueOf(Long.MIN_VALUE) to BigInteger.valueOf(Long.MAX_VALUE)
            else -> {
                val bound = BigInteger.TEN.pow(type.precision).subtract(BigInteger.ONE)
                bound.negate() to bound
            }
        }

    /**
     * `IS_VARCHAR(x)` is true for character strings. For a SUPER value, the test is `COALESCE(CHAR_LENGTH(x) >= 0, false)`,
     * which is true for every PartiQL string type (`x IS STRING` is false for a VARCHAR or CHAR value); for a static type,
     * it is `x IS NOT NULL` for character strings and false otherwise.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_is_varchar.html
     */
    private fun isVarchar(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1)
        functions.scalar(name, setq)
        functions.note(name, "Redshift `IS_VARCHAR(x)` was replaced by a test of the type of x")
        return functions.resolve(args) { exprs, types ->
            val x = exprs[0]
            when {
                isDynamic(
                    types[0],
                ) -> exprCoalesce(listOf(exprOperator(">=", functions.fn("char_length", x), functions.int(0)), bool(false)))
                isText(types[0]) -> exprNullPredicate(x, true)
                else -> bool(false)
            }
        }
    }

    /**
     * `IS_CHAR(x)` is true for character strings of ASCII characters only: `COALESCE(OCTET_LENGTH(x) = CHAR_LENGTH(x),
     * false)`, because only ASCII characters have one byte in UTF-8. For a static type that is not a character string, it
     * is false.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_is_char.html
     */
    private fun isChar(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1)
        functions.scalar(name, setq)
        functions.note(name, "Redshift `IS_CHAR(x)` was replaced by `COALESCE(OCTET_LENGTH(x) = CHAR_LENGTH(x), false)`")
        return functions.resolve(args) { exprs, types ->
            val x = exprs[0]
            if (isDynamic(types[0]) || isText(types[0])) {
                exprCoalesce(listOf(exprOperator("=", functions.fn("octet_length", x), functions.fn("char_length", x)), bool(false)))
            } else {
                bool(false)
            }
        }
    }

    private fun isType(
        x: Expr,
        type: DataType,
    ): Expr = exprIsType(x, type, false)

    private fun bool(value: Boolean): Expr = exprLit(Literal.bool(value))

    private companion object {
        const val NO_POSITIONS = "PartiQL cannot print the element positions that it needs"
        const val NO_ATTRIBUTE_NAMES = "PartiQL has no function that changes attribute names"
        const val NO_JSON = "PartiQL has no JSON text functions"
    }
}
