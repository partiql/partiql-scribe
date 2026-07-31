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
         * Function overload to test that a UDF can be transpiled.
         *
         * NOTE: `split` used to be registered here as a test UDF, but it is now a PartiQL builtin
         * (see PLK `FnSplit`), so it resolves without a registered overload.
         */
        val scalarUdf =
            FnOverload.Builder("my_udf")
                .addParameters(
                    listOf(
                        Parameter("value", PType.string()),
                    ),
                )
                .returns(PType.string())
                .build()
    }
}
