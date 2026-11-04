package org.partiql.scribe.extensions.functions

import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.problems.ScribeProblemListener
import org.partiql.scribe.sql.SqlArgs
import org.partiql.spi.types.PType

/**
 * The `to_char` template elements that every Scribe target formats identically. Each element knows its spelling in
 * the PostgreSQL-style template (Redshift), Java / Joda patterns (Spark `date_format`, Trino `format_datetime`), and
 * `strftime` (DuckDB).
 */
internal enum class ToCharElement(
    val postgres: String,
    val java: String,
    val strftime: String,
) {
    YEAR("YYYY", "yyyy", "%Y"),
    YEAR_2("YY", "yy", "%y"),
    MONTH("MM", "MM", "%m"),
    DAY("DD", "dd", "%d"),
    DAY_OF_YEAR("DDD", "DDD", "%j"),
    HOUR_24("HH24", "HH", "%H"),
    HOUR_12("HH12", "hh", "%I"),
    MINUTE("MI", "mm", "%M"),
    SECOND("SS", "ss", "%S"),
    MILLISECOND("MS", "SSS", "%g"),
    MERIDIEM("AM", "a", "%p"),
}

internal sealed interface ToCharPart {
    data class Element(
        val element: ToCharElement,
    ) : ToCharPart

    data class Text(
        val text: String,
    ) : ToCharPart
}

/**
 * Parses the subset of PostgreSQL / Oracle `to_char` templates that Scribe translates to every target.
 *
 * Template patterns are matched case-insensitively, longest first, exactly like the `to_char` function extension, so
 * a template is either translated with the same meaning or rejected; it is never reinterpreted. Supported:
 *
 * - `YYYY YY MM DD DDD HH24 HH12 HH MI SS MS FF3`, and `AM` / `PM` in upper case;
 * - unquoted digits, spaces, and `- / : . , ; _` between them.
 *
 * Everything else is rejected, including names (`Month`, `Dy`, ...), fill mode (`FM`), ordinal suffixes (`TH`),
 * other patterns (`Q`, `IW`, `US`, `FF6`, ...), double-quoted text, and other literal characters. These either have no
 * equivalent in some target or are formatted differently (padding, locale, or precision).
 */
internal object ToCharTemplate {
    // All template patterns of the `to_char` function extension, longest first, so that, for example, `SSSS` is never
    // read as `SS` `SS`, and `MON` is never read as `MM`.
    private val PATTERNS =
        listOf(
            "A.D.", "A.M.", "AD", "AM", "B.C.", "BC", "CC", "DAY", "DDD", "DD", "DY", "D",
            "FF1", "FF2", "FF3", "FF4", "FF5", "FF6", "FF7", "FF8", "FF9", "FF",
            "HH24", "HH12", "HH", "IDDD", "ID", "IW", "IYYY", "IYY", "IY", "I", "J",
            "MI", "MM", "MONTH", "MON", "MS", "OF", "P.M.", "PM", "Q", "RM", "RRRR", "RR",
            "SSSSS", "SSSS", "SS", "TZH", "TZM", "TZ", "US", "WW", "W",
            "Y,YYY", "YYYY", "YYY", "YY", "Y", "FM", "FX", "TM",
        ).sortedByDescending { it.length }

    private val ELEMENTS =
        mapOf(
            "YYYY" to ToCharElement.YEAR,
            "YY" to ToCharElement.YEAR_2,
            "MM" to ToCharElement.MONTH,
            "DD" to ToCharElement.DAY,
            "DDD" to ToCharElement.DAY_OF_YEAR,
            "HH24" to ToCharElement.HOUR_24,
            "HH12" to ToCharElement.HOUR_12,
            "HH" to ToCharElement.HOUR_12,
            "MI" to ToCharElement.MINUTE,
            "SS" to ToCharElement.SECOND,
            "MS" to ToCharElement.MILLISECOND,
            "FF3" to ToCharElement.MILLISECOND,
        )

    private const val TEXT = " -/:.,;_0123456789"

    /**
     * Returns the parsed [template], or the reason it is not supported.
     */
    fun parse(template: String): Result {
        val parts = mutableListOf<ToCharPart>()
        val text = StringBuilder()
        var i = 0
        while (i < template.length) {
            val c = template[i]
            if (c in TEXT) {
                text.append(c)
                i++
                continue
            }
            val pattern =
                PATTERNS.firstOrNull { template.regionMatches(i, it, 0, it.length, ignoreCase = true) }
                    ?: return Result.Unsupported("character `$c`")
            val spelling = template.substring(i, i + pattern.length)
            val element =
                when {
                    pattern == "AM" || pattern == "PM" -> if (spelling == pattern) ToCharElement.MERIDIEM else null
                    else -> ELEMENTS[pattern]
                } ?: return Result.Unsupported("pattern `$spelling`")
            i += pattern.length
            if (element != ToCharElement.MERIDIEM && template.regionMatches(i, "TH", 0, 2, ignoreCase = true)) {
                return Result.Unsupported("suffix `${template.substring(i, i + 2)}`")
            }
            if (text.isNotEmpty()) {
                parts.add(ToCharPart.Text(text.toString()))
                text.setLength(0)
            }
            parts.add(ToCharPart.Element(element))
        }
        if (text.isNotEmpty()) {
            parts.add(ToCharPart.Text(text.toString()))
        }
        return Result.Supported(parts)
    }

    sealed interface Result {
        data class Supported(
            val parts: List<ToCharPart>,
        ) : Result

        data class Unsupported(
            val reason: String,
        ) : Result
    }
}

/**
 * Renders parsed `to_char` template parts in a target's pattern language. Literal text is limited to digits, spaces,
 * and separators, which every pattern language copies as-is.
 */
internal fun List<ToCharPart>.render(element: (ToCharElement) -> String): String =
    joinToString("") {
        when (it) {
            is ToCharPart.Element -> element(it.element)
            is ToCharPart.Text -> it.text
        }
    }

/**
 * Translates `to_char(<timestamp | date>, '<template>')` to `<function>(<value>, '<pattern>')`, where [pattern]
 * renders the parsed template for the target. A date argument is cast to TIMESTAMP so every target formats it as
 * midnight, like the function extension.
 */
internal fun toChar(
    listener: ScribeProblemListener,
    args: SqlArgs,
    function: String,
    pattern: (List<ToCharPart>) -> String,
): Expr {
    val template = (args.getOrNull(1)?.expr as? ExprLit)?.lit
    if (args.size != 2 || template?.code() != Literal.STRING) {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                ScribeProblem.UNSUPPORTED_OPERATION,
                "Function extension `to_char` requires a timestamp or date and a string literal template",
            ),
        )
    }
    val parts =
        when (val result = ToCharTemplate.parse(template.stringValue())) {
            is ToCharTemplate.Result.Supported -> result.parts
            is ToCharTemplate.Result.Unsupported ->
                listener.reportAndThrow(
                    ScribeProblem.simpleError(
                        ScribeProblem.UNSUPPORTED_OPERATION,
                        "Unsupported ${result.reason} in `to_char` template '${template.stringValue()}'. " +
                            "Supported: YYYY YY MM DD DDD HH24 HH12 HH MI SS MS FF3 AM PM, " +
                            "with digits, spaces, and - / : . , ; _ between them",
                    ),
                )
        }
    val source = args[0]
    val value = if (source.type.code() == PType.DATE) exprCast(source.expr, DataType.TIMESTAMP()) else source.expr
    return exprCall(Identifier.regular(function), listOf(value, exprLit(Literal.string(pattern(parts)))))
}

internal val TO_CHAR_POSTGRES: (List<ToCharPart>) -> String = { parts -> parts.render { it.postgres } }
internal val TO_CHAR_JAVA: (List<ToCharPart>) -> String = { parts -> parts.render { it.java } }
internal val TO_CHAR_STRFTIME: (List<ToCharPart>) -> String = { parts -> parts.render { it.strftime } }
