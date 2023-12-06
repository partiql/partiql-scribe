package org.partiql.scribe.plugin

import com.amazon.ionelement.api.StructElement
import org.partiql.spi.BindingPath
import org.partiql.spi.connector.Connector
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.spi.connector.ConnectorObjectHandle
import org.partiql.spi.connector.ConnectorObjectPath
import org.partiql.spi.connector.ConnectorSession
import org.partiql.types.StaticType

class TestConnector(private val catalog: TestCatalog) : Connector {

    override fun getMetadata(session: ConnectorSession): ConnectorMetadata = Metadata()

    class Factory(private val provider: TestCatalog.Provider) : Connector.Factory {

        override fun getName(): String = "test"

        override fun create(catalogName: String, config: StructElement): Connector {
            val catalog = provider[catalogName]
            return TestConnector(catalog)
        }
    }

    private inner class Metadata : ConnectorMetadata {

        override fun getObjectHandle(session: ConnectorSession, path: BindingPath): ConnectorObjectHandle? {
            val kPath = ConnectorObjectPath(path.steps.map { it.loweredName })
            val k = kPath.steps.joinToString(".")
            return when (val v = catalog[k]) {
                null -> null
                else -> ConnectorObjectHandle(kPath, TestObject(v))
            }
        }

        override fun getObjectType(session: ConnectorSession, handle: ConnectorObjectHandle): StaticType {
            val obj = handle.value as TestObject
            return obj.type
        }
    }
}
