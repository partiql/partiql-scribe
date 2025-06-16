package org.partiql.scribe.sql

import org.partiql.ast.Ast.exprArray
import org.partiql.ast.Ast.exprBag
import org.partiql.ast.Ast.exprCase
import org.partiql.ast.Ast.exprCaseBranch
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprCoalesce
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprNullIf
import org.partiql.ast.Ast.exprPath
import org.partiql.ast.Ast.exprPathStepElement
import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.exprStruct
import org.partiql.ast.Ast.exprStructField
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.queryBodySetOp
import org.partiql.ast.Ast.setOp
import org.partiql.ast.DataType
import org.partiql.ast.Identifier
import org.partiql.ast.Identifier.Simple.regular
import org.partiql.ast.Literal
import org.partiql.ast.SetOpType
import org.partiql.ast.SetQuantifier
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprPath
import org.partiql.plan.Operator
import org.partiql.plan.OperatorVisitor
import org.partiql.plan.rel.Rel
import org.partiql.plan.rex.Rex
import org.partiql.plan.rex.RexArray
import org.partiql.plan.rex.RexBag
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexCase
import org.partiql.plan.rex.RexCast
import org.partiql.plan.rex.RexCoalesce
import org.partiql.plan.rex.RexDispatch
import org.partiql.plan.rex.RexError
import org.partiql.plan.rex.RexLit
import org.partiql.plan.rex.RexNullIf
import org.partiql.plan.rex.RexPathIndex
import org.partiql.plan.rex.RexPathKey
import org.partiql.plan.rex.RexPathSymbol
import org.partiql.plan.rex.RexPivot
import org.partiql.plan.rex.RexSelect
import org.partiql.plan.rex.RexSpread
import org.partiql.plan.rex.RexStruct
import org.partiql.plan.rex.RexSubquery
import org.partiql.plan.rex.RexSubqueryComp
import org.partiql.plan.rex.RexSubqueryIn
import org.partiql.plan.rex.RexSubqueryTest
import org.partiql.plan.rex.RexTable
import org.partiql.plan.rex.RexVar
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField
import org.partiql.spi.value.Datum
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

public typealias TypeEnv = List<PTypeField>

public class Locals(
    public val env: TypeEnv,
    public val aggregations: List<Expr> = emptyList(),
) {
    public companion object {
        public val EMPTY: Locals = Locals(env = emptyList(), aggregations = emptyList())
    }
}

private const val UNSPECIFIED_LENGTH = "UNSPECIFIED_LENGTH"
private const val UNSPECIFIED_PRECISION = "UNSPECIFIED_PRECISION"
private const val UNSPECIFIED_SCALE = "UNSPECIFIED_SCALE"

public open class RexConverter(
    private val transform: PlanToAst,
    private val locals: Locals,
    private val context: ScribeContext,
) : OperatorVisitor<Expr, Unit> {
    private val listener = context.getProblemListener()

    /**
     * Convert a [Rex] to an [Expr].
     */
    public fun apply(rex: Rex): Expr = rex.accept(this, Unit)

    override fun defaultReturn(
        operator: Operator,
        ctx: Unit,
    ): Expr {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "$operator is not yet supported",
            ),
        )
    }

    override fun defaultVisit(
        operator: Operator,
        ctx: Unit,
    ): Expr {
        return defaultReturn(operator, ctx)
    }

    public fun visitRex(
        rex: Rex,
        ctx: Unit,
    ): Expr {
        return visit(rex, ctx)
    }

    override fun visitArray(
        rex: RexArray,
        ctx: Unit,
    ): Expr {
        val values = rex.values.map { visitRex(it, ctx) }
        return exprArray(values)
    }

    override fun visitBag(
        rex: RexBag,
        ctx: Unit,
    ): Expr {
        val values = rex.values.map { visitRex(it, ctx) }
        return exprBag(values)
    }

    override fun visitCall(
        rex: RexCall,
        ctx: Unit,
    ): Expr {
        val fn = rex.function
        val args = rex.args.map { SqlArg(visitRex(it, ctx), it.type.pType) }
        return transform.getFunction(fn.signature.name, args)
    }

    override fun visitCase(
        rex: RexCase,
        ctx: Unit,
    ): Expr {
        val matchExpr = rex.match?.let { visitRex(it, ctx) }
        val default = rex.default?.let { visitRex(it, ctx) }
        val branches =
            rex.branches.map {
                val condition = visitRex(it.condition, ctx)
                val result = visitRex(it.result, ctx)
                exprCaseBranch(condition, result)
            }
        return exprCase(matchExpr, branches, default)
    }

    private fun PType.unspecifiedLength() = metas[UNSPECIFIED_LENGTH] == true

    private fun PType.unspecifiedPrecision() = metas[UNSPECIFIED_PRECISION] == true

    private fun PType.unspecifiedScale() = metas[UNSPECIFIED_SCALE] == true

    override fun visitCast(
        rex: RexCast,
        ctx: Unit,
    ): Expr {
        val value = visitRex(rex.operand, ctx)
        val targetType =
            when (rex.target.code()) {
                // BOOL type
                PType.BOOL -> DataType.BOOL()
                // INTEGER types
                PType.TINYINT -> DataType.TINYINT()
                PType.SMALLINT -> DataType.SMALLINT()
                PType.INTEGER -> DataType.INT()
                PType.BIGINT -> DataType.BIGINT()
                // DECIMAL types
                PType.NUMERIC -> {
                    val noPrecision = rex.target.unspecifiedPrecision()
                    val noScale = rex.target.unspecifiedScale()
                    when {
                        noPrecision && noScale -> DataType.NUMERIC()
                        noScale -> DataType.NUMERIC(rex.target.precision)
                        noPrecision -> error("Invalid PType in plan ${rex.target} has a scale but no precision specified")
                        else -> DataType.NUMERIC(rex.target.precision, rex.target.scale)
                    }
                }
                PType.DECIMAL -> {
                    val noPrecision = rex.target.unspecifiedPrecision()
                    val noScale = rex.target.unspecifiedScale()
                    when {
                        noPrecision && noScale -> DataType.DECIMAL()
                        noScale -> DataType.DECIMAL(rex.target.precision)
                        noPrecision -> error("Invalid PType in plan ${rex.target} has a scale but no precision specified")
                        else -> DataType.DECIMAL(rex.target.precision, rex.target.scale)
                    }
                }
                // Approximate numeric types
                PType.REAL -> DataType.REAL()
                PType.DOUBLE -> DataType.DOUBLE_PRECISION()
                // String types
                PType.CHAR ->
                    when (rex.target.unspecifiedLength()) {
                        true -> DataType.CHAR()
                        false -> DataType.CHAR(rex.target.length)
                    }
                PType.VARCHAR ->
                    when (rex.target.unspecifiedLength()) {
                        true -> DataType.VARCHAR()
                        false -> DataType.VARCHAR(rex.target.length)
                    }
                PType.STRING -> DataType.STRING()
                // Datetime types
                PType.DATE -> DataType.DATE()
                PType.TIME ->
                    when (rex.target.unspecifiedPrecision()) {
                        true -> DataType.TIME()
                        false -> DataType.TIME(rex.target.precision)
                    }
                PType.TIMEZ ->
                    when (rex.target.unspecifiedPrecision()) {
                        true -> DataType.TIME_WITH_TIME_ZONE()
                        false -> DataType.TIME_WITH_TIME_ZONE(rex.target.precision)
                    }
                PType.TIMESTAMP ->
                    when (rex.target.unspecifiedPrecision()) {
                        true -> DataType.TIMESTAMP()
                        false -> DataType.TIMESTAMP(rex.target.precision)
                    }
                PType.TIMESTAMPZ ->
                    when (rex.target.unspecifiedPrecision()) {
                        true -> DataType.TIMESTAMP_WITH_TIME_ZONE()
                        false -> DataType.TIMESTAMP_WITH_TIME_ZONE(rex.target.precision)
                    }
                // Dynamic type
                PType.DYNAMIC -> return value
                PType.BAG -> return value
                else ->
                    listener.reportAndThrow(
                        ScribeProblem.simpleError(
                            code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                            message = "CAST with type ${rex.target.code()} is not yet supported",
                        ),
                    )
            }
        return exprCast(
            value = value,
            asType = targetType,
        )
    }

    override fun visitCoalesce(
        rex: RexCoalesce,
        ctx: Unit,
    ): Expr {
        val args = rex.args.map { visitRex(it, ctx) }
        return exprCoalesce(args)
    }

    override fun visitDispatch(
        rex: RexDispatch,
        ctx: Unit,
    ): Expr {
        val fn = rex.functions.first()
        val args = rex.args.map { SqlArg(visitRex(it, ctx), it.type.pType) }
        return transform.getFunction(fn.signature.name, args)
    }

    override fun visitError(
        rex: RexError,
        ctx: Unit,
    ): Expr {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message =
                    "ERROR Rex node indicates there was an error in the plan. " +
                        "Check the PErrorListener used during planning for more error details.",
            ),
        )
    }

    override fun visitLit(
        rex: RexLit,
        ctx: Unit,
    ): Expr {
        // convert plan literal Rex to ast literal Expr
        val datum = rex.datum
        return exprLit(datum.toLiteral())
    }

    private fun Datum.toLiteral(): Literal {
        if (this.isNull) {
            return Literal.nul()
        }
        if (this.isMissing) {
            return Literal.missing()
        }
        return when (this.type.code()) {
            // BOOL literal
            PType.BOOL -> Literal.bool(this.boolean)
            // INTEGER literals
            PType.TINYINT -> Literal.intNum(this.byte.toInt()) // TODO define byte fn version in PLK Literal?
            PType.SMALLINT -> Literal.intNum(this.short.toInt()) // TODO define short fn version in PLK Literal?
            PType.INTEGER -> Literal.intNum(this.int)
            PType.BIGINT -> Literal.intNum(this.long)
            // DECIMAL literals
            PType.NUMERIC, PType.DECIMAL -> {
                when (this.type.scale) {
                    0 -> Literal.intNum(this.bigDecimal.unscaledValue())
                    else -> Literal.exactNum(this.bigDecimal)
                }
            }
            // Approximate numeric literals
            PType.REAL -> Literal.approxNum(this.float.toString()) // TODO some possible data loss here?
            PType.DOUBLE -> Literal.approxNum(this.double.toString()) // TODO some possible data loss here?
            // String literals
            PType.CHAR, PType.VARCHAR, PType.STRING -> Literal.string(this.string)
            // Date literal
            PType.DATE -> Literal.typedString(DataType.DATE(), this.localDate.toString())
            PType.TIME -> Literal.typedString(DataType.TIME(), this.localTime.format(DateTimeFormatter.ISO_LOCAL_TIME))
            PType.TIMESTAMP ->
                Literal.typedString(
                    DataType.TIMESTAMP(),
                    "${this.localDate} ${this.localTime.format(DateTimeFormatter.ISO_LOCAL_TIME)}",
                )
            PType.TIMEZ -> {
                // TODO precision
                val offsetString =
                    when (val offset = this.offsetTime.offset) {
                        ZoneOffset.UTC -> "+00:00"
                        else -> offset.toString()
                    }
                Literal.typedString(
                    DataType.TIME_WITH_TIME_ZONE(),
                    "${this.localTime.format(DateTimeFormatter.ISO_LOCAL_TIME)}$offsetString",
                )
            }
            PType.TIMESTAMPZ -> {
                // TODO precision
                val offsetString =
                    when (val offset = this.offsetTime.offset) {
                        ZoneOffset.UTC -> "+00:00"
                        else -> offset.toString()
                    }
                Literal.typedString(
                    DataType.TIMESTAMP_WITH_TIME_ZONE(),
                    "${this.localDate} ${this.localTime.format(DateTimeFormatter.ISO_LOCAL_TIME)}$offsetString",
                )
            }
            else ->
                listener.reportAndThrow(
                    ScribeProblem.simpleError(
                        code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                        message = "LITERAL with type ${this.type.code()} is not yet supported",
                    ),
                )
        }
    }

    override fun visitNullIf(
        rex: RexNullIf,
        ctx: Unit,
    ): Expr {
        val v1 = visitRex(rex.v1, ctx)
        val v2 = visitRex(rex.v2, ctx)
        return exprNullIf(v1, v2)
    }

    override fun visitPathIndex(
        rex: RexPathIndex,
        ctx: Unit,
    ): Expr {
        val prev = visitRex(rex.operand, ctx)
        val step = exprPathStepElement(visitRex(rex.index, ctx))
        return if (prev is ExprPath) {
            exprPath(prev.root, prev.steps + step)
        } else {
            exprPath(prev, listOf(step))
        }
    }

    override fun visitPathKey(
        rex: RexPathKey,
        ctx: Unit,
    ): Expr {
        val prev = visitRex(rex.operand, ctx)
        val step = exprPathStepElement(visitRex(rex.key, ctx))
        return if (prev is ExprPath) {
            exprPath(prev.root, prev.steps + step)
        } else {
            exprPath(prev, listOf(step))
        }
    }

    override fun visitPathSymbol(
        rex: RexPathSymbol,
        ctx: Unit,
    ): Expr {
        val prev = visitRex(rex.operand, ctx)
        val step = exprPathStepField(regular(rex.symbol))
        return if (prev is ExprPath) {
            exprPath(prev.root, prev.steps + step)
        } else {
            exprPath(prev, listOf(step))
        }
    }

    override fun visitPivot(
        rex: RexPivot,
        ctx: Unit,
    ): Expr {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "PIVOT is not yet supported",
            ),
        )
    }

    private fun relSetOpToBagOp(
        left: Rel,
        right: Rel,
        isAll: Boolean,
        setOpType: SetOpType,
        ctx: Unit,
    ): Expr {
        val leftRex =
            RexSelect.create(
                left,
                RexVar.create(0, 0, left.type.fields.first().type),
            )
        val rightRex =
            RexSelect.create(
                right,
                RexVar.create(0, 0, right.type.fields.first().type),
            )
        val lhsExpr = visitRex(leftRex, ctx)
        val rhsExpr = visitRex(rightRex, ctx)
        return exprQuerySet(
            body =
                queryBodySetOp(
                    type =
                        setOp(
                            setOpType = setOpType,
                            setq =
                                when (isAll) {
                                    true -> SetQuantifier.ALL()
                                    false -> SetQuantifier.DISTINCT()
                                },
                        ),
                    lhs = lhsExpr,
                    rhs = rhsExpr,
                    isOuter = false,
                ),
        )
    }

    override fun visitSelect(
        rex: RexSelect,
        ctx: Unit,
    ): Expr {
        val inputRel = rex.input
        val relConverter = RelConverter(transform, context)
        return relConverter.apply(inputRel, ctx).toExprQuerySet()
    }

    override fun visitStruct(
        rex: RexStruct,
        ctx: Unit,
    ): Expr {
        val fields =
            rex.fields.map {
                exprStructField(
                    name = visitRex(it.key, ctx),
                    value = visitRex(it.value, ctx),
                )
            }
        return exprStruct(fields)
    }

    override fun visitSubquery(
        rex: RexSubquery,
        ctx: Unit,
    ): Expr {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "SUBQUERY is not yet supported",
            ),
        )
    }

    override fun visitSubqueryComp(
        rex: RexSubqueryComp,
        ctx: Unit,
    ): Expr {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "SUBQUERYCOMP is not yet supported",
            ),
        )
    }

    override fun visitSubqueryIn(
        rex: RexSubqueryIn,
        ctx: Unit,
    ): Expr {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "SUBQUERYIN is not yet supported",
            ),
        )
    }

    override fun visitSubqueryTest(
        rex: RexSubqueryTest,
        ctx: Unit,
    ): Expr {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "SUBQUERYTEST is not yet supported",
            ),
        )
    }

    override fun visitSpread(
        rex: RexSpread,
        ctx: Unit,
    ): Expr {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                code = ScribeProblem.UNSUPPORTED_PLAN_TO_AST_CONVERSION,
                message = "SPREAD is not yet supported",
            ),
        )
    }

    override fun visitTable(
        rex: RexTable,
        ctx: Unit,
    ): Expr {
        val tableName = rex.table.getName()
        val parts = tableName.getNamespace().getLevels() + tableName.getName()
        val global =
            transform.getGlobal(org.partiql.spi.catalog.Identifier.delimited(parts.toList()))
                ?: listener.reportAndThrow(
                    ScribeProblem.simpleError(
                        ScribeProblem.INVALID_PLAN,
                        "Malformed plan, resolved global (\$global ${rex.table.getName()}) does not exist",
                    ),
                )
        return exprVarRef(global, isQualified = false)
    }

    override fun visitVar(
        rex: RexVar,
        ctx: Unit,
    ): Expr {
        val scope = rex.scope // TODO currently unused
        val offset = rex.offset
        if (0 <= offset && offset < locals.aggregations.size) {
            return locals.aggregations[offset]
        }
        val binding =
            locals.env.getOrNull(offset) ?: listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.INVALID_PLAN, "Malformed plan, resolved local (\$var $offset) not in ${locals.dump()}",
                ),
            )
        val identifier = binder(binding.name)
        return exprVarRef(
            identifier = identifier,
            isQualified = false,
        )
    }

    // Private helpers
    private fun Locals.dump(): String {
        val pairs = this.env.joinToString { "${it.name}: ${it.type}" }
        return "< $pairs >"
    }

    private fun binder(symbol: String): Identifier = Identifier.delimited(symbol)
}
