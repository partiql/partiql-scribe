package org.partiql.scribe.frontend.redshift

import org.partiql.extensions.functions.custom.math.Pow
import org.partiql.scribe.frontend.redshift.internal.functions.MathFunctions
import org.partiql.scribe.utils.SessionProvider
import org.partiql.spi.catalog.Catalog
import org.partiql.spi.catalog.Session
import org.partiql.spi.function.FnOverload

/**
 * The session the front end's tests plan against: the Scribe test catalogs, with the scalar function `pow` that
 * `POWER`, `POW`, and `^` are written as, resolved to `Pow.overloads()` of `org.partiql:partiql-function-extensions`,
 * one overload per numeric type, as a deployment that plugs in the extensions has it. PartiQL's built-in functions do
 * not define `pow`.
 */
internal object RedshiftTestSession {
    /** The test catalogs with `pow`. */
    fun withPow(): Session {
        val base = SessionProvider().getSession()
        val default = base.getCatalogs().getCatalog("default", false)!!
        val withPow =
            object : Catalog by default {
                override fun getFunctions(
                    session: Session,
                    name: String,
                ): Collection<FnOverload> = if (name == MathFunctions.POW) Pow.overloads() else default.getFunctions(session, name)
            }
        return Session.builder().catalog("default").catalogs(withPow).namespace(emptyList()).build()
    }

    /** The test catalogs as they are, without `pow`. */
    fun withoutPow(): Session = SessionProvider().getSession()
}
