package org.partiql.scribe.extensions.functions

import org.partiql.ast.Ast.exprBag
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprInCollection
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.from
import org.partiql.ast.Ast.fromExpr
import org.partiql.ast.Ast.queryBodySFW
import org.partiql.ast.Ast.selectItemExpr
import org.partiql.ast.Ast.selectList
import org.partiql.ast.DataType
import org.partiql.ast.FromType
import org.partiql.ast.Identifier
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprPath
import org.partiql.ast.expr.PathStep
import org.partiql.plan.RoutineRef
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.targets.redshift.RedshiftCalls
import org.partiql.scribe.targets.redshift.RedshiftTarget

public class RedshiftFunctionExtensionTarget(
    routines: Set<RoutineRef>,
) : RedshiftTarget() {
    private val routines: Set<RoutineRef> = routines.toSet()

    override fun getCalls(context: ScribeContext): SqlCalls = RedshiftFunctionExtensionCalls(context, routines)
}

/**
 * Redshift translations for explicitly bound PartiQL function extensions.
 */
public open class RedshiftFunctionExtensionCalls(
    context: ScribeContext,
    routines: Set<RoutineRef>,
) : RedshiftCalls(context) {
    private val listener = context.getProblemListener()

    public override val routineRules: Map<RoutineRef, SqlCallFn> =
        bind(
            routines,
            commonRules(this, listener) +
                mapOf(
                    CONTAINS to ::contains,
                    TO_UNIXTIME to ::toUnixTime,
                    HEX_TO_BIGINT to ::hexToBigInt,
                ),
        )

    private fun toUnixTime(args: SqlArgs): Expr =
        exprCast(
            exprCall(
                Identifier.regular("DATE_PART"),
                listOf(exprVarRef(Identifier.regular("EPOCH"), false), args[0].expr),
            ),
            DataType.BIGINT(),
        )

    private fun hexToBigInt(args: SqlArgs): Expr =
        exprCall(
            Identifier.regular("STRTOL"),
            listOf(args[0].expr, intLiteral(16)),
        )

    private fun contains(args: SqlArgs): Expr {
        val path = args.getOrNull(0)?.expr as? ExprPath
        val element = args.getOrNull(1)?.expr as? ExprLit
        if (args.size != 2 || path == null || element == null) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.UNSUPPORTED_OPERATION,
                    "Redshift `contains` requires an array path and a literal element",
                ),
            )
        }
        val alias =
            when (val step = path.steps.lastOrNull()) {
                is PathStep.Field -> step.field.text
                is PathStep.Element -> (step.element as? ExprLit)?.lit?.stringValue()
                else -> null
            } ?: listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.UNSUPPORTED_OPERATION,
                    "Redshift `contains` requires an array path ending in a named field",
                ),
            )
        val variable = exprVarRef(Identifier.regular(alias), false)
        val query =
            exprQuerySet(
                queryBodySFW(
                    select =
                        selectList(
                            listOf(
                                selectItemExpr(
                                    exprCall(Identifier.regular("COUNT"), listOf(variable)),
                                ),
                            ),
                        ),
                    from =
                        from(
                            listOf(
                                fromExpr(
                                    path,
                                    FromType.SCAN(),
                                    variable.identifier.identifier,
                                ),
                            ),
                        ),
                    where = exprInCollection(variable, exprBag(listOf(element)), false),
                ),
            )
        return exprOperator("<=", intLiteral(1), query)
    }
}
