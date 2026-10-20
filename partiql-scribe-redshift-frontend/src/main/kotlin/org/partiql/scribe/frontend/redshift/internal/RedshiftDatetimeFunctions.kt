package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Identifier
import org.partiql.ast.expr.Expr

/**
 * The rules that translate Redshift date and time functions with the function call syntax; see [RedshiftFunctions].
 * `DATEADD`, `DATEDIFF`, `EXTRACT`, and the datetime keywords are in [RedshiftDatetime].
 */
internal class RedshiftDatetimeFunctions(
    private val functions: RedshiftFunctions,
) {
    val rules: Map<String, RedshiftFunctions.Rule> =
        mapOf(
            // https://docs.aws.amazon.com/redshift/latest/dg/r_GETDATE.html
            "getdate" to
                RedshiftFunctions.Rule { name, args, _ ->
                    functions.arity(name, args, 0)
                    utcnow(name, "GETDATE()")
                },
        )

    /**
     * Redshift returns TIMESTAMP (without time zone) in the session time zone; PartiQL `UTCNOW()` returns TIMESTAMP
     * WITH TIME ZONE in UTC. The values are equal when the session time zone is UTC, which is the Redshift default.
     */
    fun utcnow(
        name: Token,
        source: String,
    ): Expr {
        functions.note(
            name,
            "Redshift `$source` was replaced by PartiQL `UTCNOW()`; the result type is TIMESTAMP WITH TIME ZONE in UTC",
        )
        return exprCall(Identifier.regular("utcnow"), emptyList(), null)
    }
}
