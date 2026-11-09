package org.partiql.scribe.frontend.redshift.internal.functions

import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.expr.Expr
import org.partiql.scribe.frontend.redshift.internal.RedshiftAstBuilder
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * The math functions: `POWER`, its synonym `POW`, and the exponentiation operator `^`.
 *
 * PartiQL's built-in functions define no power, so the three are written as the call
 * `pow(CAST(a AS DOUBLE PRECISION), CAST(b AS DOUBLE PRECISION))`, for a catalog that defines [POW]. Redshift's
 * `POWER` converts its arguments to `DOUBLE PRECISION` and returns one; the casts are written here so that a catalog
 * with one `pow` overload per numeric type (`Pow.overloads()` of `org.partiql:partiql-function-extensions`, which
 * this repository's `partiql-scribe-function-extensions` compiles for each target) resolves to the `DOUBLE PRECISION`
 * one and not, for `POWER(1.5, 2)`, to the integer one with the arguments cast down. Against a catalog without `pow`,
 * the planner reports the function as not found.
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

    /** `pow(CAST(base AS DOUBLE PRECISION), CAST(exponent AS DOUBLE PRECISION))`, the form of `POWER`, `POW`, and `^`. */
    fun power(
        base: Expr,
        exponent: Expr,
    ): Expr = exprCall(Identifier.regular(POW), listOf(double(base), double(exponent)), null)

    private fun double(expr: Expr): Expr = exprCast(expr, DataType.DOUBLE_PRECISION())

    internal companion object {
        /**
         * The name of the power function the call is written with: that of `Pow` in `partiql-function-extensions`, the
         * catalog function a deployment is most likely to have. SQL names the function `POWER`; PartiQL has no built-in
         * of either name.
         */
        const val POW = "pow"
    }
}
