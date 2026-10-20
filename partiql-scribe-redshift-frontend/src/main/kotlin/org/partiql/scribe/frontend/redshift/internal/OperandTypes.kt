package org.partiql.scribe.frontend.redshift.internal

import org.antlr.v4.runtime.ParserRuleContext
import org.partiql.ast.Ast.exprCall
import org.partiql.ast.Ast.exprLit
import org.partiql.ast.AstNode
import org.partiql.ast.AstRewriter
import org.partiql.ast.Identifier
import org.partiql.ast.Literal
import org.partiql.ast.expr.Expr
import org.partiql.ast.expr.ExprCall
import org.partiql.ast.expr.ExprLit
import org.partiql.plan.Operator
import org.partiql.plan.OperatorRewriter
import org.partiql.plan.Plan
import org.partiql.plan.rel.RelSort
import org.partiql.plan.rel.RelWindow
import org.partiql.plan.rex.RexCall
import org.partiql.plan.rex.RexLit
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Catalogs
import org.partiql.spi.catalog.Namespace
import org.partiql.spi.catalog.Path
import org.partiql.spi.catalog.Session
import org.partiql.spi.function.Fn
import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.RoutineOverloadSignature
import org.partiql.spi.types.PType
import org.partiql.spi.value.Datum
import java.util.IdentityHashMap

/**
 * The static types of Redshift operands, for the rules whose translation depends on them.
 *
 * Some Redshift constructs have different semantics for different argument types. For example, `LEN` of a CHAR(n)
 * value does not count trailing blanks, and `AVG` of an integer argument truncates the result. The AST builder gets
 * the types from the PartiQL planner:
 *
 *  1. In a probe pass, the AST builder wraps each operand whose type a rule needs in a probe call (see [operand]). The
 *     probe returns its argument with the argument's type. A rule whose operand types are not known yet translates to
 *     a placeholder (see [pending]), which accepts arguments of any type and returns a value of the dynamic type.
 *  2. The planner plans the AST against the session and the probe functions (see [probeSession]), and [collect] reads
 *     the type of each probed operand from the plan.
 *  3. Steps 1 and 2 repeat until the types do not change. Each pass resolves the rules whose operands contain no
 *     placeholder, so the number of passes is one more than the nesting depth of such rules.
 *  4. The AST builder translates the query without probes, with the final types.
 *
 * An operand is identified by its translation, without probe calls, and the query block that contains it. Two operands
 * with the same translation in one query block have the same probe, so that a select item and the GROUP BY key that it
 * repeats (`UPPER(c)` and `UPPER(t.c)`) stay equal in a probe pass. When a rule in an operand is resolved, the
 * translation of the operand changes, and the next pass probes it again.
 */
internal class OperandTypes {
    private val ids = HashMap<Key, Int>()
    private val types = HashMap<Int, PType>()
    private val scopes = ArrayDeque<ParserRuleContext>()

    /** The key of each operand, from the last probe pass that translated it. */
    private val keys = IdentityHashMap<ParserRuleContext, Key>()

    /** True in a probe pass; false when the AST builder makes the final AST. */
    var probing: Boolean = false
        private set

    /** True if the last probe pass wrapped an operand in a probe call. */
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
        keys[ctx] = key
        probed = true
        val id = ids.getOrPut(key) { ids.size }
        return exprCall(Identifier.regular(PROBE), listOf(exprLit(Literal.intNum(id)), expr))
    }

    /**
     * Returns the static type of the operand [ctx], or null if it is not known: in the first probe pass, or when the
     * planner could not type the operand.
     */
    fun typeOf(ctx: ParserRuleContext): PType? = keys[ctx]?.let { ids[it] }?.let { types[it] }

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

    /** Removes the probe calls from a translation. */
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
        if (action is org.partiql.plan.Action.Query) Collector(found).visit(action.rex, Unit)
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

    /** Adds the ids of the probe calls in [node] to [ids]. */
    private fun probeIds(
        node: AstNode,
        ids: MutableSet<Int>,
    ): Set<Int> {
        if (node is ExprCall && node.function.identifier.let { it.isRegular() && it.getText() == PROBE } && node.args.size == 2) {
            ((node.args[0] as? ExprLit)?.lit?.bigDecimalValue())?.let { ids.add(it.toInt()) }
        }
        node.children.forEach { probeIds(it, ids) }
        return ids
    }

    /**
     * Returns [session] with the probe functions.
     */
    fun probeSession(session: Session): Session = ProbeSession(session)

    private class Collector(private val found: MutableMap<Int, PType>) : OperatorRewriter<Unit>() {
        override fun visitCall(
            rex: RexCall,
            ctx: Unit,
        ): Operator {
            if (rex.function.signature.name == PROBE) {
                val id = (rex.args[0] as? RexLit)?.datum
                if (id != null && !id.isNull && !id.isMissing) found[id.int] = rex.args[1].type.pType
            }
            return super.visitCall(rex, ctx)
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
     * The session of a probe pass: [session] with a catalog for the probe functions, which is last in the path.
     */
    private class ProbeSession(private val session: Session) : Session by session {
        private val catalogs =
            object : Catalogs {
                override fun getCatalog(
                    name: String,
                    ignoreCase: Boolean,
                ): Catalog? = if (name == ProbeCatalog.getName()) ProbeCatalog else session.getCatalogs().getCatalog(name, ignoreCase)
            }

        override fun getCatalogs(): Catalogs = catalogs

        override fun getPath(): Path {
            val namespaces = session.getPath().toList() + listOf(Namespace.of(ProbeCatalog.getName()))
            return Path.of(*namespaces.toTypedArray())
        }
    }

    private object ProbeCatalog : Catalog {
        override fun getName(): String = "\$redshift_frontend_probes"

        override fun getFunctions(
            session: Session,
            name: String,
        ): Collection<FnOverload> =
            when (name) {
                PROBE -> listOf(Probe)
                PENDING -> Pending.OVERLOADS
                else -> emptyList()
            }
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

    companion object {
        /** The number of probe passes after which the transpiler stops, even if the types still change. */
        const val MAX_PASSES = 8

        private const val MAX_PENDING_ARITY = 16

        /** Hidden function names, which cannot appear in the input; see `FunctionUtils` in partiql-spi. */
        private const val PROBE = "\uFDEFredshift_probe"
        private const val PENDING = "\uFDEFredshift_pending"
    }
}
