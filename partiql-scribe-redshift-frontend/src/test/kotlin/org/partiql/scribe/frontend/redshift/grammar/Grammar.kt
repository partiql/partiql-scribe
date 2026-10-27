package org.partiql.scribe.frontend.redshift.grammar

import org.antlr.v4.runtime.BaseErrorListener
import org.antlr.v4.runtime.CharStreams
import org.antlr.v4.runtime.CommonTokenStream
import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.RecognitionException
import org.antlr.v4.runtime.Recognizer
import org.antlr.v4.runtime.tree.ParseTree
import org.antlr.v4.runtime.tree.TerminalNode
import org.partiql.scribe.frontend.redshift.antlr.RedshiftLexer
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import kotlin.streams.toList

/**
 * A syntax error reported by the lexer or the parser. [line] and [column] are 1-based.
 */
data class SyntaxError(val line: Int, val column: Int, val message: String) {
    override fun toString(): String = "$line:$column $message"
}

/**
 * The result of parsing a statement with the Redshift grammar: the parse tree of the `root` rule and the syntax errors.
 * A statement is syntactically valid when [errors] is empty.
 */
class ParseResult(val tree: RedshiftParser.RootContext, val errors: List<SyntaxError>) {
    val isValid: Boolean get() = errors.isEmpty()
}

/**
 * Parses [sql] with the Redshift grammar, collecting every syntax error instead of recovering silently or throwing.
 */
fun parse(sql: String): ParseResult {
    val errors = mutableListOf<SyntaxError>()
    val listener =
        object : BaseErrorListener() {
            override fun syntaxError(
                recognizer: Recognizer<*, *>?,
                offendingSymbol: Any?,
                line: Int,
                charPositionInLine: Int,
                msg: String,
                e: RecognitionException?,
            ) {
                errors.add(SyntaxError(line, charPositionInLine + 1, msg))
            }
        }
    val lexer = RedshiftLexer(CharStreams.fromString(sql))
    lexer.removeErrorListeners()
    lexer.addErrorListener(listener)
    val parser = RedshiftParser(CommonTokenStream(lexer))
    parser.removeErrorListeners()
    parser.addErrorListener(listener)
    val tree = parser.root()
    return ParseResult(tree, errors)
}

/**
 * Renders a parse tree compactly: a rule node with a single child is replaced by that child, so the rendering shows
 * the structure (which operands an operator took, which clause a keyword belongs to) and not the rule chain. A rule
 * node with several children is rendered as `(label child child ...)`, where the label is the name of the labeled
 * alternative (`valueAdditive`) or of the rule when the alternative is not labeled. Tokens are rendered as their text.
 */
fun ParseTree.render(): String =
    when (this) {
        is TerminalNode -> text
        is ParserRuleContext -> {
            val children = (0 until childCount).map { getChild(it) }
            if (children.size == 1) {
                children[0].render()
            } else {
                val label = javaClass.simpleName.removeSuffix("Context").replaceFirstChar { it.lowercaseChar() }
                children.joinToString(" ", "($label ", ")") { it.render() }
            }
        }
        else -> error("Unexpected node ${this::class}")
    }

/**
 * A statement of a fixture file: the file groups statements under a `--#[name]` marker; a reject fixture also carries
 * the expected position of the first syntax error, `--#[name] line:column`.
 */
data class Fixture(val file: String, val name: String, val statement: String, val position: Pair<Int, Int>?) {
    val key: String get() = "$file/$name"
}

object Fixtures {
    private val MARKER = Regex("""^--#\[([^]]+)](?:\s+(\d+):(\d+))?\s*$""")

    /**
     * The fixture files of a directory under `src/test/resources/grammar`, in file order, each with its statements.
     */
    fun load(directory: String): Map<String, List<Fixture>> {
        val root = Paths.get("src/test/resources/grammar", directory)
        val files = Files.list(root).use { it.filter { p -> p.toString().endsWith(".sql") }.sorted().toList() }
        return files.associate { path -> path.fileName.toString().removeSuffix(".sql") to read(path) }
    }

    private fun read(path: Path): List<Fixture> {
        val file = path.fileName.toString().removeSuffix(".sql")
        val fixtures = mutableListOf<Fixture>()
        var name: String? = null
        var position: Pair<Int, Int>? = null
        val body = StringBuilder()

        fun flush() {
            val n = name ?: return
            // An empty statement is a valid reject fixture (empty input), so it is kept.
            fixtures.add(Fixture(file, n, body.toString().trim(), position))
            body.setLength(0)
        }
        Files.readAllLines(path).forEach { line ->
            val match = MARKER.matchEntire(line)
            if (match != null) {
                flush()
                name = match.groupValues[1]
                position = match.groupValues[2].takeIf { it.isNotEmpty() }?.let { it.toInt() to match.groupValues[3].toInt() }
            } else if (name != null) {
                body.append(line).append('\n')
            }
        }
        flush()
        check(fixtures.map { it.name }.toSet().size == fixtures.size) { "Duplicate fixture names in $file" }
        return fixtures
    }
}
