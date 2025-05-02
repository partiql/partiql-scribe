package org.partiql.scribe

import java.util.Properties

internal object VersionProvider {

    @JvmField
    internal val version: String

    @JvmField
    internal val commit: String

    @JvmField
    internal val tag: String

    init {
        val properties = Properties()
        properties.load(this.javaClass.getResourceAsStream("/scribe.properties"))
        version = properties.getProperty("version")
        commit = properties.getProperty("commit")
        tag = "$version-$commit"
    }
}
