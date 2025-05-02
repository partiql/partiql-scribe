package org.partiql.scribe.plugin

import org.partiql.spi.connector.ConnectorObject
import org.partiql.types.StaticType

// wrap a StaticType
class TestObject(val type: StaticType) : ConnectorObject
