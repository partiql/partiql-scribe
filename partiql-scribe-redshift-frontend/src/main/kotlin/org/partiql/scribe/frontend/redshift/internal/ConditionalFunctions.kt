package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprAnd
import org.partiql.ast.Ast.exprCase
import org.partiql.ast.Ast.exprCaseBranch
import org.partiql.ast.Ast.exprCoalesce
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprNullIf
import org.partiql.ast.Ast.exprNullPredicate
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.exprOr
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprOperator
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * The conditional functions: `NVL`, `COALESCE`, `NULLIF`, `NVL2`, `DECODE`, `GREATEST`, and `LEAST`. Each rule rewrites
 * a call into the PartiQL expression with the same value. `COALESCE` and `NULLIF` are PartiQL's own; the others become
 * `CASE` expressions. Where Redshift converts the arguments of one of these functions to a common type, the planner
 * types the PartiQL expression afterwards, as for any expression of the query.
 *
 * `DECODE`, `GREATEST`, and `LEAST` repeat their arguments in the rewrite, which [RedshiftAstBuilder.size] bounds over
 * the whole statement; every argument is an [RedshiftAstBuilder.argument], so a subquery is refused.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/c_conditional_expressions.html
 */
internal class ConditionalFunctions(private val builder: RedshiftAstBuilder) {
    /** Translates a call of [name] with [args], or returns null when [name] is not a conditional function. */
    fun translate(
        name: Token,
        args: List<P.ExprContext>,
    ): Expr? =
        when (name.type) {
            P.NVL, P.COALESCE -> coalesce(name, args)
            P.NULLIF -> nullif(name, args)
            P.NVL2 -> nvl2(name, args)
            P.DECODE -> decode(name, args)
            P.GREATEST -> extremum(name, args, greatest = true)
            P.LEAST -> extremum(name, args, greatest = false)
            else -> null
        }

    /**
     * `COALESCE(e1, ..., en)` and its synonym `NVL(e1, ..., en)` return the first argument that is not NULL, as PartiQL's
     * `COALESCE` does.
     * https://docs.aws.amazon.com/redshift/latest/dg/r_NVL_function.html
     */
    private fun coalesce(
        name: Token,
        args: List<P.ExprContext>,
    ): Expr = builder.nested(name) { exprCoalesce(args.map(builder::argument)) }

    /**
     * `NULLIF(a, b)` is NULL when `a` equals `b`, and `a` otherwise, as PartiQL's `NULLIF` is.
     * https://docs.aws.amazon.com/redshift/latest/dg/r_NULLIF_function.html
     */
    private fun nullif(
        name: Token,
        args: List<P.ExprContext>,
    ): Expr {
        arity(name, args, 2, 2)
        return builder.nested(name) { exprNullIf(builder.argument(args[0]), builder.argument(args[1])) }
    }

    /**
     * `NVL2(e, a, b)` is `a` when `e` is not NULL and `b` otherwise: `CASE WHEN e IS NOT NULL THEN a ELSE b END`.
     * https://docs.aws.amazon.com/redshift/latest/dg/r_NVL2.html
     */
    private fun nvl2(
        name: Token,
        args: List<P.ExprContext>,
    ): Expr {
        arity(name, args, 3, 3)
        return builder.nested(name) {
            val (e, a, b) = args.map(builder::argument)
            exprCase(null, listOf(exprCaseBranch(exprNullPredicate(e, true), a)), b)
        }
    }

    /**
     * `DECODE(e, s1, r1, ..., sk, rk [, d])` is the result of the first search value that matches `e`, else `d`, else
     * NULL. A NULL search value matches a NULL `e`, which `=` does not, so the match `mi` is `e IS NULL` when `si` is the
     * NULL literal (`si IS NULL` when `e` is), `e = si` when either is another literal, and
     * `e = si OR (e IS NULL AND si IS NULL)` when both may be NULL: `CASE WHEN m1 THEN r1 ... WHEN mk THEN rk ELSE d END`.
     * The rewrite repeats `e`, which [RedshiftAstBuilder.expanded] bounds. The simple `CASE e WHEN s1 ...` would write
     * `e` once, but the planner expands it to the same `e = si` branches, so the repetition would only leave the bound's
     * sight.
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DECODE_expression.html
     */
    private fun decode(
        name: Token,
        args: List<P.ExprContext>,
    ): Expr {
        arity(name, args, 3, null)
        return builder.nested(name) {
            val exprs = args.map(builder::argument)
            val e = exprs[0]
            val pairs = (args.size - 1) / 2
            val searches = (0 until pairs).map { exprs[1 + 2 * it] }
            val results = (0 until pairs).map { exprs[2 + 2 * it] }
            val default = if (args.size % 2 == 0) exprs.last() else null
            val branches =
                searches.indices.map { i ->
                    val s = searches[i]
                    val match =
                        when {
                            s.isNullLiteral() -> exprNullPredicate(e, false)
                            e.isNullLiteral() -> exprNullPredicate(s, false)
                            s.isLiteral() || e.isLiteral() -> exprOperator("=", e, s)
                            else ->
                                builder.nested(name) {
                                    val bothNull =
                                        builder.nested(
                                            name,
                                        ) { exprAnd(exprNullPredicate(e, false), exprNullPredicate(s, false)) }
                                    exprOr(exprOperator("=", e, s), bothNull)
                                }
                        }
                    exprCaseBranch(match, results[i])
                }
            builder.expanded(name, exprCase(null, branches, default))
        }
    }

    /**
     * `GREATEST(v1, ..., vn)` and `LEAST(v1, ..., vn)` are the largest and the smallest argument, NULL arguments left
     * out, and NULL when every argument is NULL. A NULL literal is left out here; one value left is `COALESCE(v)`, which
     * keeps the item's own name (a bare column would lend it the column's); for more,
     * `CASE WHEN c1 THEN v1 ... WHEN c(n-1) THEN v(n-1) ELSE vn END` where `ci` is the conjunction over `j > i` of
     * `vi >= COALESCE(vj, vi)` (`<=` for `LEAST`), which is unknown when `vi` is NULL, so the branch falls through, and
     * true against a NULL `vj`. The comparison with a literal `vj` is written without the `COALESCE`. The rewrite has a
     * comparison for each pair of arguments, which [RedshiftAstBuilder.expanded] bounds.
     * https://docs.aws.amazon.com/redshift/latest/dg/r_GREATEST_LEAST.html
     */
    private fun extremum(
        name: Token,
        args: List<P.ExprContext>,
        greatest: Boolean,
    ): Expr {
        val values = args.map(builder::argument).filterNot { it.isNullLiteral() }
        if (values.isEmpty()) return exprLit(Literal.nul())
        if (values.size == 1) return exprCoalesce(values)
        // Each pair of arguments is a comparison of at least NODES_PER_PAIR nodes; too many pairs is known before they are built.
        if (values.size.toLong() * (values.size - 1) / 2 * NODES_PER_PAIR > RedshiftAstBuilder.MAX_SIZE) throw builder.tooLarge(name)
        val op = if (greatest) ">=" else "<="
        return builder.nested(name) {
            val branches =
                (0 until values.lastIndex).map { i ->
                    val conditions =
                        (i + 1..values.lastIndex).map { j ->
                            val other = if (values[j].isLiteral()) values[j] else exprCoalesce(listOf(values[j], values[i]))
                            exprOperator(op, values[i], other)
                        }
                    exprCaseBranch(conjunction(name, conditions), values[i])
                }
            builder.expanded(name, exprCase(null, branches, values.last()))
        }
    }

    /** The conjunction of [terms], as a balanced tree of `AND`s, so that its depth is the logarithm of their number. */
    private fun conjunction(
        at: Token,
        terms: List<Expr>,
    ): Expr {
        if (terms.size == 1) return terms[0]
        val half = terms.size / 2
        return builder.nested(at) { exprAnd(conjunction(at, terms.subList(0, half)), conjunction(at, terms.subList(half, terms.size))) }
    }

    private fun Expr.isNullLiteral(): Boolean = this is ExprLit && lit.code() == Literal.NULL

    /** A literal that is not NULL, with or without a sign: a value that is known not to be NULL. */
    private fun Expr.isLiteral(): Boolean =
        when (this) {
            is ExprLit -> lit.code() != Literal.NULL
            is ExprOperator -> lhs == null && (symbol == "-" || symbol == "+") && rhs.isLiteral()
            else -> false
        }

    /** Stops when the number of [args] is not between [min] and [max]: Redshift has no such function. */
    private fun arity(
        name: Token,
        args: List<P.ExprContext>,
        min: Int,
        max: Int?,
    ) {
        if (args.size < min || (max != null && args.size > max)) {
            val takes =
                when {
                    max == null -> "at least $min"
                    min == max -> "$min"
                    else -> "$min to $max"
                }
            throw TranslationStop.invalid(name, "${name.text.uppercase()} takes $takes arguments, not ${args.size}")
        }
    }

    private companion object {
        /** The fewest nodes a pair of `GREATEST` arguments adds: a comparison, its `AND`, and two one-node operands. */
        const val NODES_PER_PAIR = 4
    }
}
