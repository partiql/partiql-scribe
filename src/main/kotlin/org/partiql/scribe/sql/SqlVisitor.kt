package org.partiql.scribe.sql

/**
 * A base visitor for the Scribe IR.
 *
 * @param R
 * @param C
 */
public abstract class SqlVisitor<R, C> {

    public open fun defaultVisit(node: SqlNode, ctx: C): R {
        for (child in node.getChildren()) {
            child.accept(this, ctx)
        }
        return defaultReturn(node, ctx)
    }

    public abstract fun defaultReturn(node: SqlNode?, ctx: C): R

    // public open fun visit(node: SqlNode, ctx: C): R {
    //     return defaultVisit(node, ctx)
    // }

    // IDENTIFIERS & NAMES

    // public open fun visitIdentifier(node: SqlIdentifier, ctx: C): R {
    //     return defaultVisit(node, ctx)
    // }

    public open fun visitName(node: SqlName, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    // STATEMENTS

    public open fun visitStatement(node: SqlStatement, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    public open fun visitSelect(node: SqlSelect, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    //  [DML placeholder]

    //  [DDL placeholder]

    // QUERY

    public open fun visitQuery(node: SqlQuery, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    public open fun visitQueryExpr(node: SqlQueryExpr, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    public open fun visitQueryBody(node: SqlQueryBody, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    public open fun visitQuerySpec(node: SqlQuerySpec, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    public open fun visitWith(node: SqlWith, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    public open fun visitSelection(node: SqlSelection, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    public open fun visitSelectStar(node: SqlSelectStar, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    public open fun visitSelectList(node: SqlSelectList, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    public open fun visitSelectItem(node: SqlSelectItem, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    public open fun visitSelectValue(node: SqlSelectValue, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    public open fun visitSelectPivot(node: SqlSelectPivot, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    // FROM

    public open fun visitFrom(node: SqlFrom, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    // EXPRESSIONS

    public open fun visitExpr(node: SqlExpr, ctx: C): R {
        return defaultVisit(node, ctx)
    }
}
