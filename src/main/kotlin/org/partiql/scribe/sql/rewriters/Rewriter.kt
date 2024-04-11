package org.partiql.scribe.sql.rewriters

import org.partiql.ast.AstNode

internal interface Rewriter {

    fun apply(ast: AstNode): AstNode

}