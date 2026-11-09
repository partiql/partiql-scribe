package org.partiql.scribe.frontend.redshift

import org.antlr.v4.runtime.Parser
import org.antlr.v4.runtime.RuntimeMetaData

/**
 * The Redshift grammar a deployment brings, as the parser generated from it. The front end does not ship a grammar: it
 * converts the parse tree of one named `Redshift`, generated with ANTLR [RedshiftFrontend.ANTLR_VERSION] as
 * `-visitor -no-listener -package org.partiql.scribe.frontend.redshift.antlr`, whose rule, label, and token names are
 * the ones the front end reads. The deployment generates that parser in its own build and hands it to
 * [RedshiftFrontend] here, by name with [of] or found on the class path with [fromClassPath]. Either way, a missing or
 * wrong parser is an [IllegalStateException] at construction, not a `NoClassDefFoundError` at the first query.
 *
 * ```
 * val frontend = RedshiftFrontend(context, RedshiftGrammar.of(RedshiftParser::class.java))
 * val frontend = RedshiftFrontend(context)   // RedshiftGrammar.fromClassPath()
 * ```
 */
public class RedshiftGrammar private constructor(
    /** The generated parser, `org.partiql.scribe.frontend.redshift.antlr.RedshiftParser`. */
    public val parser: Class<out Parser>,
) {
    override fun toString(): String = "RedshiftGrammar(${parser.name})"

    public companion object {
        /** The name of the parser class generated from a grammar named `Redshift` into [RedshiftFrontend.PARSER_PACKAGE]. */
        public const val PARSER_CLASS: String = "${RedshiftFrontend.PARSER_PACKAGE}.RedshiftParser"

        private const val CONTRACT =
            "The front end needs the parser generated from your Redshift grammar: a grammar named `Redshift`, generated " +
                "with ANTLR ${RedshiftFrontend.ANTLR_VERSION} as `-visitor -no-listener -package " +
                "${RedshiftFrontend.PARSER_PACKAGE}`, whose rule, label, and token names are the ones the front end " +
                "reads; see the module's README."

        /**
         * The grammar whose generated parser is [parser]: `RedshiftGrammar.of(RedshiftParser::class.java)`. Checks that
         * the class is the one the front end is compiled against, [PARSER_CLASS], and that the ANTLR runtime on the
         * class path is the version it was generated with.
         *
         * @throws IllegalStateException when [parser] is another class, or the runtime version differs.
         */
        @JvmStatic
        public fun of(parser: Class<out Parser>): RedshiftGrammar {
            check(parser.name == PARSER_CLASS) {
                "${parser.name} is not the Redshift parser: the front end is compiled against $PARSER_CLASS. $CONTRACT"
            }
            check(RuntimeMetaData.VERSION == RedshiftFrontend.ANTLR_VERSION) {
                "ANTLR runtime ${RuntimeMetaData.VERSION} is on the class path; the front end and the parser need " +
                    "${RedshiftFrontend.ANTLR_VERSION}. $CONTRACT"
            }
            return RedshiftGrammar(parser)
        }

        /**
         * The grammar whose generated parser is on the class path, found through the class loader that defined the
         * front end, which is the loader the front end's own references to the parser resolve against.
         *
         * @throws IllegalStateException when the class path has no parser.
         */
        @JvmStatic
        public fun fromClassPath(): RedshiftGrammar {
            val parser =
                try {
                    Class.forName(PARSER_CLASS, false, RedshiftFrontend::class.java.classLoader)
                } catch (e: ClassNotFoundException) {
                    throw IllegalStateException(
                        "No Redshift grammar: $PARSER_CLASS is not on the class path. $CONTRACT Pass " +
                            "`RedshiftGrammar.of(RedshiftParser::class.java)` to the front end, or put the generated " +
                            "classes on the class path next to it.",
                        e,
                    )
                }
            check(Parser::class.java.isAssignableFrom(parser)) { "$PARSER_CLASS is not an ANTLR parser. $CONTRACT" }
            @Suppress("UNCHECKED_CAST")
            return of(parser as Class<out Parser>)
        }
    }
}
