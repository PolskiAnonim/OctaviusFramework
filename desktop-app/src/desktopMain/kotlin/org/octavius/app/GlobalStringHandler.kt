package org.octavius.app

import io.github.octaviusframework.db.api.type.GlobalTypeHandler
import kotlin.reflect.KClass

class GlobalStringHandler: GlobalTypeHandler<String> {
    override val pgTypeName: String = "text"
    override val kotlinClass: KClass<String> = String::class
    override val fromPgString: (String) -> String = { it }
    override val toPgString: (String) -> String = { it.clean() }
    override val isDefaultForKotlinType: Boolean = true
}

/**
 * Simple function cleaning String from typographic apostrophes and quotes
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
