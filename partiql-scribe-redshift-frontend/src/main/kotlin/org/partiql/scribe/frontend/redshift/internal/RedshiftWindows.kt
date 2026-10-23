package org.partiql.scribe.frontend.redshift.internal

import org.partiql.ast.Ast.exprWindowFunction
import org.partiql.ast.Ast.orderBy
import org.partiql.ast.Ast.windowPartition
import org.partiql.ast.Ast.windowSpecification
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.WindowFunctionNullTreatment
import org.partiql.ast.WindowFunctionType
import org.partiql.ast.WindowPartition
import org.partiql.ast.WindowSpecification
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprCoalesce
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprPath
import org.partiql.ast.expr.ExprVarRef
import org.partiql.ast.expr.PathStep
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * Translates Redshift window functions to PartiQL window functions.
 *
 * PartiQL has ROW_NUMBER, RANK, DENSE_RANK, LAG, and LEAD over a window with PARTITION BY columns and an ORDER BY.
 * It has no aggregate window functions, no window frames, and no IGNORE NULLS, so these are unsupported, except the
 * running `COUNT(*)`, which is ROW_NUMBER.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/c_Window_functions.html
 */
@Suppress("DEPRECATION")
internal class RedshiftWindows(private val builder: RedshiftAstBuilder) {
    fun window(ctx: P.AtomContext): Expr =
        when (ctx) {
            is P.AtomCountStarContext -> aggregate(ctx, "count", ctx.overClause(), constantArgument = true)
            is P.AtomFunctionCallContext -> call(ctx)
            else -> builder.unexpected(ctx)
        }

    private fun call(ctx: P.AtomFunctionCallContext): Expr {
        val name = foldAscii(ctx.name.text)
        val treatment = ctx.nullTreatment()
        treatment?.IGNORE()?.let { throw UnsupportedConstructException.at(it.symbol, "IGNORE NULLS") }
        val over = ctx.overClause() ?: throw InvalidQueryException.at(treatment!!, "${treatment.text.uppercase()} without OVER")
        ctx.setQuantifier()?.let {
            throw UnsupportedConstructException.at(it, "${it.text.uppercase()} in the window function `${ctx.name.text.uppercase()}`")
        }
        return when (name) {
            "row_number", "rank", "dense_rank" -> ranking(ctx, name, over)
            "lag", "lead" -> lagLead(ctx, name, over)
            in AGGREGATES -> {
                val args = ctx.expr()
                val constant = args.size == 1 && isNonNullConstant(args[0]) && (name == "count" || name == "sum" && isOne(args[0]))
                aggregate(ctx, name, over, constant)
            }
            else -> throw UnsupportedConstructException.at(ctx.name.start, "window function `${ctx.name.text.uppercase()}`")
        }
    }

    /**
     * ROW_NUMBER, RANK, and DENSE_RANK. Redshift RANK returns INTEGER, and PartiQL RANK returns BIGINT; the values
     * are the same.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_WF_ROW_NUMBER.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_WF_RANK.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_WF_DENSE_RANK.html
     */
    private fun ranking(
        ctx: P.AtomFunctionCallContext,
        name: String,
        over: P.OverClauseContext,
    ): Expr {
        if (ctx.expr().isNotEmpty()) throw InvalidQueryException.at(ctx, "`${ctx.name.text.uppercase()}` takes no arguments")
        ctx.nullTreatment()?.let { throw InvalidQueryException.at(it, "${it.text.uppercase()} with `${ctx.name.text.uppercase()}`") }
        val type =
            when (name) {
                "row_number" -> WindowFunctionType.RowNumber()
                "rank" -> {
                    builder.note(ctx.name.start, "Redshift `RANK` returns INTEGER; PartiQL `RANK` returns BIGINT")
                    WindowFunctionType.Rank()
                }
                else -> WindowFunctionType.DenseRank()
            }
        return exprWindowFunction(type, specification(over, frame = null))
    }

    /**
     * `LAG(x [, offset]) [RESPECT NULLS] OVER (...)` and LEAD. PartiQL needs the offset as an integer literal.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_WF_LAG.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_WF_LEAD.html
     */
    private fun lagLead(
        ctx: P.AtomFunctionCallContext,
        name: String,
        over: P.OverClauseContext,
    ): Expr {
        val args = ctx.expr()
        val function = ctx.name.text.uppercase()
        when {
            args.isEmpty() -> throw InvalidQueryException.at(ctx, "`$function` needs an argument")
            args.size > 2 -> throw UnsupportedConstructException.at(args[2], "$function with a default value")
        }
        val offset =
            if (args.size == 2) {
                val offsetError = "$function offset that is not a non-negative integer literal"
                val literal = integerLiteral(args[1]) ?: throw UnsupportedConstructException.at(args[1], offsetError)
                literal.toLongOrNull() ?: throw UnsupportedConstructException.at(args[1], offsetError)
            } else {
                1L
            }
        if (over.sortItem().isEmpty()) throw UnsupportedConstructException.at(over, "$function without ORDER BY")
        val extent = builder.expr(args[0])
        val type =
            if (name == "lag") {
                WindowFunctionType.Lag(extent, offset, null, WindowFunctionNullTreatment.RESPECT_NULLS())
            } else {
                WindowFunctionType.Lead(extent, offset, null, WindowFunctionNullTreatment.RESPECT_NULLS())
            }
        return exprWindowFunction(type, specification(over, frame = null))
    }

    /**
     * An aggregate window function. A running count, `COUNT(*) OVER ([PARTITION BY p] ORDER BY o ROWS UNBOUNDED
     * PRECEDING)` (also `COUNT(<constant>)` and `SUM(1)`, and the frame `ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT
     * ROW`), is the number of the row in the partition: ROW_NUMBER. Other aggregate windows are unsupported.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_WF_COUNT.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_WF_SUM.html
     */
    private fun aggregate(
        ctx: P.AtomContext,
        name: String,
        over: P.OverClauseContext,
        constantArgument: Boolean,
    ): Expr {
        val frame = over.frameClause()
        val running = frame != null && isRunning(frame) && over.sortItem().isNotEmpty()
        if (running && constantArgument) {
            builder.note(ctx.start, "Redshift running `${name.uppercase()}` was replaced by `ROW_NUMBER()`")
            return exprWindowFunction(WindowFunctionType.RowNumber(), specification(over, frame))
        }
        if (frame != null) throw UnsupportedConstructException.at(frame, "window frame")
        throw UnsupportedConstructException.at(ctx, "aggregate window function `${name.uppercase()}`")
    }

    /** `ROWS UNBOUNDED PRECEDING` or `ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW` */
    private fun isRunning(frame: P.FrameClauseContext): Boolean {
        if (frame.unit.type != P.ROWS) return false
        val bounds = frame.frameBound()

        fun unboundedPreceding(b: P.FrameBoundContext) = b.UNBOUNDED() != null && b.PRECEDING() != null

        fun currentRow(b: P.FrameBoundContext) = b.CURRENT() != null
        return when (bounds.size) {
            1 -> unboundedPreceding(bounds[0])
            2 -> unboundedPreceding(bounds[0]) && currentRow(bounds[1])
            else -> false
        }
    }

    /**
     * The window specification. PartiQL accepts only columns in PARTITION BY. The ORDER BY items always have the null
     * order: Redshift sorts NULL values last for ASC and first for DESC.
     */
    private fun specification(
        over: P.OverClauseContext,
        frame: P.FrameClauseContext?,
    ): WindowSpecification {
        over.frameClause()?.let { if (it !== frame) throw UnsupportedConstructException.at(it, "window frame") }
        val partitions = over.partition.map(::partition)
        val sorts = over.sortItem().map(builder::windowSortItem)
        return windowSpecification(null, partitions.ifEmpty { null }, if (sorts.isEmpty()) null else orderBy(sorts))
    }

    private fun partition(ctx: P.ExprContext): WindowPartition {
        var expr = builder.windowExpr(ctx)
        // NULL and MISSING keys are in the same partition, so a SUPER path in COALESCE is the same partition key.
        if (expr is ExprCoalesce && expr.args.size == 1) expr = expr.args[0]
        val name = qualifiedName(expr) ?: throw UnsupportedConstructException.at(ctx, "PARTITION BY expression")
        return windowPartition(name)
    }

    /** The qualified name of a column or SUPER path, `t.c` or `t.x.y`, or null. */
    private fun qualifiedName(probed: Expr): Identifier? {
        // A probe pass probes the root of a path; a path can also be the root of another path.
        val expr = flatten(builder.types.unprobe(probed))
        val root = (if (expr is ExprPath) expr.root else expr) as? ExprVarRef ?: return null
        if (root.identifier.hasQualifier() || root.isQualified) return null
        val parts = mutableListOf(root.identifier.identifier)
        if (expr is ExprPath) {
            for (step in expr.steps) {
                parts.add(
                    when (step) {
                        is PathStep.Field -> step.field
                        is PathStep.Element -> {
                            val element = step.element
                            if (element is ExprLit && element.lit.code() == Literal.STRING) {
                                Identifier.Simple.delimited(
                                    element.lit.stringValue(),
                                )
                            } else {
                                return null
                            }
                        }
                        else -> return null
                    },
                )
            }
        }
        return Identifier.of(parts)
    }

    private fun flatten(expr: Expr): Expr {
        if (expr !is ExprPath) return expr
        val root = flatten(expr.root)
        return if (root is ExprPath) org.partiql.ast.Ast.exprPath(root.root, root.steps + expr.steps) else expr
    }

    /** The text of a non-negative integer literal, if [ctx] is one. */
    private fun integerLiteral(ctx: P.ExprContext): String? {
        val primary = (RedshiftAstBuilder.valueOf(ctx) as? P.ValuePrimaryContext)?.primaryExpr() as? P.PrimaryAtomContext ?: return null
        val literal = (primary.atom() as? P.AtomLiteralContext)?.literal() as? P.LiteralIntegerContext ?: return null
        return literal.text
    }

    private fun isNonNullConstant(ctx: P.ExprContext): Boolean {
        val primary = (RedshiftAstBuilder.valueOf(ctx) as? P.ValuePrimaryContext)?.primaryExpr() as? P.PrimaryAtomContext ?: return false
        val literal = (primary.atom() as? P.AtomLiteralContext)?.literal() ?: return false
        return literal !is P.LiteralNullContext
    }

    private fun isOne(ctx: P.ExprContext): Boolean = integerLiteral(ctx)?.toBigIntegerOrNull() == java.math.BigInteger.ONE

    private companion object {
        /** Redshift aggregate functions that have a window form. */
        val AGGREGATES =
            setOf(
                "count", "sum", "avg", "min", "max", "listagg", "median", "stddev", "stddev_samp", "stddev_pop", "variance",
                "var_samp", "var_pop", "percentile_cont", "percentile_disc",
            )
    }
}
