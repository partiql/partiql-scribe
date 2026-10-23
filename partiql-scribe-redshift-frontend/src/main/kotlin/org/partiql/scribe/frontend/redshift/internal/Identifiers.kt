package org.partiql.scribe.frontend.redshift.internal

/**
 * Folds the ASCII letters A to Z of [text] to lower case, as Redshift folds identifiers: other characters, including
 * non-ASCII letters, are kept.
 *
 * https://docs.aws.amazon.com/redshift/latest/dg/r_names.html
 */
internal fun foldAscii(text: String): String =
    buildString(text.length) {
        for (c in text) append(if (c in 'A'..'Z') c + ('a' - 'A') else c)
    }
