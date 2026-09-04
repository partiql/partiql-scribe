package org.partiql.scribe.extensions.functions

import org.partiql.ast.Ast.exprBag
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprCast
import org.partiql.ast.Ast.exprInCollection
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprOperator
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.from
import org.partiql.ast.Ast.fromExpr
import org.partiql.ast.Ast.queryBodySFW
import org.partiql.ast.Ast.selectItemExpr
import org.partiql.ast.Ast.selectList
import org.partiql.ast.DataType
import org.partiql.ast.FromType
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprLit
import org.partiql.ast.expr.ExprPath
import org.partiql.ast.expr.PathStep
import org.partiql.extensions.functions.compat.datetime.DateAdd
import org.partiql.extensions.functions.compat.datetime.UtcNow
import org.partiql.extensions.functions.custom.collection.Contains
import org.partiql.extensions.functions.custom.conversion.HexToBigInt
import org.partiql.extensions.functions.custom.datetime.ToUnixTime
import org.partiql.extensions.functions.custom.math.Pow
import org.partiql.plan.RoutineRef
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.problems.ScribeProblem
import org.partiql.scribe.problems.ScribeProblemListener
import org.partiql.scribe.sql.SqlArgs
import org.partiql.scribe.sql.SqlCallFn
import org.partiql.scribe.sql.SqlCalls
import org.partiql.scribe.targets.redshift.RedshiftCalls
import org.partiql.scribe.targets.redshift.RedshiftTarget
import org.partiql.scribe.targets.spark.SparkCalls
import org.partiql.scribe.targets.spark.SparkTarget
import org.partiql.scribe.targets.trino.TrinoCalls
import org.partiql.scribe.targets.trino.TrinoTarget
import java.util.Locale

private val DATE_ADD = DateAdd.overloads().first().signature.name
private val UTCNOW = UtcNow.overloads().first().signature.name
private val CONTAINS = Contains.overloads().first().signature.name
private val HEX_TO_BIGINT = HexToBigInt.overloads().first().signature.name
private val TO_UNIXTIME = ToUnixTime.overloads().first().signature.name
private val POW = Pow.overloads().first().signature.name

public class SparkFunctionExtensionTarget(
    routines: Set<RoutineRef>,
) : SparkTarget() {
    private val routines: Set<RoutineRef> = routines.toSet()

    override fun getCalls(context: ScribeContext): SqlCalls = SparkFunctionExtensionCalls(context, routines)
}

public class TrinoFunctionExtensionTarget(
    routines: Set<RoutineRef>,
) : TrinoTarget() {
    private val routines: Set<RoutineRef> = routines.toSet()

    override fun getCalls(context: ScribeContext): SqlCalls = TrinoFunctionExtensionCalls(context, routines)
}

public class RedshiftFunctionExtensionTarget(
    routines: Set<RoutineRef>,
) : RedshiftTarget() {
    private val routines: Set<RoutineRef> = routines.toSet()

    override fun getCalls(context: ScribeContext): SqlCalls = RedshiftFunctionExtensionCalls(context, routines)
}

/**
 * Spark translations for explicitly bound PartiQL function extensions.
 *
 * The routines are exact resolved identities for functions from `partiql-function-extensions`.
 */
public open class SparkFunctionExtensionCalls(
    context: ScribeContext,
    routines: Set<RoutineRef>,
) : SparkCalls(context) {
    protected override val routineRules: Map<RoutineRef, SqlCallFn> =
        bind(
            routines,
            commonRules(this, context.getProblemListener()) +
                mapOf(
                    CONTAINS to { args ->
                        exprCall(Identifier.delimited("array_contains"), args.map { it.expr })
                    },
                    TO_UNIXTIME to { args ->
                        exprCast(
                            exprCall(Identifier.regular("unix_timestamp"), args.map { it.expr }),
                            DataType.BIGINT(),
                        )
                    },
                    HEX_TO_BIGINT to { args ->
                        exprCast(
                            exprCall(
                                Identifier.regular("conv"),
                                listOf(args[0].expr, intLiteral(16), intLiteral(10)),
                            ),
                            DataType.BIGINT(),
                        )
                    },
                ),
        )
}

/**
 * Trino translations for explicitly bound PartiQL function extensions.
 */
public open class TrinoFunctionExtensionCalls(
    context: ScribeContext,
    routines: Set<RoutineRef>,
) : TrinoCalls(context) {
    protected override val routineRules: Map<RoutineRef, SqlCallFn> =
        bind(
            routines,
            commonRules(this, context.getProblemListener()) +
                mapOf(
                    CONTAINS to { args ->
                        exprCall(Identifier.delimited("contains"), args.map { it.expr })
                    },
                    TO_UNIXTIME to { args ->
                        exprCast(
                            exprCall(Identifier.regular("to_unixtime"), args.map { it.expr }),
                            DataType.BIGINT(),
                        )
                    },
                    HEX_TO_BIGINT to { args ->
                        exprCall(
                            Identifier.regular("from_base"),
                            listOf(args[0].expr, intLiteral(16)),
                        )
                    },
                ),
        )
}

/**
 * Redshift translations for explicitly bound PartiQL function extensions.
 */
public open class RedshiftFunctionExtensionCalls(
    context: ScribeContext,
    routines: Set<RoutineRef>,
) : RedshiftCalls(context) {
    private val listener = context.getProblemListener()

    protected override val routineRules: Map<RoutineRef, SqlCallFn> =
        bind(
            routines,
            commonRules(this, listener) +
                mapOf(
                    CONTAINS to ::contains,
                    TO_UNIXTIME to { args ->
                        exprCast(
                            exprCall(
                                Identifier.regular("DATE_PART"),
                                listOf(exprVarRef(Identifier.regular("EPOCH"), false), args[0].expr),
                            ),
                            DataType.BIGINT(),
                        )
                    },
                    HEX_TO_BIGINT to { args ->
                        exprCall(
                            Identifier.regular("STRTOL"),
                            listOf(args[0].expr, intLiteral(16)),
                        )
                    },
                ),
        )

    private fun contains(args: SqlArgs): Expr {
        val path = args.getOrNull(0)?.expr as? ExprPath
        val element = args.getOrNull(1)?.expr as? ExprLit
        if (args.size != 2 || path == null || element == null) {
            listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.UNSUPPORTED_OPERATION,
                    "Redshift `contains` requires an array path and a literal element",
                ),
            )
        }
        val alias =
            when (val step = path.steps.lastOrNull()) {
                is PathStep.Field -> step.field.text
                is PathStep.Element -> (step.element as? ExprLit)?.lit?.stringValue()
                else -> null
            } ?: listener.reportAndThrow(
                ScribeProblem.simpleError(
                    ScribeProblem.UNSUPPORTED_OPERATION,
                    "Redshift `contains` requires an array path ending in a named field",
                ),
            )
        val variable = exprVarRef(Identifier.regular(alias), false)
        val query =
            exprQuerySet(
                queryBodySFW(
                    select =
                        selectList(
                            listOf(
                                selectItemExpr(
                                    exprCall(Identifier.regular("COUNT"), listOf(variable)),
                                ),
                            ),
                        ),
                    from =
                        from(
                            listOf(
                                fromExpr(
                                    path,
                                    FromType.SCAN(),
                                    variable.identifier.identifier,
                                ),
                            ),
                        ),
                    where = exprInCollection(variable, exprBag(listOf(element)), false),
                ),
            )
        return exprOperator("<=", intLiteral(1), query)
    }
}

private fun commonRules(
    calls: SqlCalls,
    listener: ScribeProblemListener,
): Map<String, SqlCallFn> =
    mapOf(
        DATE_ADD to { args -> dateAdd(calls, listener, args) },
        UTCNOW to { args -> calls.retarget(UTCNOW, args) },
        POW to { args -> exprCall(Identifier.delimited(POW), args.map { it.expr }) },
    )

private fun bind(
    routines: Set<RoutineRef>,
    translations: Map<String, SqlCallFn>,
): Map<RoutineRef, SqlCallFn> =
    routines.associateWith { routineRef ->
        val name = routineRef.name.getName()
        requireNotNull(translations[name]) { "Unsupported PartiQL function extension `$name`" }
    }

private fun dateAdd(
    calls: SqlCalls,
    listener: ScribeProblemListener,
    args: SqlArgs,
): Expr {
    val part = (args.firstOrNull()?.expr as? ExprLit)?.lit
    if (args.size != 3 || part?.code() != Literal.STRING) {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                ScribeProblem.UNSUPPORTED_OPERATION,
                "Function extension `date_add` requires a string datetime part, quantity, and timestamp",
            ),
        )
    }
    val normalizedPart = part.stringValue().lowercase(Locale.ROOT)
    if (normalizedPart !in setOf("year", "month", "day", "hour", "minute", "second")) {
        listener.reportAndThrow(
            ScribeProblem.simpleError(
                ScribeProblem.UNSUPPORTED_OPERATION,
                "Unsupported datetime part for function extension `date_add`: ${part.stringValue()}",
            ),
        )
    }
    return calls.retarget("date_add_$normalizedPart", args.drop(1))
}

private fun intLiteral(value: Int): Expr = exprLit(Literal.intNum(value.toLong()))
