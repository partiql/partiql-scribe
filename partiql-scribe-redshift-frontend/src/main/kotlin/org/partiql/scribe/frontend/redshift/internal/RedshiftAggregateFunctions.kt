package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.tree.ParseTree
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.SetQuantifier
import org.partiql.ast.expr.Expr
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isDecimal
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isDynamic
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isInteger
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isUnknown
import org.partiql.scribe.frontend.redshift.internal.RedshiftFunctions.Rule
import org.partiql.spi.types.PType
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * The rules that translate Redshift aggregate functions; see [RedshiftFunctions].
 *
 * The Redshift result types of SUM, AVG, MIN, and MAX depend on the argument type, and PLK 1.6.1 has runtime defects
 * for some argument types (SUM of BIGINT, AVG of SMALLINT and REAL). So these rules depend on the static type of the
 * argument. An argument of type SUPER is unsupported, because the Redshift results for values of mixed types are not
 * documented.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/c_Aggregate_Functions.html
 */
internal class RedshiftAggregateFunctions(
    private val functions: RedshiftFunctions,
) {
    val rules: Map<String, Rule> =
        mapOf(
            "count" to Rule(::count),
            "sum" to Rule(::sum),
            "avg" to Rule(::avg),
            "min" to Rule { name, args, setq -> minMax("min", name, args, setq) },
            "max" to Rule { name, args, setq -> minMax("max", name, args, setq) },
            "bool_and" to Rule { name, args, setq -> boolean("every", name, args, setq) },
            "bool_or" to Rule { name, args, setq -> boolean("some", name, args, setq) },
            "any_value" to Rule(::anyValue),
            // https://docs.aws.amazon.com/redshift/latest/dg/r_LISTAGG.html
            "listagg" to functions.unsupported("PartiQL has no string aggregation"),
            // https://docs.aws.amazon.com/redshift/latest/dg/r_MEDIAN.html
            "median" to functions.unsupported(NO_DISTRIBUTION),
            "percentile_cont" to functions.unsupported(NO_DISTRIBUTION),
            "percentile_disc" to functions.unsupported(NO_DISTRIBUTION),
            // https://docs.aws.amazon.com/redshift/latest/dg/r_STDDEV_functions.html
            "stddev" to functions.unsupported(NO_STATISTICS),
            "stddev_samp" to functions.unsupported(NO_STATISTICS),
            "stddev_pop" to functions.unsupported(NO_STATISTICS),
            // https://docs.aws.amazon.com/redshift/latest/dg/r_VARIANCE_functions.html
            "variance" to functions.unsupported(NO_STATISTICS),
            "var_samp" to functions.unsupported(NO_STATISTICS),
            "var_pop" to functions.unsupported(NO_STATISTICS),
            // https://docs.aws.amazon.com/redshift/latest/dg/c_bitwise_aggregate_functions.html
            "bit_and" to functions.unsupported("PartiQL has no bit-wise aggregation"),
            "bit_or" to functions.unsupported("PartiQL has no bit-wise aggregation"),
            // https://docs.aws.amazon.com/redshift/latest/dg/r_HLL_function.html
            "hll" to functions.unsupported(NO_SKETCHES),
            "hll_create_sketch" to functions.unsupported(NO_SKETCHES),
            "hll_combine" to functions.unsupported(NO_SKETCHES),
            "hll_cardinality" to functions.unsupported(NO_SKETCHES),
            "hll_combine_sketches" to functions.unsupported(NO_SKETCHES),
        )

    /**
     * `COUNT([ALL | DISTINCT] x)`. COUNT returns BIGINT in both languages and does not count NULL values. `COUNT(*)` is
     * a special form of the grammar.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_COUNT.html
     */
    private fun count(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1)
        return functions.aggregateResult(agg("count", args[0].expr, setq), PType.bigint())
    }

    /**
     * `SUM([ALL | DISTINCT] x)`. Redshift returns BIGINT for an integer argument, DECIMAL(38, s) for DECIMAL(p, s), and
     * DOUBLE PRECISION for REAL and DOUBLE PRECISION.
     *  - SMALLINT, INTEGER, DOUBLE PRECISION: `SUM(x)`, which has the Redshift result type.
     *  - BIGINT: `CAST(SUM(CAST(x AS DECIMAL(38, 0))) AS BIGINT)`. PLK 1.6.1 fails at run time for SUM of a BIGINT.
     *  - REAL: `SUM(CAST(x AS DOUBLE PRECISION))`; PLK types SUM of a REAL as REAL.
     *  - DECIMAL(p, s): `CAST(SUM(x) AS DECIMAL(38, s))`; PLK types the sum as DECIMAL(38, 19). The scale is the
     *    Redshift scale unless `x` contains a division, whose scale differs in the two languages.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_SUM.html
     */
    private fun sum(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1)
        return aggregate("sum", args[0], setq) { x, type ->
            when {
                isUnknown(type) -> agg("sum", x, setq)
                type.code() == PType.BIGINT ->
                    exprCast(agg("sum", exprCast(x, DataType.DECIMAL(38, 0)), setq), DataType.BIGINT())
                isInteger(type) || type.code() == PType.DOUBLE -> agg("sum", x, setq)
                type.code() == PType.REAL -> agg("sum", exprCast(x, DataType.DOUBLE_PRECISION()), setq)
                isDecimal(type) -> {
                    if (containsDivision(args[0].ctx)) unsupported(name, "SUM of a DECIMAL quotient (the scales differ)")
                    exprCast(agg("sum", x, setq), DataType.DECIMAL(38, type.scale))
                }
                else -> unsupported(name, "SUM of ${FunctionTypes.describe(type, "argument")}")
            }
        }
    }

    /**
     * `AVG([ALL | DISTINCT] x)`.
     *  - SMALLINT, INTEGER: `SUM(x) / COUNT(x)`. Redshift returns BIGINT without the fraction; PartiQL AVG returns the
     *    exact DECIMAL mean, and fails at run time for SMALLINT.
     *  - BIGINT: `CAST(AVG(x) - AVG(x) % 1 AS BIGINT)`, which truncates the exact mean. `SUM(x)` fails at run time for
     *    BIGINT in PLK 1.6.1.
     *  - REAL: `AVG(CAST(x AS DOUBLE PRECISION))`. Redshift returns DOUBLE PRECISION; PLK AVG fails at run time for REAL.
     *  - DOUBLE PRECISION: `AVG(x)`.
     *  - DECIMAL: unsupported. Redshift returns DECIMAL(38, s) with the scale of the argument, and does not document how
     *    it shortens the fraction.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_AVG.html
     */
    private fun avg(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1)
        return aggregate("avg", args[0], setq) { x, type ->
            when {
                isUnknown(type) -> agg("avg", x, setq)
                type.code() == PType.BIGINT -> {
                    functions.note(
                        name,
                        "Redshift `AVG` of a BIGINT argument returns BIGINT without the fraction; it was replaced by " +
                            "`CAST(AVG(x) - AVG(x) % 1 AS BIGINT)`",
                    )
                    val avg = agg("avg", x, setq)
                    exprCast(exprOperator("-", avg, exprOperator("%", avg, functions.int(1))), DataType.BIGINT())
                }
                isInteger(type) -> {
                    functions.note(
                        name,
                        "Redshift `AVG` of an integer argument returns BIGINT without the fraction; it was replaced by `SUM(x) / COUNT(x)`",
                    )
                    exprOperator("/", agg("sum", x, setq), agg("count", x, setq))
                }
                type.code() == PType.REAL -> agg("avg", exprCast(x, DataType.DOUBLE_PRECISION()), setq)
                type.code() == PType.DOUBLE -> agg("avg", x, setq)
                isDecimal(type) -> unsupported(name, "AVG of a DECIMAL argument (Redshift returns the scale of the argument)")
                else -> unsupported(name, "AVG of ${FunctionTypes.describe(type, "argument")}")
            }
        }
    }

    /**
     * `MIN([ALL | DISTINCT] x)` and `MAX([ALL | DISTINCT] x)`; the result has the type of the argument.
     *  - DECIMAL(p, s): `CAST(MIN(x) AS DECIMAL(p, s))`. PLK types the result as DECIMAL(38, 19), so arithmetic on a
     *    result with more than 19 digits before the decimal point fails.
     *  - CHAR(n): `MIN(TRIM(TRAILING FROM x))`, because Redshift disregards the padding when it compares CHAR values (the
     *    padded values compare differently when a value has a character below the blank). The result is the value without
     *    the padding; PLK 1.6.1 has no run-time cast from VARCHAR to CHAR(n) to restore it.
     *  - BOOLEAN: unsupported; Redshift does not accept it (BOOL_AND and BOOL_OR are the BOOLEAN equivalents).
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_MIN.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_MAX.html
     */
    private fun minMax(
        fn: String,
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1)
        return aggregate(fn, args[0], setq) { x, type ->
            when {
                type.code() == PType.BOOL -> unsupported(name, "${fn.uppercase()} of a BOOLEAN argument")
                type.code() == PType.CHAR -> agg(fn, functions.trimTrailing(x), setq)
                else -> extremum(fn, name, x, type, setq)
            }
        }
    }

    /**
     * `MIN(x)` or `MAX(x)` for an argument of a type that the result keeps; see [minMax].
     */
    private fun extremum(
        fn: String,
        name: Token,
        x: Expr,
        type: PType,
        setq: SetQuantifier?,
    ): Expr =
        when {
            isDecimal(type) -> exprCast(agg(fn, x, setq), DataType.DECIMAL(type.precision, type.scale))
            isDynamic(type) || type.code() in COLLECTIONS -> {
                val reason = if (type === FunctionTypes.UNRESOLVED) "" else " (the order of SUPER values is not documented)"
                unsupported(name, "${name.text.uppercase()} of ${FunctionTypes.describe(type, "argument")}$reason")
            }
            type.code() == PType.INTERVAL_YM || type.code() == PType.INTERVAL_DT ->
                unsupported(name, "${name.text.uppercase()} of an INTERVAL argument")
            else -> agg(fn, x, setq)
        }

    /**
     * `BOOL_AND(x)` and `BOOL_OR(x)` of a BOOLEAN argument are PartiQL `EVERY(x)` and `SOME(x)`: NULL values are ignored,
     * and the result is NULL when there is no value. Redshift also accepts integer arguments, for which the mapping to
     * BOOLEAN is not documented.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_BOOL_AND.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_BOOL_OR.html
     */
    private fun boolean(
        fn: String,
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1)
        return aggregate(fn, args[0], setq) { x, type ->
            if (type.code() != PType.BOOL && !isUnknown(type)) {
                unsupported(name, "${name.text.uppercase()} of ${FunctionTypes.describe(type, "argument")}; the argument must be BOOLEAN")
            }
            functions.note(name, "Redshift `${name.text.uppercase()}` was replaced by PartiQL `${fn.uppercase()}`")
            agg(fn, x, setq)
        }
    }

    /**
     * `ANY_VALUE([ALL | DISTINCT] x)` returns an arbitrary value of the group, or NULL. The translation is `MIN(x)`,
     * which returns one of those values, deterministically.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_ANY_VALUE.html
     */
    private fun anyValue(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1)
        return aggregate("min", args[0], null) { x, type ->
            val min = extremum("min", name, x, type, null)
            functions.note(
                name,
                "Redshift `ANY_VALUE` returns an arbitrary value of the group; it was replaced by PartiQL `MIN`, which returns " +
                    "one of those values",
            )
            min
        }
    }

    /**
     * Translates an aggregate call with [rule], which receives the probed argument and its static type; see
     * [RedshiftFunctions.resolve]. While the type is not known, the placeholder is a COUNT of the argument, so that the
     * query block stays a grouped query in which the argument has its type. The static type of the translation is
     * recorded; see [RedshiftFunctions.aggregateResult].
     *
     * A rule that probes an operand with [OperandTypes.operand] (for example the rule for `+` in `SUM(x) + 1`) puts the
     * placeholder in a probe call, in which the PLK 1.6.1 planner does not find the COUNT; then the type of the argument
     * stays unknown, and the aggregate is unsupported.
     */
    private fun aggregate(
        fn: String,
        arg: Operand,
        setq: SetQuantifier?,
        rule: (Expr, PType) -> Expr,
    ): Expr =
        functions.resolveAggregate(fn, listOf(arg), setq) { exprs, argTypes ->
            val expr = rule(exprs[0], argTypes[0])
            val type = resultType(fn, argTypes[0])
            if (type != null) functions.aggregateResult(expr, type) else expr
        }

    /**
     * The static PartiQL type of the translation of the aggregate [fn] (a PartiQL name) of an argument of type [type], or
     * null if it is not known.
     */
    private fun resultType(
        fn: String,
        type: PType,
    ): PType? =
        when (fn) {
            "sum" ->
                when {
                    isInteger(type) -> PType.bigint()
                    type.code() == PType.REAL || type.code() == PType.DOUBLE -> PType.doublePrecision()
                    isDecimal(type) -> PType.decimal(38, type.scale)
                    else -> null
                }
            "avg" ->
                when {
                    isInteger(type) -> PType.bigint()
                    type.code() == PType.REAL || type.code() == PType.DOUBLE -> PType.doublePrecision()
                    else -> null
                }
            "min", "max" ->
                when {
                    isInteger(type) || type.code() == PType.REAL || type.code() == PType.DOUBLE -> type
                    isDecimal(type) -> PType.decimal(type.precision, type.scale)
                    isUnknown(type) -> null
                    // PLK has MIN and MAX overloads for numbers only; other values have the dynamic type.
                    else -> PType.dynamic()
                }
            "every", "some" -> PType.bool()
            else -> null
        }

    private fun agg(
        fn: String,
        x: Expr,
        setq: SetQuantifier?,
    ): Expr = exprCall(Identifier.regular(fn), listOf(x), setq)

    private fun unsupported(
        name: Token,
        construct: String,
    ): Nothing = throw UnsupportedConstructException.at(name, construct)

    /** True if [ctx] contains a division outside of a subquery. */
    private fun containsDivision(ctx: ParseTree): Boolean =
        when (ctx) {
            is P.ValueMultiplicativeContext -> ctx.op.type == P.SLASH || containsDivision(ctx.lhs) || containsDivision(ctx.rhs)
            is P.AtomSubqueryContext -> false
            is ParserRuleContext -> (0 until ctx.childCount).any { containsDivision(ctx.getChild(it)) }
            else -> false
        }

    private companion object {
        const val NO_DISTRIBUTION = "PartiQL has no inverse distribution aggregation"
        const val NO_STATISTICS = "PartiQL has no square root or statistical aggregation"
        const val NO_SKETCHES = "PartiQL has no HyperLogLog sketches"

        val COLLECTIONS = setOf(PType.ARRAY, PType.BAG, PType.ROW, PType.STRUCT)
    }
}
