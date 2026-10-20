package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCoalesce
import org.partiql.ast.Ast.exprNullIf
import org.partiql.ast.Ast.exprTrim
import org.partiql.ast.Identifier
import org.partiql.ast.SetQuantifier
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.TrimSpec
import org.partiql.scribe.frontend.redshift.RedshiftProblem
import org.partiql.spi.types.PType

/**
 * An argument of a function call: the parse tree of the argument and its translation. A rule that depends on the static
 * type of an argument reads it with [OperandTypes.resolve].
 */
internal class Operand(
    val ctx: ParserRuleContext,
    val expr: Expr,
)

/**
 * The rules that translate Redshift function calls to PartiQL.
 *
 * A function that is not in [rules] is unsupported. The translator does not pass unknown functions through by name,
 * because a function with the same name can have different semantics in the two languages.
 */
internal class RedshiftFunctions(
    private val notes: MutableList<RedshiftProblem>,
    internal val types: OperandTypes,
) {
    internal fun interface Rule {
        fun apply(
            name: Token,
            args: List<Operand>,
            setq: SetQuantifier?,
        ): Expr
    }

    internal val datetime = RedshiftDatetimeFunctions(this)

    private val rules: Map<String, Rule> =
        mapOf(
            // Functions with the same name and semantics in both languages.
            "abs" to same("abs", 1),
            "lower" to same("lower", 1),
            "upper" to same("upper", 1),
            "char_length" to Rule { name, args, setq -> length(name, args, setq) },
            "character_length" to Rule { name, args, setq -> length(name, args, setq) },
            "octet_length" to same("octet_length", 1),
            // Aggregations.
            "count" to aggregate("count"),
            "sum" to aggregate("sum"),
            "avg" to aggregate("avg"),
            "min" to aggregate("min"),
            "max" to aggregate("max"),
            // Special forms in the PartiQL AST.
            "coalesce" to Rule { name, args, _ -> coalesce(name, args.map { it.expr }) },
            "nullif" to Rule { name, args, _ -> nullif(name, args.map { it.expr }) },
            // https://docs.aws.amazon.com/redshift/latest/dg/r_NVL_function.html
            "nvl" to
                Rule { name, args, _ ->
                    note(name, "Redshift `NVL` was replaced by PartiQL `COALESCE`")
                    coalesce(name, args.map { it.expr })
                },
            // https://docs.aws.amazon.com/redshift/latest/dg/r_LEN.html
            "len" to
                Rule { name, args, setq ->
                    note(name, "Redshift `LEN` was replaced by PartiQL `CHAR_LENGTH`")
                    length(name, args, setq)
                },
            "length" to
                Rule { name, args, setq ->
                    note(name, "Redshift `LENGTH` was replaced by PartiQL `CHAR_LENGTH`")
                    length(name, args, setq)
                },
        ) + datetime.rules

    /**
     * `POSITION(sub IN str)`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_POSITION.html
     */
    fun position(
        name: Token,
        sub: Operand,
        str: Operand,
    ): Expr = throw UnsupportedConstructException.at(name, "POSITION")

    /**
     * `SUBSTRING(value FROM start [FOR length])` and `SUBSTRING(value FOR length)`; [start] is null for the second form.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_SUBSTRING.html
     */
    fun substring(
        name: Token,
        value: Operand,
        start: Operand?,
        length: Operand?,
    ): Expr = throw UnsupportedConstructException.at(name, "SUBSTRING")

    /**
     * `TRIM([BOTH | LEADING | TRAILING] [chars] FROM value)`; [spec] is the BOTH, LEADING, or TRAILING token, if any.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_TRIM.html
     */
    fun trim(
        name: Token,
        spec: Token?,
        chars: Operand?,
        value: Operand,
    ): Expr = throw UnsupportedConstructException.at(name, "TRIM")

    fun call(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        val rule = rules[name.text.lowercase()] ?: throw UnsupportedConstructException.at(name, "function `${name.text}`")
        return rule.apply(name, args, setq)
    }

    internal fun same(
        partiql: String,
        arity: Int,
    ): Rule =
        Rule { name, args, setq ->
            arity(name, args, arity)
            if (setq != null) throw UnsupportedConstructException.at(name, "set quantifier in scalar function `${name.text}`")
            exprCall(Identifier.regular(partiql), args.map { it.expr }, null)
        }

    internal fun renamed(
        redshift: String,
        partiql: String,
        arity: Int,
    ): Rule {
        val rule = same(partiql, arity)
        return Rule { name, args, setq ->
            note(name, "Redshift `${redshift.uppercase()}` was replaced by PartiQL `${partiql.uppercase()}`")
            rule.apply(name, args, setq)
        }
    }

    internal fun aggregate(partiql: String): Rule =
        Rule { name, args, setq ->
            arity(name, args, 1)
            exprCall(Identifier.regular(partiql), args.map { it.expr }, setq)
        }

    /**
     * `LEN`, `LENGTH`, `CHAR_LENGTH`, and `CHARACTER_LENGTH` count characters. Redshift does not count the trailing
     * blanks of a CHAR(n) value; PartiQL counts the padding, so the translation trims the blanks first.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_LEN.html
     */
    private fun length(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        arity(name, args, 1)
        if (setq != null) throw UnsupportedConstructException.at(name, "set quantifier in scalar function `${name.text}`")
        return types.resolve(args.map { it.ctx to it.expr }) { exprs, argTypes ->
            val arg =
                when (argTypes[0].code()) {
                    PType.CHAR -> exprTrim(exprs[0], null, TrimSpec.TRAILING())
                    else -> exprs[0]
                }
            exprCall(Identifier.regular("char_length"), listOf(arg), null)
        }
    }

    private fun coalesce(
        name: Token,
        args: List<Expr>,
    ): Expr {
        if (args.isEmpty()) throw UnsupportedConstructException.at(name, "`${name.text}` with no arguments")
        return exprCoalesce(args)
    }

    private fun nullif(
        name: Token,
        args: List<Expr>,
    ): Expr {
        arity(name, args, 2)
        return exprNullIf(args[0], args[1])
    }

    internal fun arity(
        name: Token,
        args: List<*>,
        expected: Int,
    ) {
        if (args.size != expected) {
            throw UnsupportedConstructException.at(name, "`${name.text}` with ${args.size} argument(s); expected $expected")
        }
    }

    internal fun note(
        token: Token,
        message: String,
    ) {
        notes.add(RedshiftProblem(RedshiftProblem.Code.TRANSLATION_INFO, message, token.line, token.charPositionInLine + 1))
    }
}
