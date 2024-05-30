package org.partiql.scribe.targets.redshift

import org.partiql.ast.DatetimeField
import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.exprBetween
import org.partiql.ast.exprCall
import org.partiql.ast.exprCast
import org.partiql.ast.exprCollection
import org.partiql.ast.exprInCollection
import org.partiql.ast.exprIsType
import org.partiql.ast.exprLike
import org.partiql.ast.exprLit
import org.partiql.ast.exprVar
import org.partiql.ast.identifierSymbol
import org.partiql.ast.typeCustom
import org.partiql.ast.typeDecimal
import org.partiql.scribe.ProblemCallback
import org.partiql.scribe.error
import org.partiql.scribe.info
import org.partiql.scribe.sql.SqlArg
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.warn
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.PartiQLValueType
import org.partiql.value.symbolValue

@OptIn(PartiQLValueExperimental::class)
public open class RedshiftCalls(private val log: ProblemCallback) : SqlCalls() {

    override val rules: Map<String, SqlCallFn> = super.rules.toMutableMap().apply {
        this["utcnow"] = ::utcnow
        // -- Extensions
        this["split"] = ::split
    }

    /**
     * SQL IN predicate is defined as `IN (...)`.
     */
    override fun inCollection(args: List<SqlArg>): Expr {
        val lhs = args[0].expr
        var rhs = args[1].expr
        rhs = when (rhs) {
            is Expr.Collection -> exprCollection(Expr.Collection.Type.LIST, rhs.values)
            is Expr.SFW -> rhs
            else -> error("IN predicate expected expression list or subquery, found ${rhs::class.qualifiedName}")
        }
        return exprInCollection(lhs, rhs, false)
    }

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATEADD_function.html
     */
    override fun dateAdd(part: DatetimeField, args: SqlArgs): Expr {
        val id = id("DATEADD")
        log.info("PartiQL `date_add` was replaced by Redshift `dateadd`")
        val arg0 = exprLit(symbolValue(part.name.uppercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        return exprCall(id, listOf(arg0, arg1, arg2))
    }

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/r_DATEDIFF_function.html
     */
    override fun dateDiff(part: DatetimeField, args: SqlArgs): Expr {
        val id = id("DATEDIFF")
        log.info("PartiQL `date_diff` was replaced by Redshift `datediff`")
        val arg0 = exprLit(symbolValue(part.name.uppercase()))
        val arg1 = args[0].expr
        val arg2 = args[1].expr
        return exprCall(id, listOf(arg0, arg1, arg2))
    }

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/r_SYSDATE.html
     */
    private fun utcnow(args: SqlArgs): Expr {
        val id = id("sysdate")
        log.info("PartiQL `utcnow()` was replaced by Redshift `SYSDATE`")
        return exprVar(id, Expr.Var.Scope.DEFAULT)
    }

    /**
     * https://docs.aws.amazon.com/redshift/latest/dg/split_to_array.html
     */
    private fun split(args: SqlArgs): Expr {
        val id = id("split_to_array")
        log.info("PartiQL `split(<string>, <string>) -> list<string>` was replaced by Redshift `split_to_array(<string>, <string>) -> SUPER`")
        val arg0 = args[0].expr
        val arg1 = args[1].expr
        return exprCall(id, listOf(arg0, arg1))
    }

    override fun rewriteCast(type: PartiQLValueType, args: SqlArgs): Expr {
        return when (type) {
            PartiQLValueType.ANY -> {
                log.error("PartiQL `ANY` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.INT8 -> {
                log.error("PartiQL `INT8` type (1-byte integer) not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.INT -> {
                log.warn("PartiQL `INT` type (arbitrary precision integer) not supported in Redshift, mapped to DECIMAL(38,0)")
                return exprCast(args[0].expr, typeDecimal(38, 0))
            }
            PartiQLValueType.MISSING -> {
                log.error("PartiQL `MISSING` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.SYMBOL -> {
                log.error("PartiQL `SYMBOL` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.INTERVAL -> {
                log.error("PartiQL `INTERVAL` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.BLOB -> {
                log.error("PartiQL `BLOB` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.CLOB -> {
                log.error("PartiQL `CLOB` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.BAG -> {
                log.error("PartiQL `BAG` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.LIST -> {
                log.error("PartiQL `LIST` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.SEXP -> {
                log.error("PartiQL `SEXP` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            PartiQLValueType.STRUCT -> {
                log.error("PartiQL `STRUCT` type not supported in Redshift")
                super.rewriteCast(type, args)
            }
            // using the customer type to rename type
            PartiQLValueType.FLOAT32 -> exprCast(args[0].expr, typeCustom("FLOAT4"))
            PartiQLValueType.FLOAT64 -> exprCast(args[0].expr, typeCustom("FLOAT8"))
            PartiQLValueType.BINARY -> exprCast(args[0].expr, typeCustom("VARBYTE"))
            PartiQLValueType.BYTE -> TODO("Mapping to VARBYTE(1), do this after supporting parameterized type")
            else -> super.rewriteCast(type, args)
        }
    }

    /**
     * As far as the documentation goes, there is no indication that redshift support type assertion other than null.
     * I.e., var IS NULL is supported, var IS INT2 may not.
     * Also, there is seemingly no helper function like pg_typeof() in redshift either.
     * Throwing a warning message if the type assertion is not targeting null type.
     */
    override fun isType(type: PartiQLValueType, args: SqlArgs): Expr {
        when (type) {
            PartiQLValueType.NULL -> Unit
            else -> log.warn("Redshift does not support type assertion on ${type.name} ")
        }
        return super.isType(type, args)
    }

    private fun id(symbol: String) = identifierSymbol(symbol, Identifier.CaseSensitivity.INSENSITIVE)
}
