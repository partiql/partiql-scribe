package org.partiql.scribe.plugin

import org.partiql.spi.Plugin
import org.partiql.spi.connector.Connector
import org.partiql.spi.function.PartiQLFunction
import org.partiql.spi.function.PartiQLFunctionExperimental

/**
 * Plugin backed by a catalog provider. This enables use to easily modify a catalog for tests.
 * 
 * val provider = TestCatalog.Provider()
 * val plugin = TestPlugin(provider)
 * 
 * // Add some catalogs..
 * 
 * provider["cat_success"] = TestCatalog.of(
 *   "lhs" to StaticType.INT,
 *   "rhs" to StaticType.INT,
 * )
 * 
 * provider["cat_failure"] = TestCatalog.of(
 *   "lhs" to StaticType.INT,
 *   "rhs" to StaticType.BOOL,
 * )
 *
 * val statement = "lhs + rhs"
 * 
 * assertSuccess { planner.plan(statement, sess.copyWith(catalog: "cat_success")) }
 * assertFailure { planner.plan(statement, sess.copyWith(catalog: "cat_failure")) }
 */
class TestPlugin(private val provider: TestCatalog.Provider) : Plugin {
    override val factory: Connector.Factory = TestConnector.Factory(provider)

    @PartiQLFunctionExperimental
    override val functions: List<PartiQLFunction> = emptyList()
}
