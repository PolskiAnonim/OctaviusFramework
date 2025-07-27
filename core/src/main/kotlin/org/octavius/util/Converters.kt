package org.octavius.util

import java.util.*

object Converters {
    fun snakeToCamelCase(snakeStr: String, firstLarge: Boolean = false): String {
        return snakeStr.split('_')
            .mapIndexed { index, word ->
                if (index == 0 && !firstLarge) {
                    word.lowercase()
                } else {
                    word.lowercase()
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() }
                }
            }
            .joinToString("")
    }

    fun camelToSnakeCase(camelStr: String): String {
        return camelStr.fold(StringBuilder()) { acc, char ->
            if (char.isUpperCase()) {
                if (acc.isNotEmpty()) acc.append('_')
                acc.append(char.lowercase())
            } else {
                acc.append(char)
            }
        }.toString()
    }
}