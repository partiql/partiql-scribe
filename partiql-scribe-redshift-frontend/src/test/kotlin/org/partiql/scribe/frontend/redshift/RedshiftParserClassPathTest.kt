package org.partiql.scribe.frontend.redshift

import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.TokenStream
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser
import java.lang.reflect.InvocationTargetException
import java.net.URLClassLoader
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * The module ships no grammar; a deployment brings its own as the generated parser, [RedshiftGrammar]. These tests
 * cover the two ways to hand it over, and load the front end through a class loader that hides the parser, as a class
 * path without it would.
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
    fun theGrammarIsGivenByItsParser() {
        val grammar = RedshiftGrammar.of(RedshiftParser::class.java)
        assertSame(RedshiftParser::class.java, grammar.parser)
        val frontend = RedshiftFrontend(ScribeContext.standard(), grammar)
        assertSame(grammar, frontend.grammar)
        assertEquals("Query", frontend.toAst("SELECT a FROM SIMPLE_T")::class.java.simpleName)
    }

    /** An ANTLR parser that is not the one the front end is compiled against. */
    private abstract class OtherParser(input: TokenStream) : Parser(input)

    @Test
    fun anotherParserIsRefused() {
        val other: Class<out Parser> = OtherParser::class.java
        val thrown = assertThrows<IllegalStateException> { RedshiftGrammar.of(other) }
        assertTrue(thrown.message!!.startsWith("${OtherParser::class.java.name} is not the Redshift parser"), thrown.message)
        assertTrue(thrown.message!!.contains(RedshiftGrammar.PARSER_CLASS), thrown.message)
    }

    @Test
    fun theGrammarIsFoundOnTheClassPath() {
        // The test class path has the parser; the default finds it, and it is the same class the explicit form names,
        // defined by the loader that defined the front end, which is what a deployment's class path provides.
        val grammar = RedshiftGrammar.fromClassPath()
        assertSame<Class<out Parser>>(RedshiftParser::class.java, grammar.parser)
        assertEquals(RedshiftFrontend::class.java.classLoader, grammar.parser.classLoader)
        assertSame(grammar.parser, RedshiftFrontend(ScribeContext.standard()).grammar.parser)
    }

    @Test
    fun aMissingParserIsReportedAtConstruction() {
        val frontend = WithoutParser().loadClass(RedshiftFrontend::class.java.name)
        val constructor = frontend.getConstructor(ScribeContext::class.java)
        val thrown = assertThrows<InvocationTargetException> { constructor.newInstance(ScribeContext.standard()) }
        val cause = assertIs<IllegalStateException>(thrown.cause)
        assertTrue(
            cause.message!!.startsWith("No Redshift grammar: ${RedshiftGrammar.PARSER_CLASS} is not on the class path."),
            cause.message,
        )
        assertTrue(cause.message!!.contains("RedshiftGrammar.of(RedshiftParser::class.java)"), cause.message)
        assertIs<ClassNotFoundException>(cause.cause)
    }
}
