package org.partiql.scribe.targets.duckdb

import org.partiql.plan.Operator
import org.partiql.plan.OperatorRewriter
import org.partiql.plan.rel.RelExclude
import org.partiql.plan.rel.RelProject
import org.partiql.plan.rex.RexStruct
import org.partiql.plan.rex.RexVar
import org.partiql.scribe.ScribeContext
import org.partiql.scribe.sql.utils.isPathRex
import org.partiql.scribe.targets.duckdb.utils.toRexDuckDB

/**
 * Plan -> plan rewriter for the DuckDB target.
 *
 * It drops standalone [RelExclude] nodes feeding a [RelProject] (matching the other SQL targets) and
 * reconstructs struct fields that contain excluded fields (see DuckDBExcludeTranspilation.md). Other
 * DuckDB-specific rewrites (array index shifting, BAG -> ARRAY, IS MISSING -> IS NULL, etc.) will be added here.
 */
public open class DuckDBRewriter(internal val context: ScribeContext) : OperatorRewriter<ScribeContext>() {
    override fun visitProject(
        rel: RelProject,
        ctx: ScribeContext?,
    ): Operator {
        val input = rel.input
        val newNode =
            if (input is RelExclude) {
                val newProject = RelProject.create(input.input, rel.projections)
                newProject.type = rel.type
                newProject
            } else {
                rel
            }
        return super.visitProject(newNode, ctx)
    }

    override fun visitStruct(
        rex: RexStruct,
        ctx: ScribeContext,
    ): Operator {
        val struct = super.visitStruct(rex, ctx) as RexStruct
        val newStructFields =
            struct.fields.map { field ->
                val fieldValue = field.value
                val type = field.value.type
                // Rewrite any structs that have a field that's a ROW type and is a var reference or path.
                val newOp =
                    if (fieldValue is RexVar || fieldValue.isPathRex() || fieldValue is RexStruct) {
                        type.pType.toRexDuckDB(
                            prefixPath = fieldValue,
                            context = context,
                        )
                    } else {
                        fieldValue
                    }
                RexStruct.field(
                    field.key,
                    newOp,
                )
            }
        val newStruct = RexStruct.create(newStructFields)
        newStruct.type = struct.type
        return newStruct
    }
}
