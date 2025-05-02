package org.partiql.scribe.utils

import com.amazon.ionelement.api.loadSingleElement
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Name
import org.partiql.spi.catalog.Session
import org.partiql.spi.function.FnOverload
import org.partiql.spi.types.PType
import java.nio.file.Files
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.nameWithoutExtension
import kotlin.io.path.relativeTo
import kotlin.io.path.toPath


class SessionProvider(
    val scalarOverloads: Map<String, FnOverload> = emptyMap()
) {
    private val catalogs: List<Catalog> by lazy {
        // Make a map from catalog name to tables.
        val resourcesDir = this::class.java.classLoader.getResource("catalogs")!!
        val catalogPath = resourcesDir.toURI().toPath()
        val inputStream = Files.walk(catalogPath)
        val map = mutableMapOf<String, MutableList<Pair<Name, PType>>>()
        inputStream.forEach { path ->
            if (path.extension == "ion" && !path.isDirectory()) {
                val ion = loadSingleElement(path.toFile().reader().readText())
                val pType = ion.toPType()
                val relativePath = path.relativeTo(catalogPath)
                val steps = relativePath.map { it.toString() }.dropLast(1) + relativePath.nameWithoutExtension
                val catalogName = steps.first()
                // args
                val name = Name.of(steps.drop(1))
                if (map.containsKey(catalogName)) {
                    map[catalogName]!!.add(name to pType)
                } else {
                    map[catalogName] = mutableListOf(name to pType)
                }
            }
        }
        // Make a catalogs list
        val catalogList = map.map { (catalog, tables) ->
            TestCatalog.builder()
                .name(catalog)
                .createTables(tables)
                .scalarOverloads(scalarOverloads)
                .build()
        }
        catalogList
    }

    fun getSession(): Session {
        val session = Session.builder()
            .catalog("default")
            .catalogs(*catalogs.toTypedArray())
            .namespace(emptyList())
            .build()
        return session
    }
}
