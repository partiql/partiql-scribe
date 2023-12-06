package org.partiql.scribe

import org.partiql.parser.PartiQLParserBuilder
import org.partiql.planner.Header
import org.partiql.planner.PartiQLPlannerBuilder
import org.partiql.spi.Plugin
import org.partiql.types.function.FunctionSignature

public class ScribeCompilerBuilder {

    private var parser = PartiQLParserBuilder.standard().build()
    private var planner = PartiQLPlannerBuilder()
    private var plugins = listOf<Plugin>()
    private var functions = listOf<FunctionSignature.Scalar>()

    public fun build(): ScribeCompiler {
        return ScribeCompiler(
            parser, planner.plugins(plugins).headers(listOf(functions.asHeader())).build()
        )
    }

    public fun plugins(plugins: List<Plugin>): ScribeCompilerBuilder = this.apply {
        this.plugins = plugins
    }

    public fun functions(functions: List<FunctionSignature.Scalar>): ScribeCompilerBuilder = this.apply {
        this.functions = functions
    }

    // make a sim
    private fun List<FunctionSignature.Scalar>.asHeader() = object : Header() {

        override val namespace: String = "extensions"

        override val functions: List<FunctionSignature.Scalar> = this@asHeader
    }
}
