package org.octavius.data.util


/**
 * Prosta funkcja oczyszczająca String z apostrofów i cudzysłowów drukarskich
 */
fun String.clean(): String {
    return buildString(this.length) {
        for (c in this@clean) {
            when (c) {
                '‘', '’' -> append('\'')
                '“', '”' -> append('"')
                else    -> append(c)
            }
        }
    }
}
