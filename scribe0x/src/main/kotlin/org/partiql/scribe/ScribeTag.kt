package org.partiql.scribe

/**
 * An information tag for outputs.
 *
 * @property scribeVersion     The Scribe version.
 * @property scribeCommit      The Scribe git hash.
 * @property target            The target name.
 * @property targetVersion     The target version.
 */
public data class ScribeTag(
    @JvmField val scribeVersion: String,
    @JvmField val scribeCommit: String,
    @JvmField val target: String,
    @JvmField val targetVersion: String,
)
