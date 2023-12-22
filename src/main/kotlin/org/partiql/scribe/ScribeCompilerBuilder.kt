package org.partiql.scribe

import org.partiql.parser.PartiQLParserBuilder
import org.partiql.planner.PartiQLPlannerBuilder
import org.partiql.spi.connector.ConnectorMetadata
import org.partiql.types.function.FunctionSignature

public class ScribeCompilerBuilder {

    private var parser = PartiQLParserBuilder().build()
    private var planner = PartiQLPlannerBuilder()
    private var functions = listOf<FunctionSignature.Scalar>()

    public fun build(): ScribeCompiler {
        return ScribeCompiler(
            parser,
            planner
                .build(),
            functions
        )
    }

    // TODO: Currently the SPI interface does not has the ability to load function
    //  We currently load the function signature manually using the Local Plugin's connector metadata implementation
    //  functions are not used at the moment.
    public fun functions(functions: List<FunctionSignature.Scalar>): ScribeCompilerBuilder = this.apply {
        this.functions = functions
    }
}
