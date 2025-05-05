package org.partiql.scribe.utils

import org.partiql.spi.errors.PError
import org.partiql.spi.errors.PErrorListener
import org.partiql.spi.errors.Severity

/**
 * Collects PartiQL parser and planner errors for testing. Allows us to collect multiple errors.
 *
 * Taken from PLK-planner's PErrorCollector.
 */
class PErrorCollector : PErrorListener {
    private val errorList = mutableListOf<PError>()
    private val warningList = mutableListOf<PError>()

    val problems: List<PError>
        get() = errorList + warningList

    val errors: List<PError>
        get() = errorList

    val warnings: List<PError>
        get() = warningList

    override fun report(error: PError) {
        when (error.severity.code()) {
            Severity.ERROR -> errorList.add(error)
            Severity.WARNING -> warningList.add(error)
            else -> error("Unsupported severity.")
        }
    }
}
