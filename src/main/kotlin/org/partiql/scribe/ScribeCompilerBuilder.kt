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
                .build()
        )
    }
}
