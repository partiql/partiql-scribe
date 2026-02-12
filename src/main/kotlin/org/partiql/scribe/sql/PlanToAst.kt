package org.partiql.scribe.sql

import org.partiql.ast.Ast.identifier
import org.partiql.ast.Ast.identifierSimple
import org.partiql.ast.Ast.query
import org.partiql.ast.AstNode
import org.partiql.ast.expr.Expr
import org.partiql.plan.Action
import org.partiql.plan.Plan
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.spi.catalog.Session
import org.partiql.ast.Identifier as AstIdentifier
import org.partiql.spi.catalog.Identifier as SpiIdentifier

/**
 * Translates a [Plan] to an [AstNode].
 *
 * @param session the current session
 * @param calls the [SqlCalls] to use for translating function calls
 * @param context the [ScribeContext] to use for error reporting and other Scribe-specific context fields
 */
public open class PlanToAst(
    private val session: Session,
    private val calls: SqlCalls,
    private val context: ScribeContext,
) {
    private val listener = context.getProblemListener()

    public open fun apply(plan: Plan): AstNode {
        when (val action = plan.action) {
            is Action.Query -> {
                val transform = getRexConverter(Locals(emptyList()))
                val expr = transform.apply(action.rex)
                return query(expr)
            }
            else ->
                listener.reportAndThrow(
                    ScribeProblem.simpleError(
                        ScribeProblem.UNSUPPORTED_OPERATION,
                        "Can only translate a query statement. Received $action",
                    ),
                )
        }
    }

    public open fun getRexConverter(locals: Locals): RexConverter = RexConverter(this, locals, context)

    @JvmOverloads
    public open fun getRelConverter(outer: Locals? = null): RelConverter = RelConverter(this, context, outer)

    public open fun getFunction(
        name: String,
        args: SqlArgs,
    ): Expr = calls.retarget(name, args)

    public open fun getGlobal(ref: SpiIdentifier): AstIdentifier? {
        val currentCatalogName = session.getCatalog()
        val currentCatalog = session.getCatalogs().getCatalog(currentCatalogName)
        val resolved = currentCatalog?.resolveTable(session, ref) ?: return null
        return when (resolved.hasNamespace()) {
            true ->
                identifier(
                    qualifier =
                        listOf(identifierSimple(currentCatalogName, isRegular = false)) +
                            resolved.getNamespace().getLevels().map {
                                identifierSimple(it, isRegular = false)
                            },
                    identifier = identifierSimple(resolved.getName(), isRegular = false),
                )
            false ->
                identifier(
                    qualifier = listOf(identifierSimple(currentCatalogName, isRegular = false)),
                    identifier = identifierSimple(resolved.getName(), isRegular = false),
                )
        }
    }
}
