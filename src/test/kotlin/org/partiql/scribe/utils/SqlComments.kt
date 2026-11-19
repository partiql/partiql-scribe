package org.partiql.scribe.utils

import org.partiql.parser.internal.antlr.PartiQLTokens
import org.partiql.parser.thirdparty.antlr.v4.runtime.BaseErrorListener
import org.partiql.parser.thirdparty.antlr.v4.runtime.CharStreams
import org.partiql.parser.thirdparty.antlr.v4.runtime.Lexer
import org.partiql.parser.thirdparty.antlr.v4.runtime.RecognitionException
import org.partiql.parser.thirdparty.antlr.v4.runtime.Recognizer
import kotlin.test.assertTrue

/**
 * Inspect tokens rather than substrings: comment-like text inside a quoted value is ordinary data.
 * Include an unterminated block opener, which the lexer's complete-comment rule cannot consume.
 */
internal fun assertNoSqlComments(sql: String) {
    val lexer = PartiQLTokens(CharStreams.fromString(sql))
    lexer.removeErrorListeners()
    lexer.addErrorListener(
        object : BaseErrorListener() {
            override fun syntaxError(
                recognizer: Recognizer<*, *>?,
                offendingSymbol: Any?,
                line: Int,
                charPositionInLine: Int,
                msg: String?,
                e: RecognitionException?,
            ) {
                throw AssertionError("Cannot check generated SQL tokens at $line:$charPositionInLine: $msg")
            }
        },
    )
    val tokens = lexer.allTokens
    assertTrue(
        lexer._mode == Lexer.DEFAULT_MODE && tokens.none { it.type == PartiQLTokens.UNRECOGNIZED },
        "Generated SQL contains an unrecognized or unterminated token: $sql",
    )
    val comments =
        tokens.filter {
            it.type == PartiQLTokens.COMMENT_SINGLE || it.type == PartiQLTokens.COMMENT_BLOCK
        }
    val openBlock =
        tokens.zipWithNext().any { (left, right) ->
            left.text == "/" && right.text == "*" && left.stopIndex + 1 == right.startIndex
        }
    assertTrue(comments.isEmpty() && !openBlock, "Generated SQL contains a comment token: $sql")
}
