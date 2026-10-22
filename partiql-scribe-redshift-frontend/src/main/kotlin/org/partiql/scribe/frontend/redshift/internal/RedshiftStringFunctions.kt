package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.Token
import org.partiql.ast.Ast.exprAnd
import org.partiql.ast.Ast.exprCase
import org.partiql.ast.Ast.exprCaseBranch
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprNullPredicate
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.exprPath
import org.partiql.ast.Ast.exprPathStepElement
import org.partiql.ast.Ast.exprPosition
import org.partiql.ast.Ast.exprSubstring
import org.partiql.ast.Ast.exprTrim
import org.partiql.ast.Literal
import org.partiql.ast.SetQuantifier
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.TrimSpec
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isDecimal
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isDynamic
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isInteger
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isText
import org.partiql.scribe.frontend.redshift.internal.FunctionTypes.isUnknown
import org.partiql.scribe.frontend.redshift.internal.RedshiftFunctions.Rule
import org.partiql.spi.types.PType
import org.partiql.scribe.frontend.redshift.antlr.RedshiftParser as P

/**
 * The rules that translate Redshift string functions; see [RedshiftFunctions].
 *
 * Two kinds of arguments need a conversion (see [text]):
 *  - Redshift converts a numeric or DATE argument of a string function to a character string implicitly; PartiQL has
 *    no implicit conversions, so the translation adds the CAST.
 *  - Redshift disregards the trailing blanks of a CHAR(n) value, and removes them when it converts the value to another
 *    string type. PartiQL keeps the padding of a CHAR(n) value, so the translation trims it where Redshift ignores it.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/String_functions_header.html
 */
internal class RedshiftStringFunctions(
    private val functions: RedshiftFunctions,
) {
    /** How a function treats the trailing blanks of a CHAR(n) argument. */
    internal enum class Padding {
        /** The function disregards them; the translation trims them. */
        TRIM,

        /** The function keeps them, as PartiQL does. */
        KEEP,

        /** The function counts them, as PartiQL does. */
        COUNT,

        /** The Redshift behavior is not documented; the argument is unsupported. */
        REJECT,
    }

    val rules: Map<String, Rule> =
        mapOf(
            "len" to Rule { name, args, setq -> length(name, args, setq, rename = true) },
            "length" to Rule { name, args, setq -> length(name, args, setq, rename = true) },
            "textlen" to Rule { name, args, setq -> length(name, args, setq, rename = true) },
            "char_length" to Rule { name, args, setq -> length(name, args, setq, rename = false) },
            "character_length" to Rule { name, args, setq -> length(name, args, setq, rename = false) },
            "octet_length" to Rule(::octetLength),
            "lower" to Rule { name, args, setq -> case("lower", name, args, setq) },
            "upper" to Rule { name, args, setq -> case("upper", name, args, setq) },
            "substring" to Rule(::substringCall),
            "btrim" to Rule { name, args, setq -> trimFunction(TrimSpec.BOTH(), "BOTH", name, args, setq) },
            "ltrim" to Rule { name, args, setq -> trimFunction(TrimSpec.LEADING(), "LEADING", name, args, setq) },
            "rtrim" to Rule { name, args, setq -> trimFunction(TrimSpec.TRAILING(), "TRAILING", name, args, setq) },
            "strpos" to Rule(::strpos),
            "charindex" to Rule(::charindex),
            "replace" to Rule(::replace),
            "split_part" to Rule(::splitPart),
            "left" to Rule { name, args, setq -> leftRight(left = true, name, args, setq) },
            "right" to Rule { name, args, setq -> leftRight(left = false, name, args, setq) },
            "concat" to Rule(::concat),
            "lpad" to Rule { name, args, setq -> pad(left = true, name, args, setq) },
            "rpad" to Rule { name, args, setq -> pad(left = false, name, args, setq) },
            "repeat" to Rule(::repeat),
            "replicate" to Rule(::repeat),
            "translate" to Rule(::translate),
            "octetindex" to Rule(::octetIndex),
            "chr" to Rule(::chr),
            "quote_literal" to Rule(::quoteLiteral),
            "regexp_count" to Rule(::regexpCount),
            "regexp_instr" to Rule(::regexpInstr),
            "regexp_replace" to Rule(::regexpReplace),
            "regexp_substr" to Rule(::regexpSubstr),
            // https://docs.aws.amazon.com/redshift/latest/dg/c_SQL_functions_leader_node_only.html
            "substr" to functions.unsupported("a leader node-only function, which fails in a query that references a table"),
            "initcap" to functions.unsupported("PartiQL has no word capitalization"),
            "reverse" to functions.unsupported("PartiQL has no string reversal"),
            "ascii" to functions.unsupported(NO_CODE_POINTS),
            "strtol" to functions.unsupported("PartiQL has no base conversion"),
            "collate" to functions.unsupported("PartiQL has no collations"),
            "bpcharcmp" to functions.unsupported(BYTE_ORDER),
            "bttext_pattern_cmp" to functions.unsupported(BYTE_ORDER),
            "quote_ident" to functions.unsupported("PartiQL has no character class tests"),
            "soundex" to functions.unsupported(NO_PHONETICS),
            "difference" to functions.unsupported(NO_PHONETICS),
            "crc32" to functions.unsupported(NO_HASHES),
            "md5" to functions.unsupported(NO_HASHES),
            "sha" to functions.unsupported(NO_HASHES),
            "sha1" to functions.unsupported(NO_HASHES),
            "func_sha1" to functions.unsupported(NO_HASHES),
            "sha2" to functions.unsupported(NO_HASHES),
            "fnv_hash" to functions.unsupported(NO_HASHES),
            "to_hex" to functions.unsupported("PartiQL has no hexadecimal formatting"),
            "to_ascii" to functions.unsupported(LEADER_NODE),
            "get_byte" to functions.unsupported(LEADER_NODE),
            "set_bit" to functions.unsupported(LEADER_NODE),
            "set_byte" to functions.unsupported(LEADER_NODE),
            "getbit" to functions.unsupported(LEADER_NODE),
        )

    /*
     * Lengths
     */

    /**
     * `LEN`, `LENGTH`, `TEXTLEN`, `CHAR_LENGTH`, and `CHARACTER_LENGTH` are synonyms that count characters, which is
     * PartiQL `CHAR_LENGTH`. Redshift does not count the trailing blanks of a CHAR(n) value, and converts a numeric or
     * DATE argument to a character string; see [text].
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_LEN.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_TEXTLEN.html
     */
    private fun length(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
        rename: Boolean,
    ): Expr {
        functions.arity(name, args, 1)
        functions.scalar(name, setq)
        if (rename) functions.note(name, "Redshift `${name.text.uppercase()}` was replaced by PartiQL `CHAR_LENGTH`")
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types -> charLength(text(name, args[0], exprs[0], types[0], Padding.TRIM)) }
    }

    /**
     * `OCTET_LENGTH(s)` counts bytes. For a CHAR(n) value, both languages count the padding.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_OCTET_LENGTH.html
     */
    private fun octetLength(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1)
        functions.scalar(name, setq)
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types -> functions.fn("octet_length", text(name, args[0], exprs[0], types[0], Padding.COUNT)) }
    }

    /**
     * `LOWER(s)` and `UPPER(s)`. A CHAR(n) argument keeps its padding in both languages; the result is CHAR(n).
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_LOWER.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_UPPER.html
     */
    private fun case(
        fn: String,
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1)
        functions.scalar(name, setq)
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types -> functions.fn(fn, text(name, args[0], exprs[0], types[0], Padding.KEEP)) }
    }

    /*
     * Substrings
     */

    /**
     * `SUBSTRING(s FROM start [FOR length])`, `SUBSTRING(s FOR length)`, and the comma form `SUBSTRING(s, start
     * [, length])` are PartiQL `SUBSTRING(s FROM start [FOR length])`, with the same semantics for a start before the
     * first character and a length past the end. The start and the length must be integers: Redshift rounds a DECIMAL
     * or floating-point value half away from zero, and PartiQL rounds it half to even.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_SUBSTRING.html
     */
    fun substring(
        name: Token,
        value: Operand,
        start: Operand?,
        length: Operand?,
    ): Expr =
        functions.resolve(listOfNotNull(value, start, length), typing = "substring:${start != null}:${length != null}") { exprs, types ->
            val s = text(name, value, exprs[0], types[0], Padding.TRIM)
            var i = 1
            // SUBSTRING(s FOR n) starts at the first character.
            val from = start?.let { integer(name, "start", exprs[i], types[i++]) } ?: functions.int(1)
            val count = length?.let { integer(name, "length", exprs[i], types[i]) }
            exprSubstring(s, from, count)
        }

    private fun substringCall(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 2..3)
        functions.scalar(name, setq)
        return substring(name, args[0], args[1], args.getOrNull(2))
    }

    /**
     * `LEFT(s, n)` is `SUBSTRING(s FROM 1 FOR n)`, and `RIGHT(s, n)` is `SUBSTRING(s FROM CHAR_LENGTH(s) - (n - 1))`.
     * Redshift documents only a positive integer `n`, so `n` must be such a literal.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_LEFT.html
     */
    private fun leftRight(
        left: Boolean,
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 2)
        functions.scalar(name, setq)
        val fn = name.text.uppercase()
        val n = functions.intLiteral(args[1].expr) ?: throw UnsupportedConstructException.at(name, "`$fn` with a non-literal length")
        if (n < 1) throw UnsupportedConstructException.at(name, "`$fn` with a length less than 1")
        functions.note(name, "Redshift `$fn` was replaced by PartiQL `SUBSTRING`")
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            val s = text(name, args[0], exprs[0], types[0], Padding.TRIM)
            if (left) {
                exprSubstring(s, functions.int(1), functions.int(n))
            } else {
                val start = if (n == 1L) charLength(s) else exprOperator("-", charLength(s), functions.int(n - 1))
                exprSubstring(s, start, null)
            }
        }
    }

    /**
     * `SPLIT_PART(s, delimiter, part)` returns the part of `s` at the 1-based position `part`, or an empty string when
     * there are fewer parts. The translation reads the part from the array of PartiQL `SPLIT`, which also splits at the
     * literal delimiter:
     *
     * ```
     * CASE WHEN CARDINALITY(SPLIT(s, d)) >= part THEN SPLIT(s, d)[part - 1]
     *      WHEN s IS NOT NULL AND d IS NOT NULL AND part IS NOT NULL THEN '' END
     * ```
     *
     * The NULL tests are needed: PLK 1.6.1 fails with an internal error for an index into `SPLIT(NULL, d)`. A test is
     * left out for a literal. Redshift requires a part greater than 0; an empty delimiter is not documented.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/SPLIT_PART.html
     */
    private fun splitPart(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 3)
        functions.scalar(name, setq)
        val literalPart = functions.intLiteral(args[2].expr)
        if (literalPart != null && literalPart < 1) {
            throw UnsupportedConstructException.at(name, "`SPLIT_PART` with a part number less than 1")
        }
        val literalDelimiter = functions.stringLiteral(args[1].expr)
        if (literalDelimiter == "") throw UnsupportedConstructException.at(name, "`SPLIT_PART` with an empty delimiter")
        functions.note(name, "Redshift `SPLIT_PART` was replaced by a CASE expression over PartiQL `SPLIT`")
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            val s = text(name, args[0], exprs[0], types[0], Padding.TRIM)
            val d = text(name, args[1], exprs[1], types[1], Padding.TRIM)
            val part = integer(name, "part", exprs[2], types[2])
            val split = functions.fn("split", s, d)
            val index = if (literalPart != null) functions.int(literalPart - 1) else exprOperator("-", part, functions.int(1))
            val present = exprOperator(">=", functions.fn("cardinality", split), part)
            val notNull =
                listOfNotNull<Expr>(
                    exprNullPredicate(s, true),
                    if (literalDelimiter == null) exprNullPredicate(d, true) else null,
                    if (literalPart == null) exprNullPredicate(part, true) else null,
                ).reduce { a, b -> exprAnd(a, b) }
            // The subscript is in range.
            functions.nonMissing(
                exprCase(
                    null,
                    listOf(
                        exprCaseBranch(present, exprPath(split, listOf(exprPathStepElement(index)))),
                        exprCaseBranch(notNull, string("")),
                    ),
                    null,
                ),
            )
        }
    }

    /*
     * Trimming
     */

    /**
     * `TRIM([BOTH | LEADING | TRAILING] [chars FROM] s)` removes blanks, or the characters of the set `chars`, from the
     * chosen ends; BOTH is the default. For a CHAR(n) value, Redshift disregards the padding: the forms that do not
     * remove trailing blanks (LEADING, and the forms with `chars`) get the value without its padding.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_TRIM.html
     */
    fun trim(
        name: Token,
        spec: Token?,
        chars: Operand?,
        value: Operand,
    ): Expr {
        val trimSpec =
            when (spec?.type) {
                P.LEADING -> TrimSpec.LEADING()
                P.TRAILING -> TrimSpec.TRAILING()
                else -> TrimSpec.BOTH()
            }
        return trim(name, trimSpec, value, chars)
    }

    private fun trim(
        name: Token,
        spec: TrimSpec,
        value: Operand,
        chars: Operand?,
    ): Expr =
        functions.resolve(listOfNotNull(value, chars), typing = "trim:${spec.code()}") { exprs, types ->
            val removesPadding = chars == null && spec.code() != TrimSpec.LEADING
            val s = text(name, value, exprs[0], types[0], if (removesPadding) Padding.KEEP else Padding.TRIM)
            val set = chars?.let { text(name, it, exprs[1], types[1], Padding.TRIM) }
            exprTrim(s, set, spec)
        }

    /**
     * `BTRIM(s [, chars])`, `LTRIM(s [, chars])`, and `RTRIM(s [, chars])` are `TRIM(BOTH | LEADING | TRAILING [chars]
     * FROM s)`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_BTRIM.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_LTRIM.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_RTRIM.html
     */
    private fun trimFunction(
        spec: TrimSpec,
        specName: String,
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1..2)
        functions.scalar(name, setq)
        functions.note(name, "Redshift `${name.text.uppercase()}` was replaced by PartiQL `TRIM($specName ...)`")
        return trim(name, spec, args[0], args.getOrNull(1))
    }

    /*
     * Positions
     */

    /**
     * `POSITION(sub IN s)` is PartiQL `POSITION(sub IN s)`: the 1-based character position, or 0 when `sub` is not in
     * `s`. Redshift returns INTEGER, and PartiQL returns BIGINT. The trailing blanks of a CHAR(n) argument may count in
     * Redshift, which is not documented, so such an argument is unsupported.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_POSITION.html
     */
    fun position(
        name: Token,
        sub: Operand,
        str: Operand,
    ): Expr {
        functions.note(name, "Redshift `POSITION` returns INTEGER; PartiQL `POSITION` returns BIGINT")
        return positionOf(name, sub, str)
    }

    private fun positionOf(
        name: Token,
        sub: Operand,
        str: Operand,
    ): Expr =
        functions.resolve(listOf(sub, str), typing = name.text.lowercase()) { exprs, types ->
            exprPosition(text(name, sub, exprs[0], types[0], Padding.REJECT), text(name, str, exprs[1], types[1], Padding.REJECT))
        }

    /**
     * `STRPOS(s, sub)` is `POSITION(sub IN s)`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_STRPOS.html
     */
    private fun strpos(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 2)
        functions.scalar(name, setq)
        functions.note(
            name,
            "Redshift `STRPOS(s, sub)` was replaced by PartiQL `POSITION(sub IN s)`; the result type is BIGINT, not INTEGER",
        )
        return positionOf(name, args[1], args[0])
    }

    /**
     * `CHARINDEX(sub, s)` is `POSITION(sub IN s)`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_CHARINDEX.html
     */
    private fun charindex(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 2)
        functions.scalar(name, setq)
        functions.note(name, "Redshift `CHARINDEX` was replaced by PartiQL `POSITION`; the result type is BIGINT, not INTEGER")
        return positionOf(name, args[0], args[1])
    }

    /**
     * `OCTETINDEX(sub, s)` is the 1-based byte position of `sub` in `s`, 0 when `sub` is not in `s`, and 1 for an empty
     * `sub`:
     *
     * ```
     * CASE WHEN POSITION(sub IN s) = 0 THEN 0 ELSE OCTET_LENGTH(SUBSTRING(s FROM 1 FOR POSITION(sub IN s) - 1)) + 1 END
     * ```
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/OCTETINDEX.html
     */
    private fun octetIndex(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 2)
        functions.scalar(name, setq)
        functions.note(name, "Redshift `OCTETINDEX` was replaced by a CASE expression over PartiQL `POSITION` and `OCTET_LENGTH`")
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            val sub = text(name, args[0], exprs[0], types[0], Padding.REJECT)
            val s = text(name, args[1], exprs[1], types[1], Padding.REJECT)
            val position = exprPosition(sub, s)
            val prefix = exprSubstring(s, functions.int(1), exprOperator("-", position, functions.int(1)))
            exprCase(
                null,
                listOf(exprCaseBranch(exprOperator("=", position, functions.int(0)), functions.int(0))),
                exprOperator("+", functions.fn("octet_length", prefix), functions.int(1)),
            )
        }
    }

    /*
     * Replacements and concatenation
     */

    /**
     * `REPLACE(s, old, new)` replaces every occurrence of `old`, as PartiQL `REPLACE` does.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_REPLACE.html
     */
    private fun replace(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 3)
        functions.scalar(name, setq)
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            functions.fn("replace", *exprs.indices.map { text(name, args[it], exprs[it], types[it], Padding.TRIM) }.toTypedArray())
        }
    }

    /**
     * `CONCAT(a, b)` is `a || b`: NULL when either argument is NULL. A CHAR(n) argument is converted to VARCHAR, which
     * removes its padding; the result of two CHAR(n) arguments is not documented.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_CONCAT.html
     */
    private fun concat(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 2)
        functions.scalar(name, setq)
        functions.note(name, "Redshift `CONCAT` was replaced by PartiQL `||`")
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            if (types.all { it.code() == PType.CHAR }) {
                throw UnsupportedConstructException.at(name, "`CONCAT` of two CHAR arguments")
            }
            exprOperator("||", text(name, args[0], exprs[0], types[0], Padding.TRIM), text(name, args[1], exprs[1], types[1], Padding.TRIM))
        }
    }

    /**
     * `REPEAT(s, k)` and `REPLICATE(s, k)` repeat `s` k times. For an integer literal k from 1 to [MAX_REPEAT], the
     * translation concatenates k copies of `s`. Redshift does not document a count of 0 or less.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_REPEAT.html
     * https://docs.aws.amazon.com/redshift/latest/dg/r_REPLICATE.html
     */
    private fun repeat(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 2)
        functions.scalar(name, setq)
        val fn = name.text.uppercase()
        val k = functions.intLiteral(args[1].expr) ?: throw UnsupportedConstructException.at(name, "`$fn` with a non-literal count")
        if (k < 1 || k > MAX_REPEAT) throw UnsupportedConstructException.at(name, "`$fn` with a count other than 1 to $MAX_REPEAT")
        functions.note(name, "Redshift `$fn` was replaced by PartiQL `||`")
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            val s = text(name, args[0], exprs[0], types[0], Padding.TRIM)
            (2..k).fold(s) { acc, _ -> exprOperator("||", acc, s) }
        }
    }

    /**
     * `LPAD(s, n [, fill])` and `RPAD(s, n [, fill])` pad `s` to n characters with `fill` (blanks by default), and
     * truncate a longer `s` to n characters. For an integer literal n and a non-empty string literal `fill`, the
     * translation precomputes the fill string F of n characters:
     *
     * ```
     * CASE WHEN CHAR_LENGTH(s) >= n THEN SUBSTRING(s FROM 1 FOR n) ELSE SUBSTRING(F FROM 1 FOR n - CHAR_LENGTH(s)) || s END
     * ```
     *
     * For RPAD, the fill follows `s`. A length of 0 or less gives an empty string.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_LPAD.html
     */
    private fun pad(
        left: Boolean,
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 2..3)
        functions.scalar(name, setq)
        val fn = name.text.uppercase()
        val n = functions.intLiteral(args[1].expr) ?: throw UnsupportedConstructException.at(name, "`$fn` with a non-literal length")
        if (n > MAX_PAD) throw UnsupportedConstructException.at(name, "`$fn` with a length greater than $MAX_PAD")
        val fill =
            if (args.size == 3) {
                functions.stringLiteral(args[2].expr) ?: throw UnsupportedConstructException.at(name, "`$fn` with a non-literal fill")
            } else {
                " "
            }
        if (fill.isEmpty()) throw UnsupportedConstructException.at(name, "`$fn` with an empty fill")
        functions.note(name, "Redshift `$fn` was replaced by a CASE expression over PartiQL `SUBSTRING`")
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            val s = text(name, args[0], exprs[0], types[0], Padding.TRIM)
            if (n <= 0) {
                exprSubstring(s, functions.int(1), functions.int(0))
            } else {
                val codePoints = fill.codePoints().toArray()
                val f = String(IntArray(n.toInt()) { codePoints[it % codePoints.size] }, 0, n.toInt())
                val padding = exprSubstring(string(f), functions.int(1), exprOperator("-", functions.int(n), charLength(s)))
                exprCase(
                    null,
                    listOf(
                        exprCaseBranch(
                            exprOperator(">=", charLength(s), functions.int(n)),
                            exprSubstring(s, functions.int(1), functions.int(n)),
                        ),
                    ),
                    if (left) exprOperator("||", padding, s) else exprOperator("||", s, padding),
                )
            }
        }
    }

    /**
     * `TRANSLATE(s, from, to)` replaces each character of `from` with the character at the same position of `to`, or
     * removes it when `to` is shorter. For string literals `from` and `to`, the translation is a chain of PartiQL
     * `REPLACE` calls, ordered so that no call replaces a character that an earlier call inserted. A character that
     * occurs again in `from` keeps its first mapping, and a mapping that cannot be ordered (a cycle like 'ab' to 'ba')
     * is unsupported.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_TRANSLATE.html
     */
    private fun translate(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 3)
        functions.scalar(name, setq)
        val from = functions.stringLiteral(args[1].expr)
        val to = functions.stringLiteral(args[2].expr)
        if (from == null || to == null) throw UnsupportedConstructException.at(name, "`TRANSLATE` with non-literal characters")
        val replacements = translations(name, from, to)
        functions.note(name, "Redshift `TRANSLATE` was replaced by PartiQL `REPLACE` calls")
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            val s = text(name, args[0], exprs[0], types[0], Padding.TRIM)
            replacements.fold(s) { acc, (old, new) -> functions.fn("replace", acc, string(old), string(new)) }
        }
    }

    /**
     * Returns the (character, replacement) pairs of `TRANSLATE(s, from, to)` in an order that gives the simultaneous
     * replacement of TRANSLATE: a pair that replaces character c comes before every pair that inserts c.
     */
    private fun translations(
        name: Token,
        from: String,
        to: String,
    ): List<Pair<String, String>> {
        val fromChars = from.codePoints().toArray().map { String(Character.toChars(it)) }
        val toChars = to.codePoints().toArray().map { String(Character.toChars(it)) }
        val seen = HashSet<String>()
        val pairs =
            fromChars.indices.mapNotNull { i ->
                val old = fromChars[i]
                val new = toChars.getOrElse(i) { "" }
                if (!seen.add(old) || old == new) null else old to new
            }
        // A topological order of the pairs; the input order breaks ties, so that the output is deterministic.
        val ordered = mutableListOf<Pair<String, String>>()
        val remaining = pairs.toMutableList()
        while (remaining.isNotEmpty()) {
            val next =
                remaining.firstOrNull { pair -> remaining.none { other -> other !== pair && other.first == pair.second } }
                    ?: throw UnsupportedConstructException.at(name, "`TRANSLATE` with a cyclic character mapping")
            ordered.add(next)
            remaining.remove(next)
        }
        return ordered
    }

    /*
     * Characters and quoting
     */

    /**
     * `CHR(n)` returns the ASCII character with the code n, and NULL when there is none (also for 0). For an integer
     * literal, the translation is the character string. CHR returns CHAR, so CHR(32), a single blank, is an
     * insignificant trailing blank in Redshift; it is unsupported.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_CHR.html
     */
    private fun chr(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1)
        functions.scalar(name, setq)
        val n = functions.intLiteral(args[0].expr) ?: throw UnsupportedConstructException.at(name, "`CHR` with a non-literal code")
        if (n == BLANK) throw UnsupportedConstructException.at(name, "`CHR(32)` (a CHAR blank)")
        return if (n in 1..MAX_ASCII) {
            val value = n.toInt().toChar().toString()
            functions.note(name, "Redshift `CHR($n)` was replaced by its character string")
            string(value)
        } else {
            functions.note(name, "Redshift `CHR($n)` returns NULL, because $n is not the code of an ASCII character")
            exprLit(Literal.nul())
        }
    }

    /**
     * `QUOTE_LITERAL(s)` encloses `s` in single quotes and doubles the single quotes and backslashes in it; a number
     * is treated as a string:
     *
     * ```
     * ('''' || REPLACE(REPLACE(s, '\', '\\'), '''', '''''')) || ''''
     * ```
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/r_QUOTE_LITERAL.html
     */
    private fun quoteLiteral(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        functions.arity(name, args, 1)
        functions.scalar(name, setq)
        functions.note(name, "Redshift `QUOTE_LITERAL` was replaced by PartiQL `||` with quotes")
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            val type = types[0]
            val quoted =
                when {
                    isInteger(type) -> functions.castToString(exprs[0])
                    type.code() == PType.VARCHAR || type.code() == PType.STRING ->
                        functions.fn("replace", functions.fn("replace", exprs[0], string("\\"), string("\\\\")), string("'"), string("''"))
                    else -> throw UnsupportedConstructException.at(name, "`QUOTE_LITERAL` of ${FunctionTypes.describe(type, "argument")}")
                }
            exprOperator("||", exprOperator("||", string("'"), quoted), string("'"))
        }
    }

    /*
     * Regular expressions
     *
     * PartiQL has no regular expressions. A pattern that is a string literal without regular expression operators
     * matches only itself, so these functions have translations for such patterns.
     */

    /**
     * `REGEXP_COUNT(s, p)` with a literal pattern of m characters is the number of non-overlapping occurrences:
     * `(CHAR_LENGTH(s) - CHAR_LENGTH(REPLACE(s, p, ''))) / m`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/REGEXP_COUNT.html
     */
    private fun regexpCount(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        val pattern = literalPattern(name, args, setq, 2..2, emptyAllowed = false)
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            val s = text(name, args[0], exprs[0], types[0], Padding.TRIM)
            val removed = exprOperator("-", charLength(s), charLength(functions.fn("replace", s, string(pattern), string(""))))
            val m = pattern.codePointCount(0, pattern.length)
            if (m == 1) removed else exprOperator("/", removed, functions.int(m.toLong()))
        }
    }

    /**
     * `REGEXP_INSTR(s, p)` with a non-empty literal pattern is `POSITION(p IN s)`. Redshift returns INTEGER, and PartiQL
     * returns BIGINT.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/REGEXP_INSTR.html
     */
    private fun regexpInstr(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        val pattern = literalPattern(name, args, setq, 2..2, emptyAllowed = false)
        functions.note(name, "Redshift `REGEXP_INSTR` returns INTEGER; PartiQL `POSITION` returns BIGINT")
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            exprPosition(string(pattern), text(name, args[0], exprs[0], types[0], Padding.TRIM))
        }
    }

    /**
     * `REGEXP_REPLACE(s, p [, r])` with a non-empty literal pattern is `REPLACE(s, p, r)`; the default `r` is an empty
     * string. `r` must be a string literal without a backslash, which would be a back reference.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/REGEXP_REPLACE.html
     */
    private fun regexpReplace(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        val pattern = literalPattern(name, args, setq, 2..3, emptyAllowed = false)
        val replacement =
            if (args.size == 3) {
                functions.stringLiteral(args[2].expr)?.takeIf { '\\' !in it && '$' !in it }
                    ?: throw UnsupportedConstructException.at(
                        name,
                        "`REGEXP_REPLACE` with a replacement that is not a string literal without a backslash or dollar sign",
                    )
            } else {
                ""
            }
        functions.note(
            name,
            "Redshift `REGEXP_REPLACE` with a pattern without regular expression operators was replaced by PartiQL `REPLACE`",
        )
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            functions.fn("replace", text(name, args[0], exprs[0], types[0], Padding.TRIM), string(pattern), string(replacement))
        }
    }

    /**
     * `REGEXP_SUBSTR(s, p)` with a literal pattern is the pattern if it occurs in `s`, and an empty string otherwise:
     * `CASE WHEN POSITION(p IN s) > 0 THEN p WHEN s IS NOT NULL THEN '' END`.
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/REGEXP_SUBSTR.html
     */
    private fun regexpSubstr(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
    ): Expr {
        val pattern = literalPattern(name, args, setq, 2..2, emptyAllowed = true)
        functions.note(
            name,
            "Redshift `REGEXP_SUBSTR` with a pattern without regular expression operators was replaced by a CASE expression " +
                "over PartiQL `POSITION`",
        )
        return functions.resolve(args, typing = name.text.lowercase()) { exprs, types ->
            val s = text(name, args[0], exprs[0], types[0], Padding.TRIM)
            exprCase(
                null,
                listOf(
                    exprCaseBranch(exprOperator(">", exprPosition(string(pattern), s), functions.int(0)), string(pattern)),
                    exprCaseBranch(exprNullPredicate(s, true), string("")),
                ),
                null,
            )
        }
    }

    /**
     * Returns the pattern of a regular expression function call, a string literal without regular expression operators,
     * or reports the call as unsupported.
     */
    private fun literalPattern(
        name: Token,
        args: List<Operand>,
        setq: SetQuantifier?,
        arity: IntRange,
        emptyAllowed: Boolean,
    ): String {
        functions.scalar(name, setq)
        val fn = name.text.uppercase()
        if (args.size !in arity) {
            throw UnsupportedConstructException.at(name, "`$fn` with ${args.size} argument(s); PartiQL has no regular expressions")
        }
        val pattern = functions.stringLiteral(args[1].expr)
        if (pattern == null || pattern.any { it in REGEX_OPERATORS } || (!emptyAllowed && pattern.isEmpty())) {
            throw UnsupportedConstructException.at(name, "`$fn` with a regular expression pattern; PartiQL has no regular expressions")
        }
        return pattern
    }

    /*
     * Arguments
     */

    /**
     * Converts [x], the translation of the argument [operand] of the string function [name] with the static type [type],
     * to a character string, as Redshift converts it:
     *  - VARCHAR and SUPER values are unchanged.
     *  - A CHAR(n) value is handled as [padding] says.
     *  - SMALLINT, INTEGER, BIGINT, and DATE values become `CAST(x AS STRING)`; the text is the same in both languages.
     *  - A DECIMAL(p, s) value becomes `CAST(x AS STRING)` when s is at most 6 and the argument is a column, a literal,
     *    or a CAST. PartiQL writes a value with a larger scale with an exponent ('0E-8'), and the scale of other DECIMAL
     *    expressions (for example a quotient) can differ from the Redshift scale.
     *  - Other types are unsupported: the text differs (PartiQL writes a TIMESTAMP with a 'T', and a DOUBLE PRECISION
     *    value with an exponent).
     *
     * https://docs.aws.amazon.com/redshift/latest/dg/c_Supported_data_types.html
     */
    internal fun text(
        name: Token,
        operand: Operand,
        x: Expr,
        type: PType,
        padding: Padding,
    ): Expr {
        val fn = name.text.uppercase()
        if ((padding == Padding.COUNT || padding == Padding.REJECT) && functions.unwrap(operand.ctx) in functions.charBranches) {
            throw UnsupportedConstructException.at(name, "`$fn` of a conditional expression that mixes CHAR with other character values")
        }
        return when {
            type.code() == PType.CHAR ->
                when (padding) {
                    Padding.TRIM -> functions.trimTrailing(x)
                    Padding.KEEP, Padding.COUNT -> x
                    Padding.REJECT -> throw UnsupportedConstructException.at(name, "CHAR argument to `$fn`")
                }
            isText(type) || isDynamic(type) || isUnknown(type) -> x
            isInteger(type) || type.code() == PType.DATE -> functions.castToString(x)
            isDecimal(type) && type.scale <= MAX_PLAIN_SCALE && functions.isColumnLiteralOrCast(operand.ctx) -> functions.castToString(x)
            else -> throw UnsupportedConstructException.at(name, "implicit conversion of ${FunctionTypes.name(type)} to VARCHAR in `$fn`")
        }
    }

    /**
     * Checks that [x], the argument [what] of [name], is an integer, and returns it.
     */
    private fun integer(
        name: Token,
        what: String,
        x: Expr,
        type: PType,
    ): Expr {
        if (!isInteger(type) && !isUnknown(type)) {
            throw UnsupportedConstructException.at(name, "`${name.text.uppercase()}` with a $what of type ${FunctionTypes.name(type)}")
        }
        return x
    }

    private fun charLength(x: Expr): Expr = functions.fn("char_length", x)

    private fun string(value: String): Expr = exprLit(Literal.string(value))

    private companion object {
        const val NO_CODE_POINTS = "PartiQL has no character code function"
        const val BYTE_ORDER = "PartiQL compares strings by UTF-16 code units, not bytes"
        const val NO_PHONETICS = "PartiQL has no phonetic encoding"
        const val NO_HASHES = "PartiQL has no hash functions"
        const val LEADER_NODE = "a deprecated leader node-only function"

        /** The largest scale of a DECIMAL value that PartiQL converts to a string without an exponent. */
        const val MAX_PLAIN_SCALE = 6

        const val MAX_REPEAT = 10L
        const val MAX_PAD = 65535L
        const val MAX_ASCII = 127L
        const val BLANK = 32L

        /** The operators of POSIX regular expressions. */
        const val REGEX_OPERATORS = ".[](){}*+?|^$\\"
    }
}
