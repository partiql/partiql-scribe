//package org.partiql.scribe.sql
//
//import org.partiql.ast.Ast.exprQuerySet
//import org.partiql.ast.Ast.from
//import org.partiql.ast.Ast.fromExpr
//import org.partiql.ast.Exclude
//import org.partiql.ast.From
//import org.partiql.ast.FromType
//import org.partiql.ast.GroupBy
//import org.partiql.ast.Identifier
//import org.partiql.ast.Let
//import org.partiql.ast.OrderBy
//import org.partiql.ast.QueryBody
//import org.partiql.ast.Select
//import org.partiql.ast.SetOp
//import org.partiql.ast.SetOpType
//import org.partiql.ast.With
//import org.partiql.ast.expr.Expr
//import org.partiql.ast.expr.ExprQuerySet
//import org.partiql.plan.Operator
//import org.partiql.plan.OperatorVisitor
//import org.partiql.plan.rel.RelFilter
//import org.partiql.plan.rel.RelLimit
//import org.partiql.plan.rel.RelScan
//import org.partiql.plan.rel.RelUnion
//import org.partiql.scribe.ScribeContext
//import org.partiql.scribe.problems.ScribeProblem
//
//public open class RelConverter2(
//    private val transform: PlanToAst,
//    private val context: ScribeContext,
//) : OperatorVisitor<RelConverter2.ExprQuerySetBuilder, Unit> {
//    private val listener = context.getProblemListener()
//
//    /*
//    RelConverter -> spits out a ExprQuerySet
//    val LIMIT: EXPR?
//    val OFFSET: EXPR?
//    val ORDER_BY: ORDER_BY?
//    val WITH: WITH?
//
//    visit union() {
//        val lhs: ExprQuerySet = RelConverter.visit(lhs).toExprQuerySet
//        val rhs: ExprQuerySet = RelConverter.visit(rhs).toExprQuerySet
//        body = combine lhs + rhs
//        return exprQuerySet(
//            body,
//            lhs,
//            rhs
//        )
//    }
//
//    visit scan(ctx: QueryBodyBuilder) {
//        ctx as QueryBodyBuilderSFW
//        ctx.from =
//    }
//
//    - LIMIT/OFFSET/SORT/HAVING -> put in the RelContext
//    - UNION/EXCEPT/INTERSECT -> call the RelConverter on lhs and rhs
//    - SELECT/
//
//     */
//
//
//    // Converts to an ExprQuerySet
//    public data class ExprQuerySetBuilder(
//        /*
//        QueryBody
//        - Set
//            - LHS (ExprQuerySet | Expr)
//            - RHS (ExprQuerySet | Expr)
//        - SFW
//            - SELECT
//            - FROM
//            - WHERE
//            ...
//         */
//        public var queryBody: QueryBodyBuilder,
//        public val limit: Expr? = null,
//        public val offset: Expr? = null,
//        public val orderBy: OrderBy? = null,
//        public val with: With? = null,
//    ) {
//        internal fun foo() {
//            queryBody
//        }
//    }
//
//    // QueryBodyBuilder
//    public abstract class QueryBodyBuilder() {
//        public abstract fun toQueryBody(): QueryBody
//    }
//
//    internal data class QueryBodyBuilderSFW(
//        public var select: Select? = null,
//        public var exclude: Exclude? = null,
//        public var from: From? = null,
//        public var let: Let? = null,
//        public var where: Expr? = null,
//        public var groupBy: GroupBy? = null,
//        public var having: Expr? = null,
//        public var aggregations: List<Expr>? = null, // TODO possibly outside
//    ): QueryBodyBuilder() {
//        override fun toQueryBody(): QueryBody {
//            return QueryBody.SFW.builder()
//                .select(select)
//                .exclude(exclude)
//                .from(from)
//                .let(let)
//                .where(where)
//                .groupBy(groupBy)
//                .having(having)
//                .build()
//        }
//    }
//
//    internal class QueryBodyBuilderSetOp(
//        public var lhs: ExprQuerySet,
//        public var rhs: ExprQuerySet,
//        public var setOp: SetOp,
//    ): QueryBodyBuilder() {
//        override fun toQueryBody(): QueryBody {
//            return QueryBody.SetOp.builder()
//                .lhs(lhs)
//                .rhs(rhs)
//                .type(setOp)
//                .build()
//        }
//    }
//
//    override fun visitScan(
//        rel: RelScan,
//        ctx: Unit,
//    ): ExprQuerySetBuilder {
//        val fromRex = rel.rex
//
//        // Init a new SFW context
//        val sfw = QueryBodyBuilderSFW()
//        // Validate scan type
//        val type = rel.type
//        if (type.fields.size != 1) {
//            listener.reportAndThrow(
//                ScribeProblem.simpleError(
//                    code = ScribeProblem.INVALID_PLAN,
//                    message = "Invalid SCAN schema, expected a single PTypeField but found ${type.dump()}",
//                ),
//            )
//        }
//        val rexConverter =
//            transform.getRexConverter(
//                Locals(
//                    env = type.fields.toList(),
//                    aggregations = emptyList(),
//                    // no projections
//                ),
//            )
//        // convert to FROM clause
//        sfw.from =
//            from(
//                tableRefs =
//                listOf(
//                    fromExpr(
//                        expr = rexConverter.apply(fromRex),
//                        // FROM <rex>
//                        fromType = FromType.SCAN(),
//                        asAlias = Identifier.Simple.delimited(type.fields[0].name),
//                    ),
//                ),
//            )
//        return ExprQuerySetBuilder(
//            queryBody = sfw,
//            limit = null,
//            offset = null,
//            orderBy = null,
//            with = null,
//        )
//    }
//
//    override fun visitUnion(node: RelUnion, ctx: Unit): ExprQuerySetBuilder {
//        val lhs = visit(node.left, Unit) // ctx as second arg
//        val rhs = visit(node.right, Unit) // ctx as second arg
//        return ExprQuerySetBuilder(
//            QueryBodyBuilderSetOp(
//                lhs = exprQuerySet(lhs.queryBody.toQueryBody()),
//                rhs = exprQuerySet(rhs.queryBody.toQueryBody()),
//                setOp = SetOp.builder().setOpType(SetOpType.UNION()).build(),
//            ),
//            limit = null, // TODO ALAN should be copied?
//            offset = null,
//            orderBy = null,
//            with = null
//        )
//    }
//
//    override fun visitLimit(rel: RelLimit, ctx: Unit): ExprQuerySetBuilder {
//        val input = visit(rel.input, Unit)
//        return input.copy(
//            limit = transform.getRexConverter(Locals.EMPTY).visit(rel.limit, RexConverter.RexContext())
//        )
//    }
//
//    override fun visitFilter(rel: RelFilter, ctx: Unit): ExprQuerySetBuilder {
//        val input = visit(rel.input, Unit)
//        return input.copy(
//            queryBody = (input.queryBody as QueryBodyBuilderSFW).copy(
//                where = transform.getRexConverter(Locals.EMPTY).visit(rel.predicate, RexConverter.RexContext())
//            )
//        )
//    }
//
//    override fun defaultReturn(operator: Operator, ctx: Unit?): ExprQuerySetBuilder {
//        TODO("Not yet implemented")
//    }
//}
