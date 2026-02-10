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
import org.partiql.ast.DatetimeField
import org.partiql.ast.Identifier
import org.partiql.ast.Identifier.Simple.regular
import org.partiql.ast.IntervalQualifier
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
import org.partiql.scribe.problems.ScribeProblemListener
import org.partiql.spi.types.IntervalCode
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField
import org.partiql.spi.value.Datum
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.absoluteValue

public typealias TypeEnv = List<PTypeField>

public class Locals(
    public val env: TypeEnv,
    public val aggregations: List<Expr> = emptyList(),
    public val windowFunctions: List<Expr> = emptyList(),
) {
    private var aggFuncOffset: Int = -1
    private var windowFuncOffset: Int = -1

    init {
        windowFuncOffset = env.indexOfFirst { it.name.startsWith("\$window_func_") }
    }

    public fun getExprOrNull(offset: Int): Expr? {
        // Handle aggregation first as RelAggregate creates a new schema with aggregations plus group keys
        if (aggregations.isNotEmpty()) {
            return aggregations.getOrNull(offset)
        }

        val binding = env.getOrNull(offset)

        return if (binding != null) {
            if (binding.name.startsWith("\$window_func_")) {
                windowFunctions.getOrNull(offset - windowFuncOffset)
            } else {
                return exprVarRef(
                    identifier = binder(binding.name),
                    isQualified = false,
                )
            }
        } else {
            null
        }
    }

    private fun binder(symbol: String): Identifier = Identifier.delimited(symbol)

    public companion object {
        public val EMPTY: Locals = Locals(env = emptyList(), aggregations = emptyList(), windowFunctions = emptyList())
    }
}

private const val UNSPECIFIED_LENGTH = "UNSPECIFIED_LENGTH"
private const val UNSPECIFIED_PRECISION = "UNSPECIFIED_PRECISION"
private const val UNSPECIFIED_SCALE = "UNSPECIFIED_SCALE"
private const val UNSPECIFIED_FRACTIONAL_PRECISION = "UNSPECIFIED_FRACTIONAL_PRECISION"

internal const val MONTHS_PER_YEAR = 12L
internal const val SECONDS_PER_MINUTE = 60L
internal const val MINUTES_PER_HOUR = 60L
internal const val HOURS_PER_DAY = 24L
internal const val SECONDS_PER_HOUR = SECONDS_PER_MINUTE * MINUTES_PER_HOUR
internal const val SECONDS_PER_DAY = SECONDS_PER_HOUR * HOURS_PER_DAY
internal const val NANOS_PER_SECOND = 1_000_000_000L

public open class RexConverter(
    private val transform: PlanToAst,
    private val locals: Locals,
    private val context: ScribeContext,
) : OperatorVisitor<Expr, Unit> {
    internal val listener: ScribeProblemListener = context.getProblemListener()

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

    public open fun visitRex(
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

    internal fun PType.unspecifiedLength(): Boolean = metas[UNSPECIFIED_LENGTH] == true

    internal fun PType.unspecifiedPrecision(): Boolean = metas[UNSPECIFIED_PRECISION] == true

    internal fun PType.unspecifiedScale(): Boolean = metas[UNSPECIFIED_SCALE] == true

    internal fun PType.unspecifiedFractionalPrecision(): Boolean = metas[UNSPECIFIED_FRACTIONAL_PRECISION] == true

    internal fun PType.toDataType(): DataType? {
        val pType = this
        return when (pType.code()) {
            // BOOL type
            PType.BOOL -> DataType.BOOL()
            // INTEGER types
            PType.TINYINT -> DataType.TINYINT()
            PType.SMALLINT -> DataType.SMALLINT()
            PType.INTEGER -> DataType.INT()
            PType.BIGINT -> DataType.BIGINT()
            // DECIMAL types
            PType.NUMERIC -> {
                val noPrecision = pType.unspecifiedPrecision()
                val noScale = pType.unspecifiedScale()
                when {
                    noPrecision && noScale -> DataType.NUMERIC()
                    noScale -> DataType.NUMERIC(pType.precision)
                    noPrecision -> error("Invalid PType in plan $pType has a scale but no precision specified")
                    else -> DataType.NUMERIC(pType.precision, pType.scale)
                }
            }
            PType.DECIMAL -> {
                val noPrecision = pType.unspecifiedPrecision()
                val noScale = pType.unspecifiedScale()
                when {
                    noPrecision && noScale -> DataType.DECIMAL()
                    noScale -> DataType.DECIMAL(pType.precision)
                    noPrecision -> error("Invalid PType in plan $pType has a scale but no precision specified")
                    else -> DataType.DECIMAL(pType.precision, pType.scale)
                }
            }
            // Approximate numeric types
            PType.REAL -> DataType.REAL()
            PType.DOUBLE -> DataType.DOUBLE_PRECISION()
            // String types
            PType.CHAR ->
                when (pType.unspecifiedLength()) {
                    true -> DataType.CHAR()
                    false -> DataType.CHAR(pType.length)
                }
            PType.VARCHAR ->
                when (pType.unspecifiedLength()) {
                    true -> DataType.VARCHAR()
                    false -> DataType.VARCHAR(pType.length)
                }
            PType.STRING -> DataType.STRING()
            // Datetime types
            PType.DATE -> DataType.DATE()
            PType.TIME ->
                when (pType.unspecifiedPrecision()) {
                    true -> DataType.TIME()
                    false -> DataType.TIME(pType.precision)
                }
            PType.TIMEZ ->
                when (pType.unspecifiedPrecision()) {
                    true -> DataType.TIME_WITH_TIME_ZONE()
                    false -> DataType.TIME_WITH_TIME_ZONE(pType.precision)
                }
            PType.TIMESTAMP ->
                when (pType.unspecifiedPrecision()) {
                    true -> DataType.TIMESTAMP()
                    false -> DataType.TIMESTAMP(pType.precision)
                }
            PType.TIMESTAMPZ ->
                when (pType.unspecifiedPrecision()) {
                    true -> DataType.TIMESTAMP_WITH_TIME_ZONE()
                    false -> DataType.TIMESTAMP_WITH_TIME_ZONE(pType.precision)
                }
            PType.INTERVAL_YM -> {
                when (pType.intervalCode) {
                    IntervalCode.YEAR -> {
                        DataType.INTERVAL(
                            IntervalQualifier.Single(
                                DatetimeField.YEAR(),
                                pType.retrievePrecision(),
                                null,
                            ),
                        )
                    }
                    IntervalCode.MONTH -> {
                        DataType.INTERVAL(
                            IntervalQualifier.Single(
                                DatetimeField.MONTH(),
                                pType.retrievePrecision(),
                                null,
                            ),
                        )
                    }
                    IntervalCode.YEAR_MONTH -> {
                        DataType.INTERVAL(
                            IntervalQualifier.Range(
                                DatetimeField.YEAR(),
                                pType.retrievePrecision(),
                                DatetimeField.MONTH(),
                                null,
                            ),
                        )
                    }
                    else ->
                        listener.reportAndThrow(
                            ScribeProblem.simpleError(
                                code = ScribeProblem.INVALID_PLAN,
                                message = "Invalid IntervalCode for INTERVAL_YM value: ${pType.intervalCode}",
                            ),
                        )
                }
            }
            PType.INTERVAL_DT -> {
                when (pType.intervalCode) {
                    IntervalCode.DAY -> {
                        DataType.INTERVAL(
                            IntervalQualifier.Single(
                                DatetimeField.DAY(),
                                pType.retrievePrecision(),
                                null,
                            ),
                        )
                    }
                    IntervalCode.HOUR -> {
                        DataType.INTERVAL(
                            IntervalQualifier.Single(
                                DatetimeField.HOUR(),
                                pType.retrievePrecision(),
                                null,
                            ),
                        )
                    }
                    IntervalCode.MINUTE -> {
                        DataType.INTERVAL(
                            IntervalQualifier.Single(
                                DatetimeField.MINUTE(),
                                pType.retrievePrecision(),
                                null,
                            ),
                        )
                    }
                    IntervalCode.SECOND -> {
                        val fracPrecision =
                            if (pType.unspecifiedFractionalPrecision()) {
                                null
                            } else {
                                pType.fractionalPrecision
                            }
                        DataType.INTERVAL(
                            IntervalQualifier.Single(
                                DatetimeField.SECOND(),
                                pType.retrievePrecision(),
                                fracPrecision,
                            ),
                        )
                    }
                    IntervalCode.DAY_HOUR -> {
                        DataType.INTERVAL(
                            IntervalQualifier.Range(
                                DatetimeField.DAY(),
                                pType.retrievePrecision(),
                                DatetimeField.HOUR(),
                                null,
                            ),
                        )
                    }
                    IntervalCode.DAY_MINUTE -> {
                        DataType.INTERVAL(
                            IntervalQualifier.Range(
                                DatetimeField.DAY(),
                                pType.retrievePrecision(),
                                DatetimeField.MINUTE(),
                                null,
                            ),
                        )
                    }
                    IntervalCode.DAY_SECOND -> {
                        val fracPrecision =
                            if (pType.unspecifiedFractionalPrecision()) {
                                null
                            } else {
                                pType.fractionalPrecision
                            }
                        DataType.INTERVAL(
                            IntervalQualifier.Range(
                                DatetimeField.DAY(),
                                pType.retrievePrecision(),
                                DatetimeField.SECOND(),
                                fracPrecision,
                            ),
                        )
                    }
                    IntervalCode.HOUR_MINUTE -> {
                        DataType.INTERVAL(
                            IntervalQualifier.Range(
                                DatetimeField.HOUR(),
                                pType.retrievePrecision(),
                                DatetimeField.MINUTE(),
                                null,
                            ),
                        )
                    }
                    IntervalCode.HOUR_SECOND -> {
                        val fracPrecision =
                            if (pType.unspecifiedFractionalPrecision()) {
                                null
                            } else {
                                pType.fractionalPrecision
                            }
                        DataType.INTERVAL(
                            IntervalQualifier.Range(
                                DatetimeField.HOUR(),
                                pType.retrievePrecision(),
                                DatetimeField.SECOND(),
                                fracPrecision,
                            ),
                        )
                    }
                    IntervalCode.MINUTE_SECOND -> {
                        val fracPrecision =
                            if (pType.unspecifiedFractionalPrecision()) {
                                null
                            } else {
                                pType.fractionalPrecision
                            }
                        DataType.INTERVAL(
                            IntervalQualifier.Range(
                                DatetimeField.MINUTE(),
                                pType.retrievePrecision(),
                                DatetimeField.SECOND(),
                                fracPrecision,
                            ),
                        )
                    }
                    else ->
                        listener.reportAndThrow(
                            ScribeProblem.simpleError(
                                code = ScribeProblem.INVALID_PLAN,
                                message = "Invalid IntervalCode for INTERVAL_DT value: ${pType.intervalCode}",
                            ),
                        )
                }
            }
            else -> null
        }
    }

    override fun visitCast(
        rex: RexCast,
        ctx: Unit,
    ): Expr {
        val value = visitRex(rex.operand, ctx)
        val targetType = rex.target.toDataType()
        if (targetType == null) {
            when (rex.target.code()) {
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

    private fun PType.retrievePrecision() =
        if (this.unspecifiedPrecision()) {
            null
        } else {
            this.precision
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
            PType.INTERVAL_YM -> {
                val dataType =
                    type.toDataType() ?: listener.reportAndThrow(
                        ScribeProblem.simpleError(
                            code = ScribeProblem.INVALID_PLAN,
                            message = "Cannot convert $type to a DataType",
                        ),
                    )
                when (type.intervalCode) {
                    IntervalCode.YEAR -> {
                        Literal.typedString(
                            dataType,
                            "$years",
                        )
                    }
                    IntervalCode.MONTH -> {
                        Literal.typedString(
                            dataType,
                            "$totalMonths",
                        )
                    }
                    IntervalCode.YEAR_MONTH -> {
                        Literal.typedString(
                            dataType,
                            "$years-${months.absoluteValue}",
                        )
                    }
                    else ->
                        listener.reportAndThrow(
                            ScribeProblem.simpleError(
                                code = ScribeProblem.INVALID_PLAN,
                                message = "Invalid IntervalCode for INTERVAL_YM value: ${type.intervalCode}",
                            ),
                        )
                }
            }
            PType.INTERVAL_DT -> {
                val dataType =
                    type.toDataType() ?: listener.reportAndThrow(
                        ScribeProblem.simpleError(
                            code = ScribeProblem.INVALID_PLAN,
                            message = "Cannot convert $type to a DataType",
                        ),
                    )
                when (type.intervalCode) {
                    IntervalCode.DAY -> {
                        Literal.typedString(
                            dataType,
                            "$days",
                        )
                    }
                    IntervalCode.HOUR -> {
                        Literal.typedString(
                            dataType,
                            "${totalSeconds / SECONDS_PER_HOUR}",
                        )
                    }
                    IntervalCode.MINUTE -> {
                        Literal.typedString(
                            dataType,
                            "${totalSeconds / SECONDS_PER_MINUTE}",
                        )
                    }
                    IntervalCode.SECOND -> {
                        val intervalValue = "$totalSeconds${getNanosPart(type, nanos)}"
                        Literal.typedString(
                            dataType,
                            intervalValue,
                        )
                    }
                    IntervalCode.DAY_HOUR -> {
                        Literal.typedString(
                            dataType,
                            "$days ${hours.absoluteValue}",
                        )
                    }
                    IntervalCode.DAY_MINUTE -> {
                        Literal.typedString(
                            dataType,
                            "$days ${hours.absoluteValue}:${minutes.absoluteValue}",
                        )
                    }
                    IntervalCode.DAY_SECOND -> {
                        val intervalValue =
                            "$days ${hours.absoluteValue}:${minutes.absoluteValue}:" +
                                "${seconds.absoluteValue}${getNanosPart(type, nanos)}"
                        Literal.typedString(
                            dataType,
                            intervalValue,
                        )
                    }
                    IntervalCode.HOUR_MINUTE -> {
                        Literal.typedString(
                            dataType,
                            "${totalSeconds / SECONDS_PER_HOUR}:${minutes.absoluteValue}",
                        )
                    }
                    IntervalCode.HOUR_SECOND -> {
                        val intervalValue =
                            "${totalSeconds / SECONDS_PER_HOUR}:${minutes.absoluteValue}:" +
                                "${seconds.absoluteValue}${getNanosPart(type, nanos)}"
                        Literal.typedString(
                            dataType,
                            intervalValue,
                        )
                    }
                    IntervalCode.MINUTE_SECOND -> {
                        val intervalValue =
                            "${totalSeconds / SECONDS_PER_MINUTE}:" +
                                "${seconds.absoluteValue}${getNanosPart(type, nanos)}"
                        Literal.typedString(
                            dataType,
                            intervalValue,
                        )
                    }
                    else ->
                        listener.reportAndThrow(
                            ScribeProblem.simpleError(
                                code = ScribeProblem.INVALID_PLAN,
                                message = "Invalid IntervalCode for INTERVAL_DT value: ${type.intervalCode}",
                            ),
                        )
                }
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
        val relConverter = transform.getRelConverter()
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
        // The planner does not support SubqueryComp, SubqueryIn, SubqueryTest planner node yet. Thus
        // corresponding subquery function are not getting triggered.
        // For `IN` and `EXISTS`, it was planned as RexSelect Node and get handled by visitSelect
        // For comparison operators, it was planned as RexSubquery and get handled by visitSubquery
        val transform = transform
        val relConverter = transform.getRelConverter()
        return relConverter.apply(rex.input, ctx).toExprQuerySet()
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

        return locals.getExprOrNull(offset) ?: listener.reportAndThrow(
            ScribeProblem.simpleError(
                ScribeProblem.INVALID_PLAN, "Malformed plan, resolved local (\$var $offset) not in ${locals.dump()}",
            ),
        )
    }

    internal fun getNanosPart(
        type: PType,
        nanos: Int,
    ): String {
        val fracPrecision =
            if (type.unspecifiedFractionalPrecision()) {
                null
            } else {
                type.fractionalPrecision
            }
        var nanosStr = ""
        if (fracPrecision != null && fracPrecision > 0) {
            nanosStr = String.format("%09d", nanos.absoluteValue)
            nanosStr = "." + nanosStr.substring(0, fracPrecision)
        }

        return nanosStr
    }

    // Private helpers
    private fun Locals.dump(): String {
        val pairs = this.env.joinToString { "${it.name}: ${it.type}" }
        val aggCount = this.aggregations.size
        val winCount = this.windowFunctions.size
        return "< $pairs > (agg: $aggCount, win: $winCount)"
    }
}
