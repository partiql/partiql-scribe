package org.partiql.scribe.targets.redshift

import org.junit.jupiter.api.Test
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Identifier
import org.partiql.ast.expr.Expr
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.scribe.Scribe
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.utils.SessionProvider
import kotlin.test.assertEquals

class RedshiftCallsTest {
    // The default output is covered by builtins/strings.sql; the .sql fixtures can't use a custom subclass.
    @Test
    fun `a subclass can override upper and lower`() {
        assertEquals(
            "SELECT custom_upper(\"T\".\"c\") AS \"_1\" FROM \"default\".\"T\" AS \"T\"",
            compile("SELECT UPPER(c) FROM T", CustomTarget),
        )
        // An override can still call super to keep the SUPER cast.
        assertEquals(
            "SELECT TRIM(LOWER(CAST(\"T\".\"d\".\"e\" AS VARCHAR))) AS \"_1\" FROM \"default\".\"T\" AS \"T\"",
            compile("SELECT LOWER(d.e) FROM T", CustomTarget),
        )
    }

    private object CustomTarget : RedshiftTarget() {
        override fun getCalls(context: ScribeContext): SqlCalls =
            object : RedshiftCalls(context) {
                override fun upper(args: SqlArgs): Expr = exprCall(Identifier.regular("custom_upper"), args.map { it.expr })

                override fun lower(args: SqlArgs): Expr = exprCall(Identifier.regular("TRIM"), listOf(super.lower(args)))
            }
    }

    private fun compile(
        query: String,
        target: RedshiftTarget,
    ): String {
        val session = SessionProvider().getSession()
        val statement = PartiQLParser.standard().parse(query).statements.single()
        val plan = PartiQLPlanner.standard().plan(statement, session).plan
        return Scribe(ScribeContext.standard()).compile(plan, session, target).output.value
    }
}
