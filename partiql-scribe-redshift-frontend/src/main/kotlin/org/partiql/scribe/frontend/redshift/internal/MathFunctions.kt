package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Identifier
import org.partiql.ast.expr.Expr
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * The math functions: `POWER`, its synonym `POW`, and the exponentiation operator `^`.
 *
 * PartiQL's built-in functions define no power, so the three are written as a call of `pow(a, b)`, for a catalog that
 * defines `pow` over two `DOUBLE PRECISION` arguments with a `DOUBLE PRECISION` result: Redshift's `POWER` converts its
 * arguments to `DOUBLE PRECISION` and returns one, and the planner inserts the same conversions when it resolves the
 * call. Against a catalog without `pow`, the planner reports the function as not found.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/Math_functions.html
 */
internal class MathFunctions(private val builder: RedshiftAstBuilder) {
    /** Translates a call of [name] with [args], or returns null when [name] is not a math function. */
    fun translate(
        name: Token,
        args: List<P.ExprContext>,
    ): Expr? =
        when (name.type) {
            P.POWER, P.POW -> power(name, args)
            else -> null
        }

    /**
     * `POWER(a, b)` and `POW(a, b)` are `a` raised to the power `b`; see [power].
     * https://docs.aws.amazon.com/redshift/latest/dg/r_POWER.html
     */
    private fun power(
        name: Token,
        args: List<P.ExprContext>,
    ): Expr {
        builder.arity(name, args, 2)
        return builder.nested(name) { power(builder.argument(args[0]), builder.argument(args[1])) }
    }

    /** `pow(base, exponent)`, the form of `POWER`, `POW`, and `^`. */
    fun power(
        base: Expr,
        exponent: Expr,
    ): Expr = exprCall(Identifier.regular("pow"), listOf(base, exponent), null)
}
