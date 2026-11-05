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
import org.partiql.ast.AstNode
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprQuerySet
import java.util.IdentityHashMap
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * The conditional functions: `NVL`, `COALESCE`, `NULLIF`, `NVL2`, `DECODE`, `GREATEST`, and `LEAST`. Each rule rewrites
 * a call into the PartiQL expression with the same value. `COALESCE` and `NULLIF` are PartiQL's own; the others become
 * `CASE` expressions. Where Redshift converts the arguments of one of these functions to a common type, the planner
 * types the PartiQL expression afterwards, as for any expression of the query.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/c_conditional_expressions.html
 */
internal class ConditionalFunctions(private val builder: RedshiftAstBuilder) {
    /** Translates a call of [name] with [args], or returns null when [name] is not a conditional function. */
    fun translate(
        name: Token,
        args: List<P.ExprContext>,
    ): Expr? =
        when (foldAscii(name.text)) {
            "nvl", "coalesce" -> coalesce(name, args)
            "nullif" -> nullif(name, args)
            "nvl2" -> nvl2(name, args)
            "decode" -> decode(name, args)
            "greatest" -> extremum(name, args, greatest = true)
            "least" -> extremum(name, args, greatest = false)
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
    ): Expr = builder.nested(name) { exprCoalesce(args.map(builder::expr)) }

    /**
     * `NULLIF(a, b)` is NULL when `a` equals `b`, and `a` otherwise, as PartiQL's `NULLIF` is.
     * https://docs.aws.amazon.com/redshift/latest/dg/r_NULLIF_function.html
     */
    private fun nullif(
        name: Token,
        args: List<P.ExprContext>,
    ): Expr {
        arity(name, args, 2, 2)
        return builder.nested(name) { exprNullIf(builder.expr(args[0]), builder.expr(args[1])) }
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
            val (e, a, b) = args.map(builder::expr)
            exprCase(null, listOf(exprCaseBranch(exprNullPredicate(e, true), a)), b)
        }
    }

    /**
     * `DECODE(e, s1, r1, ..., sk, rk [, d])` is the result of the first search value that matches `e`, else `d`, else
     * NULL. A NULL search value matches a NULL `e`, which `=` does not, so the match is `e IS NULL` for the NULL literal,
     * `e = s` for another literal, and `e = s OR (e IS NULL AND s IS NULL)` for a search value that may be NULL:
     * `CASE WHEN m1 THEN r1 ... WHEN mk THEN rk ELSE d END`. The rewrite repeats `e`; see [bounded].
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DECODE_expression.html
     */
    private fun decode(
        name: Token,
        args: List<P.ExprContext>,
    ): Expr {
        arity(name, args, 3, null)
        return builder.nested(name) {
            val exprs = args.map(builder::expr)
            val e = exprs[0]
            val pairs = (args.size - 1) / 2
            val branches =
                (0 until pairs).map { i ->
                    val s = exprs[1 + 2 * i]
                    val match =
                        when {
                            s.isNullLiteral() -> exprNullPredicate(e, false)
                            s is ExprLit -> exprOperator("=", e, s)
                            else ->
                                builder.nested(name) {
                                    val bothNull =
                                        builder.nested(
                                            name,
                                        ) { exprAnd(exprNullPredicate(e, false), exprNullPredicate(s, false)) }
                                    exprOr(exprOperator("=", e, s), bothNull)
                                }
                        }
                    exprCaseBranch(match, exprs[2 + 2 * i])
                }
            val default = if (args.size % 2 == 0) exprs.last() else null
            bounded(name, exprCase(null, branches, default))
        }
    }

    /**
     * `GREATEST(v1, ..., vn)` and `LEAST(v1, ..., vn)` are the largest and the smallest argument, NULL arguments left
     * out, and NULL when every argument is NULL. A NULL literal is left out here; for the rest,
     * `CASE WHEN c1 THEN v1 ... WHEN c(n-1) THEN v(n-1) ELSE vn END` where `ci` is the conjunction over `j > i` of
     * `vi >= COALESCE(vj, vi)` (`<=` for `LEAST`), which is unknown when `vi` is NULL, so the branch falls through, and
     * true against a NULL `vj`. The comparison with a literal `vj` is written without the `COALESCE`. The rewrite
     * repeats every argument; see [bounded].
     * https://docs.aws.amazon.com/redshift/latest/dg/r_GREATEST_LEAST.html
     */
    private fun extremum(
        name: Token,
        args: List<P.ExprContext>,
        greatest: Boolean,
    ): Expr {
        val values = args.map(builder::expr).filterNot { it.isNullLiteral() }
        if (values.isEmpty()) return exprLit(Literal.nul())
        if (values.size == 1) return values[0]
        // The result has a comparison for each pair of arguments; too many pairs is known before they are built.
        if (values.size.toLong() * (values.size - 1) / 2 > MAX_REWRITE_SIZE) tooLarge(name)
        val op = if (greatest) ">=" else "<="
        return builder.nested(name) {
            val branches =
                (0 until values.lastIndex).map { i ->
                    val conditions =
                        (i + 1..values.lastIndex).map { j ->
                            val other = if (values[j] is ExprLit) values[j] else exprCoalesce(listOf(values[j], values[i]))
                            exprOperator(op, values[i], other)
                        }
                    exprCaseBranch(conjunction(name, conditions), values[i])
                }
            bounded(name, exprCase(null, branches, values.last()))
        }
    }

    /**
     * A rewrite that repeats its arguments grows faster than the call it replaces, and a call nested in such a call is
     * repeated too, so the size of the result is checked against [MAX_REWRITE_SIZE]: the nodes of the expression as
     * the planner and the targets walk it, a repeated argument counted at each use. The depth bound cannot catch this
     * growth, since the depth of the result is the depth of the call.
     */
    private fun bounded(
        name: Token,
        result: Expr,
    ): Expr {
        if (size(result) > MAX_REWRITE_SIZE) tooLarge(name)
        return result
    }

    private fun tooLarge(name: Token): Nothing =
        throw TranslationStop.unsupported(
            name,
            "${name.text.uppercase()} rewritten to a CASE expression of more than $MAX_REWRITE_SIZE nodes (its arguments are repeated)",
        )

    /** The sizes of the nodes seen so far, by identity: a repeated argument is one object, and its size is computed once. */
    private val sizes = IdentityHashMap<AstNode, Long>()

    private fun size(node: AstNode): Long =
        sizes.getOrPut(node) {
            val children = node.children + listOfNotNull((node as? ExprQuerySet)?.with)
            1 + children.sumOf(::size)
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

    companion object {
        /**
         * The most nodes a rewrite that repeats its arguments may produce, a repeated argument counted at each use. The
         * planner and the targets walk the expression as a tree, so this bounds their work; it is far above any call a
         * query writes by hand, and it stops a chain of nested calls, which grows geometrically.
         */
        const val MAX_REWRITE_SIZE = 100_000L
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
}
