package org.partiql.scribe.frontend.redshift.internal.functions

import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprOperator
import org.partiql.scribe.frontend.redshift.internal.RedshiftAstBuilder
import java.math.BigDecimal
import java.math.BigInteger
import org.partiql.scribe.frontend.redshift.antlr.ScribeRedshiftParser as P

/**
 * The string functions: `UPPER`, `LOWER`, and `CONCAT`, and the concatenation operator `||`. `UPPER` and `LOWER` are
 * PartiQL's own; `CONCAT(a, b)` is `a || b`, which Redshift documents as the same operation.
 *
 * Redshift converts an operand of `||` that is not a character string to `VARCHAR`, so that `'seats ' || 73298` is
 * `'seats 73298'`. The conversion depends on the operand's type, which the front end does not see, except for a numeric
 * literal: it converts to its own digits, and is written as that string here. Any other operand is left as it is, and
 * the planner reports a type mismatch where PartiQL does not define `||` on the operand types.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/String_functions_header.html
 */
internal class StringFunctions(private val builder: RedshiftAstBuilder) {
    /** Translates a call of [name] with [args], or returns null when [name] is not a string function. */
    fun translate(
        name: Token,
        args: List<P.ExprContext>,
    ): Expr? =
        when (name.type) {
            P.UPPER -> caseFold(name, args, "upper")
            P.LOWER -> caseFold(name, args, "lower")
            P.CONCAT_FUNCTION -> concat(name, args)
            else -> null
        }

    /**
     * `UPPER(s)` and `LOWER(s)` are PartiQL's `UPPER` and `LOWER`: the result has the type of the argument, and a
     * `CHAR(n)` argument keeps its padding, in both languages.
     * https://docs.aws.amazon.com/redshift/latest/dg/r_UPPER.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_LOWER.html
     */
    private fun caseFold(
        name: Token,
        args: List<P.ExprContext>,
        function: String,
    ): Expr {
        builder.arity(name, args, 1)
        return builder.nested(name) { exprCall(Identifier.regular(function), listOf(builder.argument(args[0])), null) }
    }

    /**
     * `CONCAT(a, b)` is `a || b`; see [concatenation]. The arguments become the operands of `||`, not of a call, and
     * the PartiQL printer writes a subquery operand with its parentheses, so a subquery is allowed here.
     * https://docs.aws.amazon.com/redshift/latest/dg/r_CONCAT.html
     */
    private fun concat(
        name: Token,
        args: List<P.ExprContext>,
    ): Expr {
        builder.arity(name, args, 2)
        return builder.nested(name) { concatenation(builder.expr(args[0]), builder.expr(args[1])) }
    }

    /**
     * `a || b`, NULL when either operand is NULL in both languages. A numeric literal operand is written as the string
     * Redshift converts it to: an integer without its leading zeros (`007` is `'7'`), a decimal with its scale (`1.50` is
     * `'1.50'`, `.5` is `'0.5'`), a sign kept (`-1` is `'-1'`, `+2` is `'2'`).
     * https://docs.aws.amazon.com/redshift/latest/dg/r_concat_op.html
     */
    fun concatenation(
        lhs: Expr,
        rhs: Expr,
    ): Expr = exprOperator("||", text(lhs), text(rhs))

    /** [operand] as the string literal Redshift converts it to when it is a numeric literal, else [operand] itself. */
    private fun text(operand: Expr): Expr = digits(operand)?.let { exprLit(Literal.string(it)) } ?: operand

    /** The digits of a numeric literal, with its sign; null when [operand] is not one. */
    private fun digits(operand: Expr): String? =
        when (operand) {
            is ExprLit ->
                when (operand.lit.code()) {
                    Literal.INT_NUM -> BigInteger(operand.lit.numberValue()).toString()
                    Literal.EXACT_NUM -> operand.lit.bigDecimalValue().toPlainString()
                    else -> null
                }
            is ExprOperator ->
                when {
                    operand.lhs != null -> null
                    operand.symbol == "+" -> digits(operand.rhs)
                    operand.symbol == "-" -> digits(operand.rhs)?.let { BigDecimal(it).negate().toPlainString() }
                    else -> null
                }
            else -> null
        }
}
