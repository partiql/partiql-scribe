package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprArray
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprCoalesce
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprTrim
import org.partiql.ast.AstNode
import org.partiql.ast.AstRewriter
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.SetQuantifier
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprCoalesce
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprNullIf
import org.partiql.ast.expr.ExprQuerySet
import org.partiql.ast.expr.TrimSpec
import org.partiql.scribe.frontend.redshift.RedshiftProblem
import org.partiql.spi.types.PType
import java.util.IdentityHashMap
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * An argument of a function call: the parse tree of the argument and its translation. A rule that depends on the static
 * type of an argument reads it with [RedshiftFunctions.resolve].
 */
internal class Operand(
    val ctx: ParserRuleContext,
    val expr: Expr,
)

/**
 * The rules that translate Redshift function calls to PartiQL.
 *
 * A function that is not in [rules] is unsupported. The translator does not pass unknown functions through by name,
 * because a function with the same name can have different semantics in the two languages. The rules of each function
 * category are in their own class: [RedshiftStringFunctions], [RedshiftConditionalFunctions], [RedshiftMathFunctions],
 * [RedshiftAggregateFunctions], [RedshiftSuperFunctions], and [RedshiftDatetimeFunctions].
 */
internal class RedshiftFunctions(
    private val notes: MutableList<RedshiftProblem>,
    internal val types: OperandTypes,
) {
    internal fun interface Rule {
        fun apply(
            name: Token,
            args: List<Operand>,
            setq: SetQuantifier?,
        ): Expr
    }

    internal val datetime = RedshiftDatetimeFunctions(this)
    internal val strings = RedshiftStringFunctions(this)
    internal val conditional = RedshiftConditionalFunctions(this)
    internal val math = RedshiftMathFunctions(this)
    internal val aggregates = RedshiftAggregateFunctions(this)
    internal val superFunctions = RedshiftSuperFunctions(this)

    private val rules: Map<String, Rule> =
        datetime.rules + strings.rules + conditional.rules + math.rules + aggregates.rules + superFunctions.rules +
            systemFunctions()

    /**
     * The calls of conditional functions (NVL, COALESCE, DECODE, ...) whose translation removed the padding of CHAR(n)
     * values, because the values are unified with other character strings; see [RedshiftConditionalFunctions]. Redshift
     * does not document whether the result keeps the padding, so the functions that count it (OCTET_LENGTH) or that
     * may count it (POSITION) report such an argument as unsupported.
     */
    internal val charBranches: MutableSet<ParserRuleContext> = HashSet()

    /**
     * `POSITION(sub IN str)`; see [RedshiftStringFunctions.position].
     */
    fun position(
        name: Token,
        sub: Operand,
        str: Operand,
    ): Expr = strings.position(name, sub, str)

    /**
     * `SUBSTRING(value FROM start [FOR length])` and `SUBSTRING(value FOR length)`; [start] is null for the second form.
     * See [RedshiftStringFunctions.substring].
     */
    fun substring(
        name: Token,
        value: Operand,
        start: Operand?,
        length: Operand?,
    ): Expr = strings.substring(name, value, start, length)

    /**
     * `TRIM([BOTH | LEADING | TRAILING] [chars] FROM value)`; [spec] is the BOTH, LEADING, or TRAILING token, if any.
     * See [RedshiftStringFunctions.trim].
     */
    fun trim(
        name: Token,
        spec: Token?,
        chars: Operand?,
        value: Operand,
    ): Expr = strings.trim(name, spec, chars, value)

    fun call(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        val rule = rules[name.text.lowercase()] ?: throw UnsupportedConstructException.at(name, "function `${name.text}`")
        val expr = rule.apply(name, args, setq)
        if (!types.probing && aggregateInCall(expr, inCall = false)) {
            throw UnsupportedConstructException.at(
                name,
                "`${name.text}` of an aggregate function (the PartiQL planner does not accept an aggregate in a function argument)",
            )
        }
        if (!types.probing && subqueryArgument(expr)) {
            throw UnsupportedConstructException.at(
                name,
                "subquery as an argument of `${name.text}` (the Scribe PartiQL target prints it without parentheses)",
            )
        }
        return expr
    }

    /**
     * True if [node] contains a subquery that is an argument of a function call, COALESCE, or NULLIF, outside of a
     * subquery. The Scribe PartiQL target prints such a subquery without parentheses, and PartiQL reads the arguments
     * that follow it as part of its FROM clause (`COALESCE(SELECT ... FROM t, 0)`).
     */
    private fun subqueryArgument(node: AstNode): Boolean {
        val args =
            when (node) {
                is ExprQuerySet -> return false
                is ExprCall -> node.args
                is ExprCoalesce -> node.args
                is ExprNullIf -> listOf(node.v1, node.v2)
                else -> emptyList()
            }
        return args.any { it is ExprQuerySet } || node.children.any(::subqueryArgument)
    }

    /**
     * True if [node] contains an aggregate call in the arguments of a scalar function call, outside of a subquery. The
     * PLK 1.6.1 planner does not find such an aggregate (`ABS(SUM(x))` fails with FUNCTION_NOT_FOUND); special forms
     * like COALESCE, CASE, CAST, and the operators are not function calls, so an aggregate in them is found.
     */
    private fun aggregateInCall(
        node: AstNode,
        inCall: Boolean,
    ): Boolean =
        when (node) {
            is ExprQuerySet -> false
            is ExprCall -> if (isAggregate(node)) inCall else node.args.any { aggregateInCall(it, inCall = true) }
            else -> node.children.any { aggregateInCall(it, inCall) }
        }

    /*
     * Rule builders
     */

    internal fun same(
        partiql: String,
        arity: Int,
    ): Rule =
        Rule { name, args, setq ->
            arity(name, args, arity)
            scalar(name, setq)
            exprCall(Identifier.regular(partiql), args.map { it.expr }, null)
        }

    internal fun renamed(
        redshift: String,
        partiql: String,
        arity: Int,
    ): Rule {
        val rule = same(partiql, arity)
        return Rule { name, args, setq ->
            note(name, "Redshift `${redshift.uppercase()}` was replaced by PartiQL `${partiql.uppercase()}`")
            rule.apply(name, args, setq)
        }
    }

    /**
     * A function that the translator recognizes but does not translate, with the [reason]. The construct in the message
     * is the same as for an unknown function, `function `NAME``, followed by the reason.
     */
    internal fun unsupported(reason: String): Rule =
        Rule { name, _, _ -> throw UnsupportedConstructException.at(name, "function `${name.text}`; $reason") }

    /**
     * The system information functions, which return properties of the Redshift cluster and session. They have no
     * PartiQL equivalent.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_System_information_functions.html
     */
    private fun systemFunctions(): Map<String, Rule> {
        val rule = unsupported("a Redshift system information function has no PartiQL equivalent")
        return SYSTEM_FUNCTIONS.associateWith { rule }
    }

    /*
     * Operand types
     */

    /**
     * Translates with [rule], which receives the probed [operands] and their static types; see [OperandTypes]. This is
     * [OperandTypes.resolve], with these changes:
     *  - In a probe pass, the rule gets [placeholder] while a type is not known yet. An aggregate rule passes a
     *    placeholder that is an aggregation, so that the query block stays a grouped query.
     *  - In a probe pass, a type can be provisional: an operand that contains a placeholder has the dynamic type until
     *    the next pass. So a rule that reports a construct as unsupported in a probe pass gets the placeholder instead;
     *    the final pass, which has the final types, reports it.
     *  - An operand that contains an aggregate is probed as [probe] describes.
     *  - An operand whose type is not known (also in the final pass), or that the planner could not type in the last
     *    probe pass, has the type [FunctionTypes.UNRESOLVED]. Only the NULL literal has the unknown type.
     */
    internal fun resolve(
        operands: List<Operand>,
        placeholder: (List<Expr>) -> Expr = ::pending,
        rule: (List<Expr>, List<PType>) -> Expr,
    ): Expr {
        val exprs = operands.map(::probe)
        val known = operands.map { types.typeOf(it.ctx) }
        if (types.probing && known.any { it == null }) return placeholder(exprs)
        // An operand that the planner could not type in the last probe pass has the unknown type, as the NULL literal.
        val resolved =
            known.mapIndexed {
                    i,
                    type,
                ->
                if (type == null || (type.code() == PType.UNKNOWN && !isNullLiteral(operands[i].expr))) FunctionTypes.UNRESOLVED else type
            }
        return try {
            rule(exprs, resolved)
        } catch (ex: UnsupportedConstructException) {
            if (types.probing) placeholder(exprs) else throw ex
        }
    }

    /**
     * Returns the translation of [operand] in a probe call, as [OperandTypes.operand] does.
     *
     * The PLK 1.6.1 planner does not find an aggregate call in the arguments of a scalar function call (for example the
     * probe call), so an aggregate in a probed operand and the operands in it would not be typed. So an operand that
     * contains an aggregate is probed with a copy in which each aggregate is a NULL of its static type (see
     * [aggregateResult]); the aggregates were translated with their types, which the earlier passes found. While the
     * type of an aggregate is not known yet, the operand is not probed.
     */
    private fun probe(operand: Operand): Expr {
        if (!types.probing || !containsAggregate(operand.expr)) return types.operand(operand.ctx, operand.expr)
        val copy = withoutAggregates(operand.expr) ?: return operand.expr
        return types.operand(operand.ctx, copy)
    }

    /**
     * The placeholder of a rule whose operand types are not known yet: `COALESCE(pending(), [arg1, ..., argn])`, which
     * has the dynamic type, like [OperandTypes.pending]. The arguments are elements of an array, not arguments of the
     * placeholder call, so that the planner finds an aggregate in them; see [probe].
     */
    internal fun pending(args: List<Expr>): Expr = exprCoalesce(listOf(types.pending(emptyList()), exprArray(args)))

    /**
     * [resolve] with the signature of [OperandTypes.resolve], for the type-dependent rules of the other translation
     * classes: an operand that contains an aggregate is probed without hiding the aggregate from the planner; see
     * [probe].
     */
    internal fun resolveOperands(
        operands: List<Pair<ParserRuleContext, Expr>>,
        rule: (List<Expr>, List<PType>) -> Expr,
    ): Expr = resolve(operands.map { (ctx, expr) -> Operand(ctx, expr) }, ::pending, rule)

    /** The static PartiQL types of the translations of aggregate calls in this pass, by identity; see [probe]. */
    private val aggregateTypes = IdentityHashMap<Expr, PType>()

    /**
     * Records [type], the static PartiQL type of [expr], the translation of an aggregate call, and returns [expr]; see
     * [probe].
     */
    internal fun aggregateResult(
        expr: Expr,
        type: PType,
    ): Expr {
        aggregateTypes[expr] = type
        return expr
    }

    /** True if [node] contains an aggregate call outside of a subquery. */
    private fun containsAggregate(node: AstNode): Boolean =
        when (node) {
            is ExprQuerySet -> false
            is ExprCall -> isAggregate(node) || node.args.any(::containsAggregate)
            else -> node.children.any(::containsAggregate)
        }

    /**
     * Returns a copy of [expr] in which each aggregate is a NULL of its static type, or null if the type of an aggregate
     * is not known.
     */
    private fun withoutAggregates(expr: Expr): Expr? {
        var complete = true
        val rewriter =
            object : AstRewriter<Unit>() {
                override fun visitExpr(
                    node: Expr,
                    ctx: Unit,
                ): AstNode {
                    aggregateTypes[node]?.let { return nullOf(it) }
                    return when {
                        node is ExprQuerySet -> node
                        // COUNT(*) is a special form of the grammar.
                        node is ExprCall && isAggregate(node) && node.args.isEmpty() -> nullOf(PType.bigint())
                        node is ExprCall && isAggregate(node) -> {
                            complete = false
                            node
                        }
                        else -> super.visitExpr(node, ctx)
                    }
                }
            }
        val copy = rewriter.visitExpr(expr, Unit) as Expr
        return if (complete) copy else null
    }

    /** A NULL of the static type [type]; the placeholder of the dynamic type stands for a dynamic value. */
    private fun nullOf(type: PType): Expr {
        val dataType =
            when (type.code()) {
                PType.BOOL -> DataType.BOOL()
                PType.TINYINT, PType.SMALLINT -> DataType.SMALLINT()
                PType.INTEGER -> DataType.INTEGER()
                PType.BIGINT -> DataType.BIGINT()
                PType.DECIMAL, PType.NUMERIC -> DataType.DECIMAL(type.precision, type.scale)
                PType.REAL -> DataType.REAL()
                PType.DOUBLE -> DataType.DOUBLE_PRECISION()
                PType.CHAR -> DataType.CHAR(type.length)
                else -> return types.pending(emptyList())
            }
        return exprCast(exprLit(Literal.nul()), dataType)
    }

    private fun isAggregate(call: ExprCall): Boolean = call.function.identifier.text.lowercase() in AGGREGATES

    /*
     * Expressions
     */

    /** A call of the PartiQL function [name]. */
    internal fun fn(
        name: String,
        vararg args: Expr,
    ): Expr = exprCall(Identifier.regular(name), args.toList(), null)

    /** `TRIM(TRAILING FROM x)`, which removes the padding of a CHAR(n) value. */
    internal fun trimTrailing(x: Expr): Expr = exprTrim(x, null, TrimSpec.TRAILING())

    /** `CAST(x AS STRING)`. PartiQL VARCHAR without a length is VARCHAR(1), so the cast to text is to STRING. */
    internal fun castToString(x: Expr): Expr = exprCast(x, DataType.STRING())

    internal fun int(value: Long): Expr = exprLit(Literal.intNum(value))

    /** The value of [expr] if it is an integer literal, else null. */
    internal fun intLiteral(expr: Expr): Long? {
        val lit = (expr as? ExprLit)?.lit ?: return null
        return if (lit.code() == Literal.INT_NUM) lit.numberValue().toLongOrNull() else null
    }

    /** The value of [expr] if it is a string literal, else null. */
    internal fun stringLiteral(expr: Expr): String? {
        val lit = (expr as? ExprLit)?.lit ?: return null
        return if (lit.code() == Literal.STRING) lit.stringValue() else null
    }

    /** The value of [expr] if it is a boolean literal, else null. */
    internal fun boolLiteral(expr: Expr): Boolean? {
        val lit = (expr as? ExprLit)?.lit ?: return null
        return if (lit.code() == Literal.BOOL) lit.booleanValue() else null
    }

    internal fun isNullLiteral(expr: Expr): Boolean = (expr as? ExprLit)?.lit?.code() == Literal.NULL

    /*
     * Parse trees
     */

    /**
     * Returns the innermost parse tree of [ctx] that is not a parenthesized expression or a single-alternative wrapper of
     * the grammar (for example, the `predicate` of a `valueExpr`).
     */
    internal fun unwrap(ctx: ParserRuleContext): ParserRuleContext {
        var current = ctx
        while (true) {
            current =
                when (current) {
                    is P.ExprContext -> current.exprOr()
                    is P.OrBaseContext -> current.exprAnd()
                    is P.AndBaseContext -> current.exprNot()
                    is P.NotBaseContext -> current.predicate()
                    is P.PredicateBaseContext -> current.valueExpr()
                    is P.ValuePrimaryContext -> current.primaryExpr()
                    is P.PrimaryAtomContext -> current.atom()
                    is P.AtomParenthesizedContext -> current.expr()
                    else -> return current
                }
        }
    }

    /**
     * True if [ctx] is a column reference, a literal (with a sign), or an explicit CAST. For these, the PartiQL type of
     * the translation is the Redshift type, which is not true for every expression; for example, the scale of a DECIMAL
     * quotient differs.
     */
    internal fun isColumnLiteralOrCast(ctx: ParserRuleContext): Boolean =
        when (val inner = unwrap(ctx)) {
            is P.AtomVariableContext, is P.PathFieldContext, is P.AtomLiteralContext -> true
            is P.AtomCastContext, is P.ValueCastShorthandContext -> true
            is P.ValueSignedContext -> unwrap(inner.valueExpr()) is P.AtomLiteralContext
            else -> false
        }

    /*
     * Checks and notes
     */

    internal fun arity(
        name: Token,
        args: List<*>,
        expected: Int,
    ) {
        if (args.size != expected) {
            throw UnsupportedConstructException.at(name, "`${name.text}` with ${args.size} argument(s); expected $expected")
        }
    }

    internal fun arity(
        name: Token,
        args: List<*>,
        expected: IntRange,
    ) {
        if (args.size !in expected) {
            val bounds = if (expected.last == Int.MAX_VALUE) "at least ${expected.first}" else "${expected.first} to ${expected.last}"
            throw UnsupportedConstructException.at(name, "`${name.text}` with ${args.size} argument(s); expected $bounds")
        }
    }

    /** Rejects a set quantifier (DISTINCT or ALL) in a call of a scalar function. */
    internal fun scalar(
        name: Token,
        setq: SetQuantifier?,
    ) {
        if (setq != null) throw UnsupportedConstructException.at(name, "set quantifier in scalar function `${name.text}`")
    }

    internal fun note(
        token: Token,
        message: String,
    ) {
        notes.add(RedshiftProblem(RedshiftProblem.Code.TRANSLATION_INFO, message, token.line, token.charPositionInLine + 1))
    }

    internal companion object {
        /** The PartiQL aggregate functions that the translation emits. */
        private val AGGREGATES = setOf("count", "sum", "avg", "min", "max", "every", "some", "any")

        private val SYSTEM_FUNCTIONS =
            listOf(
                "current_aws_account", "current_database", "current_namespace", "current_schema", "current_schemas",
                "current_session_arn", "current_user_id", "db_collation", "default_iam_role", "get_mounted_role",
                "has_assumerole_privilege", "has_column_privilege", "has_database_privilege", "has_function_privilege",
                "has_language_privilege", "has_schema_privilege", "has_table_privilege", "last_user_query_id",
                "pg_backend_pid", "pg_get_cols", "pg_get_grantee_by_iam_role", "pg_get_iam_role_by_user",
                "pg_get_late_binding_view_cols", "pg_get_session_roles", "pg_last_copy_count", "pg_last_copy_id",
                "pg_last_query_id", "pg_last_unload_count", "pg_last_unload_id", "role_is_member_of", "slice_num",
                "user_is_member_of", "version",
            )
    }
}

/**
 * Static types of function arguments, by Redshift type family.
 */
internal object FunctionTypes {
    fun isInteger(type: PType): Boolean = type.code() in INTEGERS

    fun isDecimal(type: PType): Boolean = type.code() == PType.DECIMAL || type.code() == PType.NUMERIC

    fun isExactNumeric(type: PType): Boolean = isInteger(type) || isDecimal(type)

    fun isApproximateNumeric(type: PType): Boolean = type.code() == PType.REAL || type.code() == PType.DOUBLE

    fun isNumeric(type: PType): Boolean = isExactNumeric(type) || isApproximateNumeric(type)

    fun isText(type: PType): Boolean = type.code() in TEXT

    fun isDatetime(type: PType): Boolean = type.code() in DATETIME

    fun isDynamic(type: PType): Boolean = type.code() == PType.DYNAMIC || type.code() == PType.VARIANT

    /** The type of the NULL literal, and of an operand that the planner could not resolve. */
    fun isUnknown(type: PType): Boolean = type.code() == PType.UNKNOWN

    /**
     * The number of digits before the decimal point of an exact numeric type, or null for other types.
     */
    fun integerDigits(type: PType): Int? =
        when (type.code()) {
            PType.TINYINT -> 3
            PType.SMALLINT -> 5
            PType.INTEGER -> 10
            PType.BIGINT -> 19
            PType.DECIMAL, PType.NUMERIC -> type.precision - type.scale
            else -> null
        }

    /**
     * The dynamic type of an operand that the planner could not type in the probe passes, for example because an
     * expression around it hides it from the planner; compared by identity.
     */
    val UNRESOLVED: PType = PType.dynamic()

    /** "a [noun] of type T" for messages, for example "an argument of type SUPER" or "a value of unknown type". */
    fun describe(
        type: PType,
        noun: String,
    ): String {
        val article = if (noun.first() in "aeiou") "an" else "a"
        return if (type === UNRESOLVED) "$article $noun of unknown type" else "$article $noun of type ${name(type)}"
    }

    /** The Redshift name of [type], for messages. */
    fun name(type: PType): String =
        when (type.code()) {
            PType.BOOL -> "BOOLEAN"
            PType.TINYINT, PType.SMALLINT -> "SMALLINT"
            PType.INTEGER -> "INTEGER"
            PType.BIGINT -> "BIGINT"
            PType.DECIMAL, PType.NUMERIC -> "DECIMAL(${type.precision}, ${type.scale})"
            PType.REAL -> "REAL"
            PType.DOUBLE -> "DOUBLE PRECISION"
            PType.CHAR -> "CHAR(${type.length})"
            PType.VARCHAR -> "VARCHAR(${type.length})"
            PType.STRING, PType.CLOB -> "VARCHAR"
            PType.DATE -> "DATE"
            PType.TIME -> "TIME"
            PType.TIMEZ -> "TIMETZ"
            PType.TIMESTAMP -> "TIMESTAMP"
            PType.TIMESTAMPZ -> "TIMESTAMPTZ"
            PType.INTERVAL_YM, PType.INTERVAL_DT -> "INTERVAL"
            PType.BLOB -> "VARBYTE"
            PType.UNKNOWN -> "UNKNOWN"
            else -> "SUPER"
        }

    private val INTEGERS = setOf(PType.TINYINT, PType.SMALLINT, PType.INTEGER, PType.BIGINT)
    private val TEXT = setOf(PType.CHAR, PType.VARCHAR, PType.STRING, PType.CLOB)
    private val DATETIME = setOf(PType.DATE, PType.TIME, PType.TIMEZ, PType.TIMESTAMP, PType.TIMESTAMPZ)
}
