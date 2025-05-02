package org.partiql.scribe

/**
 * A place to record compilation oddities
 *
 * @property level
 * @property message
 */
class ScribeProblem(val level: Level, val message: String) {

    enum class Level {
        INFO,
        WARNING,
        ERROR,
    }

    override fun toString() = "$level: $message"
}
