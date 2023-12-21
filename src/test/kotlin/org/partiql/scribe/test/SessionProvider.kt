package org.partiql.scribe.test

import com.amazon.ionelement.api.StructElement
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.connector.ConnectorMetadata
import java.time.Instant

class SessionProvider(
    private val catalogs: Map<String, ConnectorMetadata>,
) {
    fun get(key: ScribeTest.Key, currentCatalog: String = "default"): PartiQLPlanner.Session {
        return PartiQLPlanner.Session(
            queryId = key.toString(),
            userId = "user_id",
            currentCatalog = currentCatalog,
            currentDirectory = listOf(),
            catalogs = catalogs,
            instant = Instant.now(),
        )
    }
}
