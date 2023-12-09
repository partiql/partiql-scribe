package org.partiql.scribe.sql

import org.partiql.ast.Expr
import org.partiql.ast.Identifier
import org.partiql.ast.Select
import org.partiql.ast.SetQuantifier
import org.partiql.ast.exprCall
import org.partiql.ast.exprCase
import org.partiql.ast.exprCaseBranch
import org.partiql.ast.exprLit
import org.partiql.ast.exprPath
import org.partiql.ast.exprPathStepIndex
import org.partiql.ast.exprPathStepSymbol
import org.partiql.ast.exprPathStepUnpivot
import org.partiql.ast.exprPathStepWildcard
import org.partiql.ast.exprStruct
import org.partiql.ast.exprStructField
import org.partiql.ast.exprVar
import org.partiql.ast.identifierSymbol
import org.partiql.ast.selectProject
import org.partiql.ast.selectProjectItemAll
import org.partiql.ast.selectProjectItemExpression
import org.partiql.ast.selectValue
import org.partiql.plan.Fn
import org.partiql.plan.PlanNode
import org.partiql.plan.Rel
import org.partiql.plan.Rex
import org.partiql.plan.visitor.PlanBaseVisitor
import org.partiql.scribe.ScribeProblem
import org.partiql.scribe.sql.SqlTransform.Companion.translate
import org.partiql.types.AnyOfType
import org.partiql.types.AnyType
import org.partiql.types.BagType
import org.partiql.types.BlobType
import org.partiql.types.BoolType
import org.partiql.types.ClobType
import org.partiql.types.DateType
import org.partiql.types.DecimalType
import org.partiql.types.FloatType
import org.partiql.types.GraphType
import org.partiql.types.IntType
import org.partiql.types.ListType
import org.partiql.types.MissingType
import org.partiql.types.NullType
import org.partiql.types.SexpType
import org.partiql.types.StaticType
import org.partiql.types.StringType
import org.partiql.types.StructType
import org.partiql.types.SymbolType
import org.partiql.types.TimeType
import org.partiql.types.TimestampType
import org.partiql.types.TupleConstraint
import org.partiql.value.PartiQLValueExperimental
import org.partiql.value.StringValue

/**
 * Local scope.
 */
public typealias TypeEnv = List<Rel.Binding>

/**
 * Locals, checking projections before then TypeEnv. This needs to be more deeply thought about.
 */
public class Locals(
    val env: TypeEnv,
    val projections: List<Expr> = emptyList(),
)

/**
 * RexToSql translates a [Rex] tree in the given local scope.
 */
public open class RexToSql(
    private val transform: SqlTransform,
    private val locals: Locals,
) : PlanBaseVisitor<Expr, StaticType>() {

    /**
     * Convert a [Rex] to an [Expr].
     */
    public fun apply(rex: Rex): Expr = rex.accept(this, StaticType.ANY)

    /**
     * Default behavior is considered unsupported.
     */
    override fun defaultReturn(node: PlanNode, ctx: StaticType): Expr =
        throw UnsupportedOperationException("Cannot translate rex $node")

    override fun defaultVisit(node: PlanNode, ctx: StaticType) = defaultReturn(node, ctx)

    /**
     * Pass along the Rex [StaticType]
     */
    override fun visitRex(node: Rex, ctx: StaticType) = super.visitRexOp(node.op, node.type)

    @OptIn(PartiQLValueExperimental::class)
    override fun visitRexOpLit(node: Rex.Op.Lit, ctx: StaticType): Expr {
        return exprLit(node.value)
    }

    override fun visitRexOpTupleUnion(node: Rex.Op.TupleUnion, ctx: StaticType): Expr {
        val args = node.args.map { arg -> visitRex(arg, ctx) }
        return exprCall(
            identifierSymbol("TUPLEUNION", Identifier.CaseSensitivity.INSENSITIVE),
            args = args
        )
    }

    override fun visitRexOpVar(node: Rex.Op.Var, ctx: StaticType): Expr {
        if (0 <= node.ref && node.ref < locals.projections.size) {
            return locals.projections[node.ref]
        }
        val binding = locals.env.getOrNull(node.ref)
        if (binding == null) {
            error("Malformed plan, resolved local (\$var ${node.ref}) not in ${locals.dump()}")
        }
        val identifier = binder(binding.name)
        val scope = Expr.Var.Scope.DEFAULT
        return exprVar(identifier, scope)
    }

    override fun visitRexOpGlobal(node: Rex.Op.Global, ctx: StaticType): Expr {
        val global = transform.getGlobal(node.ref)
        if (global == null) {
            error("Malformed plan, resolved global (\$global ${node.ref}) does not exist")
        }
        val scope = Expr.Var.Scope.DEFAULT
        return exprVar(global, scope)
    }

    override fun visitRexOpPath(node: Rex.Op.Path, ctx: StaticType): Expr {
        val root = visitRex(node.root, StaticType.ANY)
        val steps = node.steps.map { visitRexOpPathStep(it) }
        return exprPath(root, steps)
    }

    private fun visitRexOpPathStep(node: Rex.Op.Path.Step): Expr.Path.Step = when (node) {
        is Rex.Op.Path.Step.Index -> visitRexOpPathStepIndex(node)
        is Rex.Op.Path.Step.Symbol -> visitRexOpPathStepSymbol(node)
        is Rex.Op.Path.Step.Key -> visitRexOpPathStepKey(node)
        is Rex.Op.Path.Step.Unpivot -> exprPathStepUnpivot()
        is Rex.Op.Path.Step.Wildcard -> exprPathStepWildcard()
    }

    private fun visitRexOpPathStepIndex(node: Rex.Op.Path.Step.Index): Expr.Path.Step {
        val k = visitRex(node.key, StaticType.ANY)
        return exprPathStepIndex(k)
    }

    private fun visitRexOpPathStepSymbol(node: Rex.Op.Path.Step.Symbol): Expr.Path.Step {
        val symbol = id(node.key)
        return exprPathStepSymbol(symbol)
    }

    private fun visitRexOpPathStepKey(node: Rex.Op.Path.Step.Key) : Expr.Path.Step {
        return exprPathStepIndex(visitRex(node.key, StaticType.STRING))
    }

    override fun visitRexOpCollection(node: Rex.Op.Collection, ctx: StaticType): Expr {
        val values = node.values.map { visitRex(it, ctx) }
        return when (ctx) {
            is ListType -> Expr.Collection(Expr.Collection.Type.LIST, values)
            is SexpType -> Expr.Collection(Expr.Collection.Type.SEXP, values)
            is BagType -> Expr.Collection(Expr.Collection.Type.BAG, values)
            else -> throw UnsupportedOperationException("unsupported collection type $ctx")
        }
    }

    override fun visitRexOpStruct(node: Rex.Op.Struct, ctx: StaticType): Expr {
        val fields = node.fields.map {
            exprStructField(
                name = visitRex(it.k, StaticType.ANY),
                value = visitRex(it.v, StaticType.ANY),
            )
        }
        return exprStruct(fields)
    }

    override fun visitRexOpCase(node: Rex.Op.Case, ctx: StaticType): Expr {
        val default = node.default.let { visitRex(it, it.type) }
        val branches = node.branches.map {
            val condition = visitRex(it.condition, StaticType.ANY)
            val result = visitRex(it.rex, StaticType.ANY)
            exprCaseBranch(condition, result)
        }
        return when (branches.isEmpty()) {
            true -> default
            false -> exprCase(expr = null, branches = branches, default = default)
        }
    }

    override fun visitRexOpCall(node: Rex.Op.Call, ctx: StaticType): Expr {

        val (name, args) = when (node) {
            is Rex.Op.Call.Static -> {
                val name = node.fn.signature.name
                val args = node.args.map { SqlArg(visitRex(it, StaticType.ANY), it.type) }
                name to args
            }
            is Rex.Op.Call.Dynamic -> {
                val name = node.candidates.first().fn.signature.name
                val args = node.args.map { SqlArg(visitRex(it, StaticType.ANY), it.type) }
                name to args
            }
        }

        return transform.getFunction(name, args)
    }

    override fun visitRexOpSelect(node: Rex.Op.Select, ctx: StaticType): Expr {
        val typeEnv = node.rel.type.schema
        val relToSql = RelToSql(transform)
        val rexToSql = RexToSql(transform, locals)
        val sfw = relToSql.apply(node.rel)
        assert(sfw.select != null) { "SELECT from RelToSql should never be null" }
        val setq = getSetQuantifier(sfw.select!!)
        val select = convertSelectValueToSqlSelect(sfw.select, node.constructor, node.rel, setq)
            ?: convertSelectValue(node.constructor, node.rel, setq)
            ?: selectValue(rexToSql.apply(node.constructor), setq)
        sfw.select = select
        return sfw.build()
    }

    override fun visitRexOpSubquery(node: Rex.Op.Subquery, ctx: StaticType): Expr {
        return visitRexOpSelect(node.select, StaticType.ANY)
    }

    /**
     * Grabs the [SetQuantifier] of a [Select].
     */
    private fun getSetQuantifier(select: Select): SetQuantifier? = when (select) {
        is Select.Project -> select.setq
        is Select.Value -> select.setq
        is Select.Star -> select.setq
        is Select.Pivot -> null
    }

    /**
     * Attempts to convert a SELECT VALUE to a SELECT LIST. Returns NULL if unable.
     *
     * For SELECT VALUE <v> FROM queries, the <v> gets pushed into the PROJECT, and it gets replaced with a variable
     * reference to the single projection. Therefore, if there was a SELECT * (which gets converted into a SELECT VALUE TUPLEUNION),
     * then the TUPLEUNION will be placed in the [Rel.Op.Project], and the [Rex.Op.Select.constructor] will be a
     * [Rex.Op.Var.Resolved] referencing the single projection. With that in mind, we can reconstruct the AST by looking
     * at the [constructor]. If it's a [Rex.Op.Var.Resolved], and it references a [Rex.Op.Struct], we can pull
     * out those struct's attributes and create a SQL-style select.
     *
     * NOTE: [Rex.Op.TupleUnion]'s get constant folded in the [org.partiql.planner.typer.PlanTyper].
     *
     * Example:
     * ```
     * SELECT t.* FROM t
     * -- Gets converted into
     * SELECT VALUE TUPLEUNION({ 'a': <INT>, 'b': <DECIMAL> }) FROM t
     * -- Gets constant folded (in the PlanTyper) into:
     * SELECT VALUE { 'a': <INT>, 'b': <DECIMAL> } FROM t
     * -- Gets converted into:
     * SELECT VALUE $__x
     * -> PROJECT < $__x: TUPLEUNION(...) >
     * -> SCAN t
     * -- Gets converted into:
     * SELECT a, b, c FROM t
     * -- Equivalent
     * SELECT "t".a, "t.b, "t".c FROM t AS "t"
     * ```
     *
     * If unable to convert into SQL-style projections (due to open content structs, non-struct arguments, etc), we
     * return null.
     */
    @OptIn(PartiQLValueExperimental::class)
    private fun convertSelectValueToSqlSelect(curr: Select?, constructor: Rex, input: Rel, setq: SetQuantifier?): Select? {
        val relProject = input.op as? Rel.Op.Project ?: return null
        val structOp = getConstructorFromProjection(constructor, relProject)?.op as? Rex.Op.Struct ?: return null
        val newRexToSql = RexToSql(transform, Locals(relProject.input.type.schema))
        val type = constructor.type as? StructType ?: return null
        if (type.constraints.contains(TupleConstraint.Open(false))
                .not() || type.constraints.contains(TupleConstraint.Ordered).not()
        ) {
            return null
        }

        // AGG HACK; this is terrible!
        val projections = if (curr != null && curr is Select.Project) {
            val first = curr.items.first() as Select.Project.Item.Expression
            val struct = first.expr as Expr.Struct
            struct.fields.map {
                val expr = it.value
                val asAlias = binder(((it.name as Expr.Lit).value as StringValue).value ?: "")
                selectProjectItemExpression(expr, asAlias)
            }
        } else {
            structOp.fields.map { field ->
                val key = field.k.op
                if (key !is Rex.Op.Lit || key.value !is StringValue) {
                    return null
                }
                val fieldName = (key.value as StringValue).value ?: return null
                //
                val expr = newRexToSql.apply(field.v)
                val asAlias = binder(fieldName)
                selectProjectItemExpression(expr, asAlias)
            }
        }

        return selectProject(
            items = projections,
            setq = setq
        )
    }

    /**
     * Since the <v> in SELECT VALUE <v> gets pulled into the [Rel.Op.Project], we attempt to recuperate the [Rex] and
     * convert it to SQL. If we are unable to, return NULL.
     */
    private fun convertSelectValue(constructor: Rex, input: Rel, setq: SetQuantifier?): Select? {
        val relProject = input.op as? Rel.Op.Project ?: return null
        val projection = getConstructorFromProjection(constructor, relProject) ?: return null
        val rexToSql = RexToSql(transform, Locals(relProject.input.type.schema))
        return when (val op = projection.op) {
            is Rex.Op.TupleUnion -> {
                val items = op.args.map {
                    getProjectionItemFromSingleItemStruct(it, rexToSql) ?: selectProjectItemAll(rexToSql.apply(it))
                }
                selectProject(items, setq)
            }
            else -> selectValue(rexToSql.apply(projection), setq)
        }
    }

    @OptIn(PartiQLValueExperimental::class)
    private fun getProjectionItemFromSingleItemStruct(rex: Rex, rexToSql: RexToSql): Select.Project.Item? {
        val op = rex.op as? Rex.Op.Struct ?: return null
        if (op.fields.size != 1) { return null }
        val key = op.fields[0].k.op
        if (key !is Rex.Op.Lit || key.value !is StringValue) { return null }
        val fieldName = (key.value as StringValue).value ?: return null
        //
        val expr = rexToSql.apply(op.fields[0].v)
        val asAlias = binder(fieldName)
        return selectProjectItemExpression(expr, asAlias)
    }

    /**
     * Grabs the first projection from [Rel.Op.Project] if the [constructor] is referencing it. If unable to
     * grab, return null.
     */
    private fun getConstructorFromProjection(constructor: Rex, relProject: Rel.Op.Project): Rex? {
        val constructorOp = constructor.op as? Rex.Op.Var ?: return null
        if (constructorOp.ref != 0) { return null }
        if (relProject.projections.size != 1) { return null }
        return relProject.projections[0]
    }

    private fun id(symbol: String): Identifier.Symbol = identifierSymbol(
        symbol = symbol,
        caseSensitivity = Identifier.CaseSensitivity.INSENSITIVE,
    )

    private fun binder(symbol: String): Identifier.Symbol = identifierSymbol(
        symbol = symbol,
        caseSensitivity = Identifier.CaseSensitivity.SENSITIVE,
    )

    private fun Locals.dump(): String {
        val pairs = this.env.joinToString { "${it.name}: ${it.type}" }
        return "< $pairs >"
    }

    internal fun Identifier.sql(): String = when (this) {
        is Identifier.Qualified -> (listOf(this.root.sql()) + this.steps.map { it.sql() }).joinToString(".")
        is Identifier.Symbol -> when (this.caseSensitivity) {
            Identifier.CaseSensitivity.SENSITIVE -> "\"$symbol\""
            Identifier.CaseSensitivity.INSENSITIVE -> symbol
        }
    }
}
