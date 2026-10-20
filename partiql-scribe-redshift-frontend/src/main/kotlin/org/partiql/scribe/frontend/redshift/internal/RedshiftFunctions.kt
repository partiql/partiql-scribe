package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCoalesce
import org.partiql.ast.Ast.exprNullIf
import org.partiql.ast.Identifier
import org.partiql.ast.SetQuantifier
import org.partiql.ast.expr.Expr
import org.partiql.scribe.frontend.redshift.RedshiftProblem

/**
 * The rules that translate Redshift function calls to PartiQL.
 *
 * A function that is not in [rules] is unsupported. The translator does not pass unknown functions through by name,
 * because a function with the same name can have different semantics in the two languages.
 */
internal class RedshiftFunctions(
    private val notes: MutableList<RedshiftProblem>,
) {
    private fun interface Rule {
        fun apply(
            name: Token,
            args: List<Expr>,
            setq: SetQuantifier?,
        ): Expr
    }

    private val rules: Map<String, Rule> =
        mapOf(
            // Functions with the same name and semantics in both languages.
            "abs" to same("abs", 1),
            "lower" to same("lower", 1),
            "upper" to same("upper", 1),
            "char_length" to same("char_length", 1),
            "character_length" to same("char_length", 1),
            "octet_length" to same("octet_length", 1),
            // Aggregations.
            "count" to aggregate("count"),
            "sum" to aggregate("sum"),
            "avg" to aggregate("avg"),
            "min" to aggregate("min"),
            "max" to aggregate("max"),
            // Special forms in the PartiQL AST.
            "coalesce" to Rule { name, args, _ -> coalesce(name, args) },
            "nullif" to Rule { name, args, _ -> nullif(name, args) },
            // https://docs.aws.amazon.com/redshift/latest/dg/r_NVL_function.html
            "nvl" to
                Rule { name, args, _ ->
                    note(name, "Redshift `NVL` was replaced by PartiQL `COALESCE`")
                    coalesce(name, args)
                },
            // https://docs.aws.amazon.com/redshift/latest/dg/r_LEN.html
            "len" to renamed("len", "char_length", 1),
            "length" to renamed("length", "char_length", 1),
            // https://docs.aws.amazon.com/redshift/latest/dg/r_GETDATE.html
            "getdate" to
                Rule { name, args, _ ->
                    arity(name, args, 0)
                    utcnow(name, "GETDATE()")
                },
        )

    fun call(
        name: Token,
        args: List<Expr>,
        setq: SetQuantifier?,
    ): Expr {
        val rule = rules[name.text.lowercase()] ?: throw UnsupportedConstructException.at(name, "function `${name.text}`")
        return rule.apply(name, args, setq)
    }

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/r_SYSDATE.html
     */
    fun sysdate(name: Token): Expr = utcnow(name, "SYSDATE")

    /**
     * Redshift returns TIMESTAMP (without time zone) in the session time zone; PartiQL `UTCNOW()` returns TIMESTAMP
     * WITH TIME ZONE in UTC. The values are equal when the session time zone is UTC, which is the Redshift default.
     */
    private fun utcnow(
        name: Token,
        source: String,
    ): Expr {
        note(
            name,
            "Redshift `$source` was replaced by PartiQL `UTCNOW()`; the result type is TIMESTAMP WITH TIME ZONE in UTC",
        )
        return exprCall(Identifier.regular("utcnow"), emptyList(), null)
    }

    private fun same(
        partiql: String,
        arity: Int,
    ): Rule =
        Rule { name, args, setq ->
            arity(name, args, arity)
            if (setq != null) throw UnsupportedConstructException.at(name, "set quantifier in scalar function `${name.text}`")
            exprCall(Identifier.regular(partiql), args, null)
        }

    private fun renamed(
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

    private fun aggregate(partiql: String): Rule =
        Rule { name, args, setq ->
            arity(name, args, 1)
            exprCall(Identifier.regular(partiql), args, setq)
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

    private fun arity(
        name: Token,
        args: List<Expr>,
        expected: Int,
    ) {
        if (args.size != expected) {
            throw UnsupportedConstructException.at(name, "`${name.text}` with ${args.size} argument(s); expected $expected")
        }
    }

    private fun note(
        token: Token,
        message: String,
    ) {
        notes.add(RedshiftProblem(RedshiftProblem.Code.TRANSLATION_INFO, message, token.line, token.charPositionInLine + 1))
    }
}
