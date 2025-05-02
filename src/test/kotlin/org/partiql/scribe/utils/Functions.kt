package org.partiql.scribe.utils

import org.partiql.spi.function.FnOverload
import org.partiql.spi.function.Parameter
import org.partiql.spi.types.PType

class Functions {
    companion object {
        val scalarSplit = FnOverload.Builder("split")
            .addParameters(
                listOf(
                    Parameter("value", PType.string()),
                    Parameter("delimiter", PType.string()),
                )
            )
            .returns(PType.array())
            .build()
    }
}