package org.partiql.scribe.sql

import org.partiql.ast.Expr
import org.partiql.ast.identifierQualified
import org.partiql.ast.identifierSymbol
import org.partiql.ast.statementQuery
import org.partiql.plan.Global
import org.partiql.plan.Identifier
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.ScribeProblem
import org.partiql.ast.Identifier as AstIdentifier
import org.partiql.ast.Statement as AstStatement
import org.partiql.plan.Identifier as PlanIdentifier
import org.partiql.plan.Statement as PlanStatement

/**
 * [SqlTransform] represents extendable logic for translating from a [PlanNode] to [AstNode] tree.
 */
public open class SqlTransform(
    private val globals: List<Global>,
    private val calls: SqlCalls,
    private val onProblem: ProblemCallback,
) {

    public fun apply(statement: PlanStatement): AstStatement {
        if (statement is PlanStatement.Query) {
            val transform = RexToSql(this, Locals(emptyList()))
            val expr = transform.apply(statement.root)
            return statementQuery(expr)
        }
        throw UnsupportedOperationException("Can only transform a query statement")
    }

    public fun getGlobal(ref: Int): AstIdentifier.Qualified? {
        val g = globals.getOrNull(ref) ?: return null
        return translate(g.path)
    }

    public fun getFunction(name: String, args: SqlArgs): Expr = calls.retarget(name, args)

    public fun handleProblem(problem: ScribeProblem) = onProblem(problem)

    // Helpers

    companion object {

        public fun translate(identifier: PlanIdentifier): AstIdentifier = when (identifier) {
            is PlanIdentifier.Qualified -> translate(identifier)
            is PlanIdentifier.Symbol -> translate(identifier)
        }

        public fun translate(identifier: PlanIdentifier.Symbol): AstIdentifier.Symbol {
            return identifierSymbol(
                symbol = identifier.symbol,
                caseSensitivity = translate(identifier.caseSensitivity),
            )
        }

        public fun translate(identifier: PlanIdentifier.Qualified): AstIdentifier.Qualified {
            return identifierQualified(
                root = translate(identifier.root),
                steps = identifier.steps.map { translate(it) },
            )
        }

        public fun id(symbol: String): AstIdentifier.Symbol = identifierSymbol(
            symbol = symbol,
            caseSensitivity = AstIdentifier.CaseSensitivity.SENSITIVE,
        )

        private fun translate(case: Identifier.CaseSensitivity) = when (case) {
            PlanIdentifier.CaseSensitivity.SENSITIVE -> AstIdentifier.CaseSensitivity.SENSITIVE
            PlanIdentifier.CaseSensitivity.INSENSITIVE -> AstIdentifier.CaseSensitivity.INSENSITIVE
        }
    }
}
