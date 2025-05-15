package org.partiql.scribe.utils

/**
 * Holding class for test input.
 *
 * --#[example-test]
 * SELECT * FROM example;
 */
public data class ScribeTest(
    public val key: Key,
    public val statement: String,
) {
    /**
     * Unique test identifier.
     *
     * @property group
     * @property name
     */
    public data class Key(
        public val group: String,
        public val name: String,
    ) {
        override fun toString(): String {
            return "${group}__$name"
        }
    }
}
