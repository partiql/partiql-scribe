package org.partiql.scribe.plugin

import org.partiql.types.StaticType

/**
 * A simple map of case-sensitive paths (lower-case normalized) to [StaticType].
 */
class TestCatalog private constructor(private val map: Map<String, StaticType>) {

    operator fun get(key: String): StaticType? = map[key]

    companion object {

        fun of(vararg entities: Pair<String, StaticType>) = TestCatalog(mapOf(*entities))
    }

    class Provider {

        private val catalogs = mutableMapOf<String, TestCatalog>()

        operator fun get(path: String): TestCatalog = catalogs[path]!!

        operator fun set(path: String, catalog: TestCatalog) {
            catalogs[path] = catalog
        }
    }
}
