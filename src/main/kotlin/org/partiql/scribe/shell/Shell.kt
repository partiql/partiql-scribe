package org.partiql.scribe.shell

import org.fusesource.jansi.AnsiConsole
import org.jline.reader.EndOfFileException
import org.jline.reader.History
import org.jline.reader.LineReader
import org.jline.reader.LineReaderBuilder
import org.jline.reader.UserInterruptException
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedString
import org.jline.utils.AttributedStringBuilder
import org.jline.utils.AttributedStyle
import org.jline.utils.AttributedStyle.BOLD
import org.jline.utils.InfoCmp
import org.partiql.scribe.Scribe
import java.io.Closeable
import java.io.PrintStream
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean

private val PROMPT_1 =
    AttributedStringBuilder().styled(BOLD.foreground(AttributedStyle.CYAN), "$ ").toAnsi()
private const val PROMPT_2 = "  " // empty so you can sanely copy-paste
private const val WELCOME_MSG = """    

┌─┐┌─┐┬─┐┬┌┐ ┌┐ ┬  ┌─┐
└─┐│  ├┬┘│├┴┐├┴┐│  ├┤ 
└─┘└─┘┴└─┴└─┘└─┘┴─┘└─┘
"""

private const val HELP = """
.h                  Print this message
.s                  Print command history
.q                  Disconnect
.d                  Describe catalog
.dt <table>         Describe table
.debug on|off       Toggle debug printing
.t [<target>]       Get/Set the transpiler target
.clear              Clear screen
"""

private val SUCCESS: AttributedStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.GREEN)
private val ERROR: AttributedStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.RED)
private val INFO: AttributedStyle = AttributedStyle.DEFAULT
private val WARN: AttributedStyle = AttributedStyle.DEFAULT.foreground(AttributedStyle.YELLOW)

internal class Shell {

    private val state = State()

    class State {
        internal var path: List<String> = listOf()
        internal var debug: Boolean = false
    }

    private val output = System.out
    private val homeDir: Path = Paths.get(System.getProperty("user.home"))
    private val out = PrintStream(output)
    private val currentUser = System.getProperty("user.name")

    private val exiting = AtomicBoolean()

    fun start() {
        val interrupter = ThreadInterrupter()
        val exited = CountDownLatch(1)
        Runtime.getRuntime().addShutdownHook(Thread {
            exiting.set(true)
            interrupter.interrupt()
        })
        try {
            AnsiConsole.systemInstall()
            run(exiting)
        } catch (ex: java.lang.Exception) {
            ex.printStackTrace()
        } finally {
            exited.countDown()
            interrupter.close()
            AnsiConsole.systemUninstall()
        }
    }

    private fun run(exiting: AtomicBoolean) = TerminalBuilder.builder().build().use { terminal ->
        val highlighter = ShellHighlighter()
        val reader = LineReaderBuilder.builder().terminal(terminal).parser(ShellParser)
            .option(LineReader.Option.GROUP_PERSIST, true).option(LineReader.Option.AUTO_LIST, true)
            .option(LineReader.Option.CASE_INSENSITIVE, true).variable(LineReader.LIST_MAX, 10).highlighter(highlighter)
            .variable(LineReader.HISTORY_FILE, homeDir.resolve(".scribe/history"))
            .variable(LineReader.SECONDARY_PROMPT_PATTERN, PROMPT_2).build()
        out.info(WELCOME_MSG)

        // !! MAIN LOOP !!
        while (!exiting.get()) {
            val line: String = try {
                reader.readLine(PROMPT_1)
            } catch (ex: UserInterruptException) {
                if (ex.partialLine.isNotEmpty()) {
                    reader.history.add(ex.partialLine)
                }
                continue
            } catch (ex: EndOfFileException) {
                out.info("^D")
                return
            }

            if (line.isBlank()) {
                out.success("OK!")
                continue
            }

            if (line.startsWith(".")) {
                // Handle commands, consider an actual arg parsing library
                val args = line.trim().substring(1).split(" ")
                if (state.debug) {
                    out.info("argv: [${args.joinToString()}]")
                }
                val command = args[0]
                when (command) {
                    "h" -> {
                        // Print help
                        out.info(HELP)
                    }
                    "s" -> {
                        // Print history
                        for (entry in reader.history) {
                            out.println(entry.pretty())
                        }
                    }
                    "q" -> return
                    "d" -> {
                        // Describe Catalog
                        out.error("cannot describe a catalog")
                    }
                    "dt" -> {
                        // Describe Table
                        val arg1 = args.getOrNull(1)
                        if (arg1 == null) {
                            out.error("Expected <table> argument")
                            continue
                        }
                        out.error("cannot describe a table")
                    }
                    "debug" -> {
                        // Toggle debug printing
                        val arg1 = args.getOrNull(1)
                        if (arg1 == null) {
                            out.info("debug: ${state.debug}")
                            continue
                        }
                        when (arg1) {
                            "on" -> state.debug = true
                            "off" -> state.debug = false
                            else -> out.error("Expected on|off")
                        }
                    }
                    "t" -> {
                        // Get/Set Target
                        val arg1 = args.getOrNull(1)
                        if (arg1 == null) {
                            out.error("cannot get target")
                        } else {
                            out.error("cannot set target")
                        }
                    }
                    "clear" -> {
                        // Clear screen
                        terminal.puts(InfoCmp.Capability.clear_screen)
                        terminal.flush()
                    }
                    else -> out.error("Unrecognized command .$command")
                }
            } else {
                // Transpile input to desired target
                transpile(line)
            }
        }
    }

    private fun transpile(source: String) {
        try {
            val scribe = Scribe()
            val result = scribe.compile(source)
            out.println(result)
            out.success("ok.")
            out.println(result)
        } catch (ex: Throwable) {
            out.println()
            out.error(ex.stackTraceToString())
            out.println()
        }
    }
}

/**
 * Pretty print a History.Entry with a gutter for the entry index
 */
private fun History.Entry.pretty(): String {
    val entry = StringBuilder()
    for (line in this.line().lines()) {
        entry.append('\t').append(line).append('\n')
    }
    return AttributedStringBuilder().style(AttributedStyle.DEFAULT.foreground(AttributedStyle.CYAN))
        .append(java.lang.String.format("%5d", this.index() + 1)).style(AttributedStyle.DEFAULT).append(entry.trimEnd())
        .toAnsi()
}

private fun ansi(string: String, style: AttributedStyle) = AttributedString(string, style).toAnsi()

internal fun PrintStream.success(string: String): Unit = this.println(ansi(string, SUCCESS))

internal fun PrintStream.error(string: String): Unit = this.println(ansi(string, ERROR))

internal fun PrintStream.info(string: String): Unit = this.println(ansi(string, INFO))

internal fun PrintStream.warn(string: String): Unit = this.println(ansi(string, WARN))

private class ThreadInterrupter : Closeable {
    private val thread = Thread.currentThread()

    private var processing = true

    @Synchronized
    fun interrupt() {
        if (processing) {
            thread.interrupt()
        }
    }

    @Synchronized
    override fun close() {
        processing = false
        Thread.interrupted()
    }
}
