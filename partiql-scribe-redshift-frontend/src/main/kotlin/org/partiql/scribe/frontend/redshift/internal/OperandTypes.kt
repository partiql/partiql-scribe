package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.Ast.exprPath
import org.partiql.ast.Ast.exprPathStepField
import org.partiql.ast.Ast.exprQuerySet
import org.partiql.ast.Ast.exprVarRef
import org.partiql.ast.Ast.from
import org.partiql.ast.Ast.fromExpr
import org.partiql.ast.Ast.query
import org.partiql.ast.Ast.queryBodySFW
import org.partiql.ast.Ast.selectValue
import org.partiql.ast.FromType
import org.partiql.ast.AstNode
import org.partiql.ast.AstRewriter
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.SetQuantifier
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprLit
import org.partiql.plan.Operator
import org.partiql.plan.OperatorRewriter
import org.partiql.plan.Plan
import org.partiql.plan.rel.RelAggregate
import org.partiql.plan.rel.RelSort
import org.partiql.plan.rel.RelWindow
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexLit
import org.partiql.planner.PartiQLPlanner
import org.partiql.spi.Context
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Catalogs
import org.partiql.spi.catalog.Namespace
import org.partiql.spi.catalog.Path
import org.partiql.spi.catalog.Session
import org.partiql.spi.function.Agg
import org.partiql.spi.function.AggOverload
import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorListener
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.types.PType
import org.partiql.spi.types.PTypeField
import org.partiql.spi.catalog.Table
import org.partiql.spi.value.Datum

/**
 * The static types of Redshift operands, for the rules whose translation depends on them.
 *
 * Some Redshift constructs have different semantics for different argument types. For example, `LEN` of a CHAR(n)
 * value does not count trailing blanks, and `AVG` of an integer argument truncates the result. The AST builder gets
 * the types from the PartiQL planner:
 *
 *  1. In a probe pass, a rule whose translation depends on its operand types translates to a call of its [Typing] in
 *     the Redshift catalog (see [resolve]): a hidden function whose arguments are the operands and whose result type is
 *     the Redshift result type of the rule for the argument types. The planner plans the AST against the session and
 *     the Redshift catalog (see [probeSession]), so the plan is typed as Redshift types the query, and [collect] reads
 *     the type of each operand from the plan.
 *  2. A rule without a typing wraps each operand in a probe call instead (see [operand]), which returns its argument
 *     with the argument's type, and translates to a placeholder of the dynamic type while its operand types are not
 *     known (see [pending]). Such a rule in an operand of another rule costs one more pass: steps 1 and 2 repeat until
 *     the types do not change.
 *  3. The AST builder translates the query without probes, with the final types.
 *
 * An operand is identified by its translation, without probe calls, and the query block that contains it. Two operands
 * with the same translation in one query block have the same probe, so that a select item and the GROUP BY key that it
 * repeats (`UPPER(c)` and `UPPER(t.c)`) stay equal in a probe pass. When a placeholder in an operand is resolved, the
 * translation of the operand changes, and the next pass probes it again.
 */
internal class OperandTypes {
    private val ids = HashMap<Key, Int>()
    private var nextId = 0
    private val types = HashMap<Int, PType>()
    private val scopes = ArrayDeque<ParserRuleContext>()

    /**
     * The key of each operand, from the last probe pass that translated it, and its slot in a rule call. An operand is
     * identified by its parse tree and the rule that reads its type (null for a probe call), because an expression can
     * be an operand of several rules: a condition of a CASE is an operand of the condition rule and of the CASE.
     */
    private val keys = HashMap<Pair<ParserRuleContext, String?>, Pair<Key, Int>>()

    /** The typings of the Redshift catalog, by hidden function name, from the last probe pass; see [Typing]. */
    private val typings = HashMap<String, Typing>()

    /** True in a probe pass; false when the AST builder makes the final AST. */
    var probing: Boolean = false
        private set

    /** True if the last probe pass probed an operand whose type is not known yet. */
    var probed: Boolean = false
        private set

    /** Starts a probe pass. */
    fun startProbe() {
        probing = true
        probed = false
    }

    /** Starts the final pass. */
    fun startFinal() {
        probing = false
    }

    /** Translates [block] with [ctx] as the query block that contains the operands. */
    fun <T> inScope(
        ctx: ParserRuleContext,
        block: () -> T,
    ): T {
        scopes.addLast(ctx)
        try {
            return block()
        } finally {
            scopes.removeLast()
        }
    }

    /**
     * The typing of a rule in the Redshift catalog: a hidden function that the probe pass calls in place of the rule,
     * with the operands of the rule as arguments. Its result type is the static type of the translation that the rule
     * produces for the argument types: the typing translates the rule with operands of those types and plans the
     * translation (see [resultType]). So the probe plan is typed as the final translation is, and the Redshift
     * semantics of a construct are described once, by its rule.
     *
     * One typing exists for each distinct rule call (see [Key]); its [id] is the name of the hidden function.
     */
    private inner class Typing(
        val id: String,
        /** The operands of the rule, without probe calls; a literal operand is passed to the rule as the literal. */
        val operands: List<Expr>,
        val rule: (List<Expr>, List<PType?>) -> Expr,
        /** True for the rule of an aggregate function, whose call the planner reads as an aggregation. */
        val aggregate: Boolean = false,
    ) {
        private val cache = HashMap<List<PType>, PType?>()

        /**
         * The static type of the translation of the rule for operands of [argTypes], or null if the rule has no
         * translation for them. A non-literal operand is a column of its type in a one-row table.
         */
        fun resultType(argTypes: List<PType>): PType? =
            cache.getOrPut(argTypes) {
                val exprs = operands.mapIndexed { i, operand -> if (operand is ExprLit) operand else column(i) }
                val translation =
                    try {
                        withoutProbing { rule(exprs, argTypes) }
                    } catch (ex: UnsupportedConstructException) {
                        return@getOrPut null
                    } catch (ex: InvalidQueryException) {
                        return@getOrPut null
                    }
                planType(translation, argTypes)
            }
    }

    /** Runs [block] as the final pass does: a rule that resolves other rules in its translation uses the known types. */
    private fun <T> withoutProbing(block: () -> T): T {
        val saved = probing
        probing = false
        try {
            return block()
        } finally {
            probing = saved
        }
    }

    /** The column of operand [i] in the one-row table of a typing; see [planType]. */
    private fun column(i: Int): Expr =
        exprPath(
            exprVarRef(Identifier.regular(TYPING_ROW), isQualified = false),
            listOf(exprPathStepField(Identifier.Simple.regular("${TYPING_COLUMN}$i"))),
        )

    /**
     * Plans `SELECT VALUE translation FROM table AS row`, where the table has one column for each type in [argTypes],
     * and returns the element type of the result, or null if the translation does not plan.
     */
    private fun planType(
        translation: Expr,
        argTypes: List<PType>,
    ): PType? {
        val fields = argTypes.mapIndexed { i, type -> PTypeField.of("${TYPING_COLUMN}$i", type) }
        val table = Table.empty(TYPING_TABLE, PType.bag(PType.row(fields)))
        val catalog = Catalog.builder().name(TYPING_CATALOG).define(table).build()
        val session = Session.builder().catalog(TYPING_CATALOG).catalogs(catalog).build()
        val from = from(listOf(fromExpr(exprVarRef(Identifier.regular(TYPING_TABLE), isQualified = false), FromType.SCAN(), Identifier.Simple.regular(TYPING_ROW))))
        val query = query(exprQuerySet(queryBodySFW(selectValue(translation), from)))
        val plan =
            try {
                typingPlanner.plan(query, session, Context.of(IgnoringPErrorListener)).plan
            } catch (ex: RuntimeException) {
                return null
            }
        val action = plan.action as? org.partiql.plan.Action.Query ?: return null
        val type = action.rex.type.pType
        return if (type.code() == PType.BAG) type.typeParameter else null
    }

    private val typingPlanner = PartiQLPlanner.standard()

    /**
     * Translates the rule [name] with [operands] with [rule], which receives the operands and their static types, null
     * for an operand that the planner could not type. In a probe pass, the result is the call of the [Typing] of the
     * rule with the operands as arguments, so that the planner types the operands and the call in one pass; [rule]
     * runs in the final pass, and in the typing. Two calls of the same rule with the same operands in one query block
     * share a typing, and so stay equal in the probe pass.
     *
     * The caller translates the operands before the call; an operand must not contain an aggregate call in a probe pass
     * (see [RedshiftFunctions.resolve]), because the planner does not find an aggregate in a function argument.
     */
    fun resolve(
        name: String,
        operands: List<Pair<ParserRuleContext, Expr>>,
        rule: (List<Expr>, List<PType?>) -> Expr,
    ): Expr {
        val exprs = operands.map { it.second }
        if (!probing) return rule(exprs, operands.map { (ctx, _) -> typeOf(ctx, name) })
        val id = register(name, operands, rule, aggregate = false)
        return exprCall(Identifier.regular(id), exprs)
    }

    /**
     * [resolve] for the rule of an aggregate function: the call of the typing is an aggregate call with [setq], which
     * the planner reads as an aggregation, so that the argument is typed in the input of the aggregation. The type of
     * the aggregation is read from the plan too; see [aggregateResultOf].
     */
    fun resolveAggregate(
        name: String,
        operands: List<Pair<ParserRuleContext, Expr>>,
        setq: SetQuantifier?,
        rule: (List<Expr>, List<PType?>) -> Expr,
    ): Expr {
        val exprs = operands.map { it.second }
        if (!probing) return rule(exprs, operands.map { (ctx, _) -> typeOf(ctx, name) })
        val id = register(name, operands, rule, aggregate = true)
        return exprCall(Identifier.regular(id), exprs, setq)
    }

    /** Registers the typing of a rule call in a probe pass and returns the name of its function; see [resolve]. */
    private fun register(
        name: String,
        operands: List<Pair<ParserRuleContext, Expr>>,
        rule: (List<Expr>, List<PType?>) -> Expr,
        aggregate: Boolean,
    ): String {
        val unprobed = operands.map { Unprobe.apply(it.second) }
        val key = Key(scopes.lastOrNull(), exprCall(Identifier.regular("$RULE$name"), unprobed))
        val base =
            ids.getOrPut(key) {
                val base = nextId
                nextId += maxOf(operands.size, 1)
                base
            }
        val id = "${if (aggregate) AGGREGATE else RULE}$base"
        typings[id] = Typing(id, unprobed, rule, aggregate)
        operands.forEachIndexed { slot, (ctx, _) ->
            keys[ctx to name] = key to slot
            if (typeOf(ctx, name) == null) probed = true
        }
        return id
    }

    /** The result types of the aggregate typing calls, by first operand id, from the last probe pass. */
    private val aggregateResults = HashMap<Int, PType>()

    /**
     * The static type of [call], the call of an aggregate typing from an earlier probe pass, or null if the call is not
     * one or its type is not known yet.
     */
    fun aggregateResultOf(call: ExprCall): PType? {
        val name = call.function.identifier.takeIf { it.isRegular() }?.getText() ?: return null
        return typingBase(name)?.takeIf { name.startsWith(AGGREGATE) }?.let { aggregateResults[it] }
    }

    /**
     * Returns [expr], the translation of [ctx], wrapped in a probe call in a probe pass. A rule that reads the type of
     * [ctx] with [typeOf] must use the returned expression in its translation.
     */
    fun operand(
        ctx: ParserRuleContext,
        expr: Expr,
    ): Expr {
        // The final pass reads the types that the last probe pass found for the same operands.
        if (!probing) return expr
        val key = Key(scopes.lastOrNull(), Unprobe.apply(expr))
        keys[ctx to null] = key to 0
        val id = ids.getOrPut(key) { nextId++ }
        if (id !in types) probed = true
        return exprCall(Identifier.regular(PROBE), listOf(exprLit(Literal.intNum(id)), expr))
    }

    /**
     * Returns the static type of the operand [ctx] of a probe call, or of the rule [name], or null if it is not known: in
     * the first probe pass, or when the planner could not type the operand.
     */
    fun typeOf(
        ctx: ParserRuleContext,
        name: String? = null,
    ): PType? = keys[ctx to name]?.let { (key, slot) -> ids[key]?.let { types[it + slot] } }

    /** Returns [expr] without probe calls. */
    fun unprobe(expr: Expr): Expr = Unprobe.apply(expr)

    /** The query block (compared by identity) and the translation of an operand without probe calls. */
    private class Key(
        val scope: ParserRuleContext?,
        val expr: Expr,
    ) {
        private val hash = 31 * System.identityHashCode(scope) + expr.hashCode()

        override fun equals(other: Any?): Boolean = other is Key && other.scope === scope && other.expr == expr

        override fun hashCode(): Int = hash
    }

    /** Removes the probe calls from a translation. The calls of typings stay: their ids follow from their operands. */
    private object Unprobe : AstRewriter<Unit>() {
        fun apply(expr: Expr): Expr = expr.accept(this, Unit) as Expr

        override fun visitExprCall(
            node: ExprCall,
            ctx: Unit,
        ): AstNode {
            val name = node.function.identifier
            if (name.isRegular() && name.getText() == PROBE && node.args.size == 2) return node.args[1].accept(this, ctx)
            return super.visitExprCall(node, ctx)
        }
    }

    /**
     * Returns the placeholder for a rule whose operand types are not known yet. The placeholder accepts [args] of any
     * type, so that the query plans and the types of the probed operands in [args] are known after the pass.
     */
    fun pending(args: List<Expr>): Expr {
        check(probing) { "A placeholder is only valid in a probe pass" }
        return exprCall(Identifier.regular(PENDING), args)
    }

    /**
     * Translates with [rule], which receives the probed operands and their types, or returns the placeholder if a type
     * is not known yet. In the final pass, an operand that the planner could not type has the dynamic type.
     */
    fun resolve(
        operands: List<Pair<ParserRuleContext, Expr>>,
        rule: (List<Expr>, List<PType>) -> Expr,
    ): Expr {
        val exprs = operands.map { (ctx, expr) -> operand(ctx, expr) }
        val known = operands.map { (ctx, _) -> typeOf(ctx) }
        if (known.any { it == null }) {
            if (probing) return pending(exprs)
            return rule(exprs, known.map { it ?: PType.dynamic() })
        }
        return rule(exprs, known.map { it!! })
    }

    /**
     * Reads the types of the probed operands from [plan], the plan of [ast]. Returns true if a type changed, which means
     * that another probe pass can resolve more rules.
     *
     * The plan has no probe in a call that the planner could not resolve. When a rule translated an operand of the
     * dynamic type, which can be the type of a placeholder of the last pass, and the call does not resolve with the real
     * type, the probe is lost and the type would stay dynamic: the type is forgotten, so that the next pass translates
     * the operand with the placeholder again.
     */
    fun collect(
        ast: AstNode,
        plan: Plan,
    ): Boolean {
        val found = HashMap<Int, PType>()
        val action = plan.action
        if (action is org.partiql.plan.Action.Query) Collector(found, aggregateResults).visit(action.rex, Unit)
        var changed = found.any { (id, type) -> types[id] != type }
        types.putAll(found)
        for (id in probeIds(ast, HashSet())) {
            if (id !in found && types[id]?.code() == PType.DYNAMIC) {
                types.remove(id)
                changed = true
            }
        }
        return changed
    }

    /** Adds the ids of the probe calls and of the operands of typing calls in [node] to [ids]. */
    private fun probeIds(
        node: AstNode,
        ids: MutableSet<Int>,
    ): Set<Int> {
        if (node is ExprCall && node.function.identifier.isRegular()) {
            val name = node.function.identifier.getText()
            if (name == PROBE && node.args.size == 2) {
                ((node.args[0] as? ExprLit)?.lit?.bigDecimalValue())?.let { ids.add(it.toInt()) }
            }
            typingBase(name)?.let { base -> node.args.indices.forEach { ids.add(base + it) } }
        }
        node.children.forEach { probeIds(it, ids) }
        return ids
    }

    /**
     * Returns [session] with the Redshift catalog: the probe functions and the typings.
     */
    fun probeSession(session: Session): Session = ProbeSession(session, RedshiftCatalog())

    private class Collector(
        private val found: MutableMap<Int, PType>,
        private val aggregates: MutableMap<Int, PType>,
    ) : OperatorRewriter<Unit>() {
        override fun visitCall(
            rex: RexCall,
            ctx: Unit,
        ): Operator {
            val name = rex.function.signature.name
            if (name == PROBE) {
                val id = (rex.args[0] as? RexLit)?.datum
                if (id != null && !id.isNull && !id.isMissing) found[id.int] = rex.args[1].type.pType
            }
            typingBase(name)?.let { base -> rex.args.forEachIndexed { slot, arg -> found[base + slot] = arg.type.pType } }
            return super.visitCall(rex, ctx)
        }

        override fun visitAggregateMeasure(
            measure: RelAggregate.Measure,
            ctx: Unit,
        ): RelAggregate.Measure {
            val signature = measure.agg.signature
            typingBase(signature.name)?.let { base ->
                measure.args.forEachIndexed { slot, arg -> found[base + slot] = arg.type.pType }
                aggregates[base] = signature.returns
            }
            return super.visitAggregateMeasure(measure, ctx)
        }

        // The rewriter does not visit the expressions of ORDER BY.
        override fun visitSort(
            rel: RelSort,
            ctx: Unit,
        ): Operator {
            visitRel(rel.input, ctx)
            rel.collations.forEach { visitRex(it.column, ctx) }
            return rel
        }

        // The rewriter does not visit window operators.
        @Suppress("DEPRECATION")
        override fun visitWindow(
            rel: RelWindow,
            ctx: Unit,
        ): Operator {
            visitRel(rel.input, ctx)
            rel.partitions.forEach { visitRex(it, ctx) }
            rel.collations.forEach { visitRex(it.column, ctx) }
            rel.windowFunctions.forEach { fn -> fn.arguments.forEach { visitRex(it, ctx) } }
            return rel
        }
    }

    /**
     * The session of a probe pass: [session] with the Redshift [catalog], which is last in the path.
     */
    private class ProbeSession(
        private val session: Session,
        private val catalog: Catalog,
    ) : Session by session {
        private val catalogs =
            object : Catalogs {
                override fun getCatalog(
                    name: String,
                    ignoreCase: Boolean,
                ): Catalog? = if (name == catalog.getName()) catalog else session.getCatalogs().getCatalog(name, ignoreCase)
            }

        override fun getCatalogs(): Catalogs = catalogs

        override fun getPath(): Path {
            val namespaces = session.getPath().toList() + listOf(Namespace.of(catalog.getName()))
            return Path.of(*namespaces.toTypedArray())
        }
    }

    /**
     * The Redshift catalog of a probe pass: the probe and placeholder functions, and the function of each [Typing]. A
     * typing call has the parameter types of its arguments, so that the planner matches the call exactly and does not
     * coerce the arguments, and returns the type of the translation of its rule for the argument types, or the dynamic
     * type when the rule has no translation for them.
     */
    private inner class RedshiftCatalog : Catalog {
        override fun getName(): String = "\$redshift_frontend_probes"

        override fun getFunctions(
            session: Session,
            name: String,
        ): Collection<FnOverload> =
            when (name) {
                PROBE -> listOf(Probe)
                PENDING -> Pending.OVERLOADS
                else -> typings[name]?.takeIf { !it.aggregate }?.let { listOf(TypingOverload(it)) } ?: emptyList()
            }

        override fun getAggregations(
            session: Session,
            name: String,
        ): Collection<AggOverload> = typings[name]?.takeIf { it.aggregate }?.let { listOf(AggregateTypingOverload(it)) } ?: emptyList()
    }

    /** The overload of the [Typing] of an aggregate rule; see [RedshiftCatalog]. Its accumulator is never run. */
    private inner class AggregateTypingOverload(private val typing: Typing) : AggOverload() {
        private val signature = RoutineOverloadSignature(typing.id, List(typing.operands.size) { PType.dynamic() })

        override fun getSignature(): RoutineOverloadSignature = signature

        override fun getInstance(args: Array<PType>): Agg =
            AggOverload.Builder(typing.id)
                .addParameters(*args)
                .returns(typing.resultType(args.toList()) ?: PType.dynamic())
                .body { error("The Redshift catalog is not executable") }
                .build()
                .getInstance(args)!!
    }

    /** The overload of a [Typing]; see [RedshiftCatalog]. */
    private inner class TypingOverload(private val typing: Typing) : FnOverload() {
        private val signature = RoutineOverloadSignature(typing.id, List(typing.operands.size) { PType.dynamic() })

        override fun getSignature(): RoutineOverloadSignature = signature

        override fun getInstance(args: Array<PType>): Fn =
            Fn.Builder(typing.id)
                .addParameters(*args)
                .returns(typing.resultType(args.toList()) ?: PType.dynamic())
                .body { Datum.nullValue() }
                .build()
    }

    /**
     * `probe(id, x)` returns `x`, with the type of `x`. The parameter types are the argument types, so that the planner
     * matches the call exactly and does not coerce the argument.
     */
    private object Probe : FnOverload() {
        private val signature = RoutineOverloadSignature(PROBE, listOf(PType.integer(), PType.dynamic()))

        override fun getSignature(): RoutineOverloadSignature = signature

        override fun getInstance(args: Array<PType>): Fn =
            Fn.Builder(PROBE)
                .addParameters(*args)
                .returns(args[1])
                .body { it[1] }
                .build()
    }

    /**
     * `pending(x1, ..., xn)` accepts arguments of any type and returns a value of the dynamic type.
     */
    private class Pending(arity: Int) : FnOverload() {
        private val signature = RoutineOverloadSignature(PENDING, List(arity) { PType.dynamic() })

        override fun getSignature(): RoutineOverloadSignature = signature

        override fun getInstance(args: Array<PType>): Fn =
            Fn.Builder(PENDING)
                .addParameters(*args)
                .returns(PType.dynamic())
                .body { Datum.nullValue() }
                .build()

        companion object {
            val OVERLOADS: List<FnOverload> = (0..MAX_PENDING_ARITY).map { Pending(it) }
        }
    }

    private object IgnoringPErrorListener : PErrorListener {
        override fun report(error: PError) {}
    }

    companion object {
        /** The number of probe passes after which the transpiler stops, even if the types still change. */
        const val MAX_PASSES = 8

        private const val MAX_PENDING_ARITY = 16

        /** The first operand id of the typing [name], or null if it is not the name of a typing. */
        private fun typingBase(name: String): Int? =
            when {
                name.startsWith(RULE) -> name.removePrefix(RULE).toIntOrNull()
                name.startsWith(AGGREGATE) -> name.removePrefix(AGGREGATE).toIntOrNull()
                else -> null
            }

        /** True if [name] is the name of the typing of an aggregate rule, whose call is an aggregation; see [resolveAggregate]. */
        fun isAggregateTyping(name: String): Boolean = name.startsWith(AGGREGATE)

        private const val TYPING_CATALOG = "\$redshift_frontend_typing"
        private const val TYPING_TABLE = "\$operands"
        private const val TYPING_ROW = "\$row"
        private const val TYPING_COLUMN = "\$a"

        /** Hidden function names, which cannot appear in the input; see `FunctionUtils` in partiql-spi. */
        private const val PROBE = "\uFDEFredshift_probe"
        private const val PENDING = "\uFDEFredshift_pending"

        /** The prefixes of the hidden names of the typings, followed by the first operand id; see [Typing]. */
        private const val RULE = "\uFDEFredshift_rule_"
        private const val AGGREGATE = "\uFDEFredshift_agg_"
    }
}
