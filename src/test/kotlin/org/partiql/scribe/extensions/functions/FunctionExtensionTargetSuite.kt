package org.partiql.scribe.extensions.functions

import org.partiql.extensions.functions.compat.datetime.DateAdd
import org.partiql.extensions.functions.compat.datetime.UtcNow
import org.partiql.extensions.functions.custom.collection.Contains
import org.partiql.extensions.functions.custom.conversion.HexToBigInt
import org.partiql.extensions.functions.custom.datetime.ToUnixTime
import org.partiql.extensions.functions.custom.math.Pow
import org.partiql.plan.RoutineRef
import org.partiql.scribe.targets.SqlTargetSuite
import org.partiql.scribe.utils.SessionProvider
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
import org.partiql.spi.types.PTypeField
import kotlin.io.path.toPath
import org.partiql.spi.catalog.Identifier as SpiIdentifier

class SparkFunctionExtensionTargetSuite : SqlTargetSuite() {
    override val target = SparkFunctionExtensionTarget(ROUTINE_REFS)
    override val root = this::class.java.getResource("/outputs/function-extensions/spark")!!.toURI().toPath()
    override val sessions = SessionProvider(fixedSession = SESSION)
}

class TrinoFunctionExtensionTargetSuite : SqlTargetSuite() {
    override val target = TrinoFunctionExtensionTarget(ROUTINE_REFS)
    override val root = this::class.java.getResource("/outputs/function-extensions/trino")!!.toURI().toPath()
    override val sessions = SessionProvider(fixedSession = SESSION)
}

class RedshiftFunctionExtensionTargetSuite : SqlTargetSuite() {
    override val target = RedshiftFunctionExtensionTarget(ROUTINE_REFS)
    override val root = this::class.java.getResource("/outputs/function-extensions/redshift")!!.toURI().toPath()
    override val sessions = SessionProvider(fixedSession = SESSION)
}

private val NAMESPACE = Namespace.of("extensions")
private val FUNCTIONS =
    listOf(
        DateAdd.overloads(),
        UtcNow.overloads(),
        Contains.overloads(),
        HexToBigInt.overloads(),
        ToUnixTime.overloads(),
        Pow.overloads(),
    ).flatten()
private val ROUTINE_REFS =
    FUNCTIONS
        .map { RoutineRef("test", Name.of("extensions", it.signature.name)) }
        .toSet()
private val ROUTINES =
    MemRoutineProvider.builder().apply {
        FUNCTIONS.forEach { register(it, NAMESPACE) }
    }.build()
private val PAYLOAD =
    Table.empty(
        "payload",
        PType.bag(PType.row(PTypeField.of("items", PType.array(PType.string())))),
    )
private val CATALOG =
    object : RoutineCatalog {
        override fun getName(): String = "test"

        override fun getTable(
            session: Session,
            name: Name,
        ): Table? = PAYLOAD.takeIf { it.getName() == name }

        override fun resolveTable(
            session: Session,
            identifier: SpiIdentifier,
        ): Name? = PAYLOAD.getName().takeIf { identifier.matches(it.getName(), ignoreCase = true) }

        override fun resolveFunctions(
            session: Session,
            identifier: SpiIdentifier,
        ): Collection<RoutineBinding<FnOverload>> = ROUTINES.getFunctions(identifier)

        override fun resolveAggregations(
            session: Session,
            identifier: SpiIdentifier,
        ): Collection<RoutineBinding<AggOverload>> = emptyList()
    }
private val SESSION =
    Session.builder()
        .catalog("test")
        .catalogs(CATALOG)
        .path(Namespace.of("test", "extensions"))
        .build()
