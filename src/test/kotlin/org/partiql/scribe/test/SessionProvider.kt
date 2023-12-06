package org.partiql.scribe.test

import com.amazon.ionelement.api.StructElement
import org.partiql.planner.PartiQLPlanner
import java.time.Instant

class SessionProvider(
    private val catalogConfig: Map<String, StructElement>,
) {

    fun get(key: ScribeTest.Key, currentCatalog: String = "default"): PartiQLPlanner.Session {
        return PartiQLPlanner.Session(
            queryId = key.toString(),
            userId = "user_id",
            currentCatalog = currentCatalog,
            currentDirectory = listOf(),
            catalogConfig = catalogConfig,
            instant = Instant.now(),
        )
    }
}
