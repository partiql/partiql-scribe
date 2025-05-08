package org.partiql.scribe.utils

import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType

/**
 * Utility class for test function definitions.
 */
class Functions {
    companion object {
        /**
         * Function overload to test that a UDF (`split`) can be transpiled.
         */
        val scalarSplit =
            FnOverload.Builder("split")
                .addParameters(
                    listOf(
                        Parameter("value", PType.string()),
                        Parameter("delimiter", PType.string()),
                    ),
                )
                .returns(PType.array())
                .build()
    }
}
