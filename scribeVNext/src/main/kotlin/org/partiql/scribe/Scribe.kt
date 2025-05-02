package org.partiql.scribe

import org.partiql.parser.PartiQLParser
import org.partiql.plan.Plan
import org.partiql.planner.PartiQLPlanner
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.spi.Context
import org.partiql.spi.catalog.Session

public class Scribe internal constructor(
    private val parser: PartiQLParser,
    private val planner: PartiQLPlanner,
    private val scribeContext: ScribeContext,
){
    companion object {
        val STANDARD = Scribe(PartiQLParser.standard(), PartiQLPlanner.standard(), ScribeContext.standard())
    }

    // entry point for converting text to other dialect's text
    public fun <T> compile(
        statement: String,
        target: ScribeTarget<T>,
        session: Session,
        partiqlContext: Context = Context.standard(),
    ): Result<T> {
        // TEXT -> AST
        val parserResult = parser.parse(statement, partiqlContext)
        val parsedStatements = parserResult.statements

        if (parsedStatements.size != 1) {
            scribeContext.getErrorListener().report(
                ScribeProblem.simpleError(
                    code = ScribeProblem.UNSUPPORTED_OPERATION,
                    "Only one statement is supported at a time."
                )
            )
        }
        val ast = parsedStatements[0]

        // AST -> PLAN
        val plannerResult = planner.plan(ast, session)
        val plan = plannerResult.plan
        return compile(plan, session, target)
    }

    public fun <T> compile(plan: Plan, session: Session, target: ScribeTarget<T>): Result<T> {
        // TODO run plan validation pass to ensure no error nodes
        val output = target.compile(plan, session, context = scribeContext)
        return Result(plan, output)
    }

    public class Result<T>(
        val input: Plan,
        val output: ScribeOutput<T>,
    )
}
