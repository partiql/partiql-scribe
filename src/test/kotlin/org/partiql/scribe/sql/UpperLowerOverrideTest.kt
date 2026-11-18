package org.partiql.scribe.sql

import org.junit.jupiter.api.Test
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Identifier
import org.partiql.ast.expr.Expr
import org.partiql.parser.PartiQLParser
import org.partiql.planner.PartiQLPlanner
import org.partiql.scribe.Scribe
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.targets.spark.SparkCalls
import org.partiql.scribe.targets.spark.SparkTarget
import org.partiql.scribe.utils.SessionProvider
import kotlin.test.assertEquals

class UpperLowerOverrideTest {
    @Test
    fun `a target without its own rule keeps the default output`() {
        assertEquals("SELECT `upper`(`T`.`c`) AS `_1` FROM `default`.`T` AS `T`", compile("SELECT UPPER(c) FROM T", SparkTarget()))
        assertEquals("SELECT `lower`(`T`.`c`) AS `_1` FROM `default`.`T` AS `T`", compile("SELECT LOWER(c) FROM T", SparkTarget()))
    }

    @Test
    fun `any target can override upper and lower`() {
        assertEquals("SELECT `custom_upper`(`T`.`c`) AS `_1` FROM `default`.`T` AS `T`", compile("SELECT UPPER(c) FROM T", CustomSpark))
        assertEquals("SELECT `custom_lower`(`T`.`c`) AS `_1` FROM `default`.`T` AS `T`", compile("SELECT LOWER(c) FROM T", CustomSpark))
    }

    private object CustomSpark : SparkTarget() {
        override fun getCalls(context: ScribeContext): SqlCalls =
            object : SparkCalls(context) {
                override fun upper(args: SqlArgs): Expr = exprCall(Identifier.regular("custom_upper"), args.map { it.expr })

                override fun lower(args: SqlArgs): Expr = exprCall(Identifier.regular("custom_lower"), args.map { it.expr })
            }
    }

    private fun compile(
        query: String,
        target: SqlTarget,
    ): String {
        val session = SessionProvider().getSession()
        val statement = PartiQLParser.standard().parse(query).statements.single()
        val plan = PartiQLPlanner.standard().plan(statement, session).plan
        return Scribe(ScribeContext.standard()).compile(plan, session, target).output.value
    }
}
