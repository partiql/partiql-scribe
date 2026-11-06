package org.partiql.scribe.frontend.redshift

import org.partiql.scribe.utils.SessionProvider
import org.partiql.spi.catalog.Session
import org.partiql.spi.function.FnOverload
import org.partiql.spi.types.PType

/**
 * The session the front end's tests plan against: the Scribe test catalogs, with the scalar function `pow` over two
 * `DOUBLE PRECISION` arguments that `POWER`, `POW`, and `^` are written as. PartiQL's built-in functions do not define
 * it; a deployment's catalog does.
 */
internal object RedshiftTestSession {
    val POW: FnOverload =
        FnOverload.Builder("pow")
            .addParameters(PType.doublePrecision(), PType.doublePrecision())
            .returns(PType.doublePrecision())
            .build()

    /** The test catalogs with `pow`. */
    fun withPow(): Session = SessionProvider(scalarOverloads = mapOf("pow" to POW)).getSession()

    /** The test catalogs as they are, without `pow`. */
    fun withoutPow(): Session = SessionProvider().getSession()
}
