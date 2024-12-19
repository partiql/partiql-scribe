package org.partiql.scribe.sql

public abstract class SqlVisitor<R, C> {

    public fun defaultVisit(node: SqlNode, ctx: C): R {
        for (child in node.getChildren()) {
            child.accept(this, ctx)
        }
        return defaultReturn(node, ctx)
    }

    public abstract fun defaultReturn(node: SqlNode?, ctx: C): R

    // public fun visit(node: SqlNode, ctx: C): R {
    //     return defaultVisit(node, ctx)
    // }

    // IDENTIFIERS AND NAMES

    // public fun visitIdentifier(node: SqlIdentifier, ctx: C): R {
    //     return defaultVisit(node, ctx)
    // }

    public fun visitName(node: SqlName, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    // STATEMENTS

    public fun visitStatement(node: SqlStatement, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    // STATEMENTS DQL

    public fun visitSelect(node: SqlSelect, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    public fun visitQuery(node: SqlQuery, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    // STATEMENTS DML

    //  [placeholder]

    // STATEMENTS DDL

    //  [placeholder]

    // SELECT

    public fun visitSelection(node: SqlSelection, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    public fun visitSelectStar(node: SqlSelectStar, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    public fun visitSelectList(node: SqlSelectList, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    public fun visitSelectItem(node: SqlSelectItem, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    public fun visitSelectValue(node: SqlSelectValue, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    public fun visitSelectPivot(node: SqlSelectPivot, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    // FROM

    public fun visitFrom(node: SqlFrom, ctx: C): R {
        return defaultVisit(node, ctx)
    }

    // EXPRESSIONS

    public fun visitExpr(node: SqlExpr, ctx: C): R {
        return defaultVisit(node, ctx)
    }
}
