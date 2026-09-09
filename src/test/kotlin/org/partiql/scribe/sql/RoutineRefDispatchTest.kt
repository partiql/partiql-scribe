package org.partiql.scribe.sql

import org.junit.jupiter.api.Test
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.AstNode
import org.partiql.ast.Identifier
import org.partiql.ast.SetQuantifier
import org.partiql.ast.expr.Expr
import org.partiql.parser.PartiQLParser
import org.partiql.plan.Plan
import org.partiql.plan.RoutineRef
import org.partiql.planner.PartiQLPlanner
import org.partiql.scribe.Scribe
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.targets.redshift.RedshiftCalls
import org.partiql.scribe.targets.redshift.RedshiftTarget
import org.partiql.scribe.targets.spark.SparkCalls
import org.partiql.scribe.targets.spark.SparkTarget
import org.partiql.scribe.utils.PErrorCollector
import org.partiql.spi.Context
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Namespace
import org.partiql.spi.catalog.RoutineBinding
import org.partiql.spi.catalog.RoutineCatalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.catalog.Table
import org.partiql.spi.function.AggOverload
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.MemRoutineProvider
import org.partiql.spi.types.PType
import kotlin.test.assertContains
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.partiql.spi.catalog.Identifier as SpiIdentifier

class RoutineRefDispatchTest {
    private val scalar = function("custom_scalar", PType.integer())
    private val dispatchInteger = function("custom_dispatch", PType.integer())
    private val dispatchString = function("custom_dispatch", PType.string())
    private val nameFallback = function("name_fallback", PType.integer())
    private val aggregate =
        AggOverload.Builder("custom_aggregate")
            .addParameter(PType.integer())
            .returns(PType.integer())
            .build()
    private val payload = Table.empty("payload", PType.dynamic())
    private val routines =
        MemRoutineProvider.builder()
            .register(scalar, Namespace.of("stable"))
            .register(scalar, Namespace.of("experimental"))
            .register(dispatchInteger, Namespace.of("stable"))
            .register(dispatchString, Namespace.of("stable"))
            .register(nameFallback, Namespace.of("stable"))
            .register(aggregate, Namespace.of("stable"))
            .register(aggregate, Namespace.of("experimental"))
            .build()
    private val catalog =
        object : RoutineCatalog {
            override fun getName(): String = "demo"

            override fun getTable(
                session: Session,
                name: Name,
            ): Table? = payload.takeIf { it.getName() == name }

            override fun resolveTable(
                session: Session,
                identifier: SpiIdentifier,
            ): Name? = payload.getName().takeIf { identifier.matches(it.getName(), ignoreCase = true) }

            override fun resolveFunctions(
                session: Session,
                identifier: SpiIdentifier,
            ): Collection<RoutineBinding<FnOverload>> = routines.getFunctions(identifier)

            override fun resolveAggregations(
                session: Session,
                identifier: SpiIdentifier,
            ): Collection<RoutineBinding<AggOverload>> = routines.getAggregations(identifier)
        }
    private val session =
        Session.builder()
            .catalog("demo")
            .catalogs(catalog)
            .path(Namespace.of("demo", "stable"))
            .build()

    @Test
    fun `dispatches by resolved routine identity`() {
        assertContains(compile("custom_scalar(1)", session), "stable_scalar(1)")
        assertContains(compile("demo.experimental.custom_scalar(1)", session), "experimental_scalar(1)")
        assertContains(
            compile("SELECT custom_aggregate(DISTINCT x) FROM << 1, 2 >> AS x", session),
            "stable_aggregate(DISTINCT",
        )
        assertContains(
            compile("SELECT demo.experimental.custom_aggregate(x) FROM << 1 >> AS x", session),
            "experimental_aggregate(",
        )
        assertContains(
            compile("SELECT VALUE custom_dispatch(x) FROM payload AS x", session),
            "stable_dispatch(",
        )
    }

    @Test
    fun `falls back to existing name rules for unmapped identity`() {
        assertContains(compile("name_fallback(1)", session), "name_rule(1)")
    }

    @Test
    fun `preserves custom plan factory fallback`() {
        assertContains(compile("name_fallback(1)", session, FactoryFallbackTarget), "factory_rule(1)")
    }

    @Test
    fun `redshift converter preserves resolved routine identity`() {
        assertContains(compile("custom_scalar(1)", session, RedshiftRoutineTarget), "stable_scalar(1)")
        assertContains(
            compile("SELECT VALUE custom_dispatch(x) FROM payload AS x", session, RedshiftRoutineTarget),
            "stable_dispatch(",
        )
        assertContains(
            compile(
                "SELECT custom_aggregate(DISTINCT x) FROM << 1, 2 >> AS x",
                session,
                RedshiftRoutineTarget,
            ),
            "stable_aggregate(DISTINCT",
        )
    }

    @Test
    fun `spark converter preserves resolved routine identity`() {
        assertContains(compile("custom_scalar(1)", session, SparkRoutineTarget), "stable_scalar")
    }

    @Test
    fun `preserves legacy catalog fallback`() {
        val legacyCatalog =
            object : Catalog {
                override fun getName(): String = "default"

                override fun getTable(
                    session: Session,
                    name: Name,
                ): Table? = payload.takeIf { it.getName() == name }

                override fun resolveTable(
                    session: Session,
                    identifier: SpiIdentifier,
                ): Name? = payload.getName().takeIf { identifier.matches(it.getName(), ignoreCase = true) }

                override fun getFunctions(
                    session: Session,
                    name: String,
                ): Collection<FnOverload> =
                    when (name) {
                        scalar.signature.name -> listOf(scalar)
                        dispatchInteger.signature.name -> listOf(dispatchInteger, dispatchString)
                        else -> emptyList()
                    }

                override fun getAggregations(
                    session: Session,
                    name: String,
                ): Collection<AggOverload> = if (name == aggregate.signature.name) listOf(aggregate) else emptyList()
            }
        val legacySession =
            Session.builder()
                .catalog("default")
                .catalogs(legacyCatalog)
                .build()
        val scalarSql = compile("custom_scalar(1)", legacySession)
        val dispatchSql = compile("SELECT VALUE custom_dispatch(x) FROM payload AS x", legacySession)
        val redshiftDispatchSql =
            compile("SELECT VALUE custom_dispatch(x) FROM payload AS x", legacySession, RedshiftRoutineTarget)
        val aggregateSql = compile("SELECT custom_aggregate(DISTINCT x) FROM << 1, 2 >> AS x", legacySession)

        assertContains(scalarSql, "\"custom_scalar\"(1)")
        assertFalse(scalarSql.contains("stable_scalar"))
        assertContains(dispatchSql, "\"custom_dispatch\"(")
        assertFalse(dispatchSql.contains("stable_dispatch"))
        assertContains(redshiftDispatchSql, "\"custom_dispatch\"(")
        assertFalse(redshiftDispatchSql.contains("stable_dispatch"))
        assertContains(aggregateSql, "custom_aggregate(DISTINCT")
        assertFalse(aggregateSql.contains("stable_aggregate"))
    }

    private fun compile(
        query: String,
        session: Session,
        target: SqlTarget = RoutineTarget,
    ): String {
        val errors = PErrorCollector()
        val context = Context.of(errors)
        val statement = PartiQLParser.standard().parse(query, context).statements.single()
        val plan = PartiQLPlanner.standard().plan(statement, session, context).plan
        assertTrue(errors.errors.isEmpty(), errors.errors.joinToString())
        return Scribe(ScribeContext.standard()).compile(plan, session, target).output.value
    }

    private object RoutineTarget : SqlTarget() {
        override val target: String = "RoutineRef"
        override val version: String = "1"
        override val features: SqlFeatures = SqlFeatures.Permissive()

        override fun getCalls(context: ScribeContext): SqlCalls =
            object : SqlCalls(context) {
                override val rules: Map<String, SqlCallFn> =
                    super.rules + ("name_fallback" to call("name_rule"))
                override val routineRules: Map<RoutineRef, SqlCallFn> =
                    mapOf(
                        RoutineRef("demo", Name.of("stable", "custom_scalar")) to call("stable_scalar"),
                        RoutineRef("demo", Name.of("experimental", "custom_scalar")) to call("experimental_scalar"),
                        RoutineRef("demo", Name.of("stable", "custom_dispatch")) to call("stable_dispatch"),
                    )
                override val aggregateRules: Map<RoutineRef, (SqlArgs, Boolean) -> Expr> =
                    mapOf(
                        RoutineRef("demo", Name.of("stable", "custom_aggregate")) to aggregate("stable_aggregate"),
                        RoutineRef("demo", Name.of("experimental", "custom_aggregate")) to
                            aggregate("experimental_aggregate"),
                    )
            }

        override fun rewrite(
            plan: Plan,
            context: ScribeContext,
        ): Plan = plan

        private fun call(name: String): SqlCallFn =
            { args ->
                exprCall(Identifier.regular(name), args.map { it.expr })
            }

        private fun aggregate(name: String): (SqlArgs, Boolean) -> Expr =
            { args, distinct ->
                exprCall(
                    Identifier.regular(name),
                    args.map { it.expr },
                    if (distinct) SetQuantifier.DISTINCT() else null,
                )
            }
    }

    private object FactoryFallbackTarget : SqlTarget() {
        override val target: String = "FactoryFallback"
        override val version: String = "1"
        override val features: SqlFeatures = SqlFeatures.Permissive()

        override fun rewrite(
            plan: Plan,
            context: ScribeContext,
        ): Plan = plan

        override fun planToAst(
            newPlan: Plan,
            session: Session,
            context: ScribeContext,
        ): AstNode =
            object : PlanToAst(session, getCalls(context), context) {
                override fun getFunction(
                    name: String,
                    args: SqlArgs,
                ) = if (name == "name_fallback") {
                    exprCall(Identifier.regular("factory_rule"), args.map { it.expr })
                } else {
                    super.getFunction(name, args)
                }
            }.apply(newPlan)
    }

    private object RedshiftRoutineTarget : RedshiftTarget() {
        override fun getCalls(context: ScribeContext): SqlCalls =
            object : RedshiftCalls(context) {
                override val routineRules: Map<RoutineRef, SqlCallFn> =
                    mapOf(
                        RoutineRef("demo", Name.of("stable", "custom_scalar")) to call("stable_scalar"),
                        RoutineRef("demo", Name.of("stable", "custom_dispatch")) to call("stable_dispatch"),
                    )
                override val aggregateRules: Map<RoutineRef, (SqlArgs, Boolean) -> Expr> =
                    mapOf(
                        RoutineRef("demo", Name.of("stable", "custom_aggregate")) to
                            { args, distinct ->
                                exprCall(
                                    Identifier.regular("stable_aggregate"),
                                    args.map { it.expr },
                                    if (distinct) SetQuantifier.DISTINCT() else null,
                                )
                            },
                    )

                private fun call(name: String): SqlCallFn =
                    { args ->
                        exprCall(Identifier.regular(name), args.map { it.expr })
                    }
            }
    }

    private object SparkRoutineTarget : SparkTarget() {
        override fun getCalls(context: ScribeContext): SqlCalls =
            object : SparkCalls(context) {
                override val routineRules: Map<RoutineRef, SqlCallFn> =
                    mapOf(
                        RoutineRef("demo", Name.of("stable", "custom_scalar")) to
                            { args ->
                                exprCall(Identifier.regular("stable_scalar"), args.map { it.expr })
                            },
                    )
            }
    }

    private fun function(
        name: String,
        parameter: PType,
    ): FnOverload =
        FnOverload.Builder(name)
            .addParameter(parameter)
            .returns(parameter)
            .body { args -> args[0] }
            .build()
}
