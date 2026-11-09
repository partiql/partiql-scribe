package org.partiql.scribe.frontend.redshift

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.scribe.ScribeContext
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * The module does not package the parser generated from the grammar; a deployment supplies it. These tests load the
 * front end through a class loader that hides the parser, as a class path without it would.
 */
class RedshiftParserClassPathTest {
    /** Defines the front end's own classes itself, refuses the parser's, and delegates everything else. */
    private class WithoutParser : URLClassLoader(
        arrayOf(RedshiftFrontend::class.java.protectionDomain.codeSource.location),
        RedshiftFrontend::class.java.classLoader,
    ) {
        override fun loadClass(
            name: String,
            resolve: Boolean,
        ): Class<*> {
            if (name.startsWith(RedshiftFrontend.PARSER_PACKAGE + ".")) throw ClassNotFoundException(name)
            if (!name.startsWith("org.partiql.scribe.frontend.redshift.")) return super.loadClass(name, resolve)
            synchronized(getClassLoadingLock(name)) {
                val loaded = findLoadedClass(name) ?: findClass(name)
                if (resolve) resolveClass(loaded)
                return loaded
            }
        }
    }

    @Test
    fun aMissingParserIsReportedAtConstruction() {
        val frontend = WithoutParser().loadClass(RedshiftFrontend::class.java.name)
        val constructor = frontend.getConstructor(ScribeContext::class.java, Boolean::class.java)
        val thrown = assertThrows<InvocationTargetException> { constructor.newInstance(ScribeContext.standard(), false) }
        val cause = assertIs<IllegalStateException>(thrown.cause)
        assertTrue(cause.message!!.startsWith("The Redshift parser is not on the class path."), cause.message)
        assertTrue(cause.message!!.contains(RedshiftFrontend.PARSER_PACKAGE), cause.message)
        assertIs<ClassNotFoundException>(cause.cause)
    }

    @Test
    fun theParserIsFoundThroughTheFrontEndsOwnClassLoader() {
        // The test class path has the parser (the `parser` source set); the front end works, and the parser's classes
        // come from the loader that defined the front end, which is what a deployment's class path provides.
        val ast = RedshiftFrontend(ScribeContext.standard()).toAst("SELECT a FROM SIMPLE_T")
        assertEquals("Query", ast::class.java.simpleName)
        val parser = Class.forName(RedshiftFrontend.PARSER_PACKAGE + ".RedshiftParser")
        assertEquals(RedshiftFrontend::class.java.classLoader, parser.classLoader)
    }
}
