package org.partiql.scribe.targets.redshift

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.DatetimeField
import org.partiql.ast.Identifier
import org.partiql.ast.expr.Expr
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.sql.utils.unquotedStringExpr

public open class RedshiftCalls(context: ScribeContext) : SqlCalls(context) {
    private val listener = context.getProblemListener()

    override val rules: Map<String, SqlCallFn> =
        super.rules.toMutableMap().apply {
            this["utcnow"] = ::utcnow
            // Extensions
            this["split"] = ::split
        }

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/r_SYSDATE.html
     */
    private fun utcnow(args: SqlArgs): Expr {
        val id = Identifier.regular("sysdate")
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `utcnow()` was replaced by Redshift `SYSDATE`",
            ),
        )
        return exprVarRef(id, isQualified = false)
    }

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/split_to_array.html
     */
    private fun split(args: SqlArgs): Expr {
        val id = Identifier.regular("split_to_array")
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message =
                    "PartiQL `split(<string>, <string>) -> list<string>` was replaced by Redshift " +
                        "`split_to_array(<string>, <string>) -> SUPER`",
            ),
        )
        val arg0 = args[0].expr
        val arg1 = args[1].expr
        return exprCall(id, listOf(arg0, arg1))
    }

    override fun dateAdd(
        part: DatetimeField,
        args: SqlArgs,
    ): Expr {
        val id = Identifier.regular("DATEADD")
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `date_add` was replaced by Redshift `dateadd`",
            ),
        )
        val arg0 = unquotedStringExpr(part.name().uppercase())
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        return exprCall(id, listOf(arg0, arg1, arg2))
    }

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATEDIFF_function.html
     */
    override fun dateDiff(
        part: DatetimeField,
        args: SqlArgs,
    ): Expr {
        val id = Identifier.regular("DATEDIFF")
        listener.report(
            ScribeProblem.simpleInfo(
                code = ScribeProblem.TRANSLATION_INFO,
                message = "PartiQL `date_diff` was replaced by Redshift `datediff`",
            ),
        )
        val arg0 = unquotedStringExpr(part.name().uppercase())
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        return exprCall(id, listOf(arg0, arg1, arg2))
    }
}
