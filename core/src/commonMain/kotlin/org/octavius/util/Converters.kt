package org.octavius.util

/**
 * Narzędzia do konwersji konwencji nazewniczych między camelCase a snake_case.
 *
 * Używane głównie do mapowania nazw klas/właściwości Kotlin na nazwy typów/kolumn PostgreSQL.
 */
object Converters {
    /**
     * Konwertuje string w konwencji snake_case na camelCase.
     *
     * @param snakeStr String w konwencji snake_case (np. "user_name").
     * @param firstLarge Jeśli true, pierwsza litera będzie wielka (PascalCase).
     * @return String w konwencji camelCase (np. "userName" lub "UserName").
     */
    fun toCamelCase(snakeStr: String, firstLarge: Boolean = false): String {
        return snakeStr.split('_')
            .mapIndexed { index, word ->
                if (index == 0 && !firstLarge) {
                    word.lowercase()
                } else {
                    word.lowercase()
                        .replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                }
            }
            .joinToString("")
    }

    /**
     * Konwertuje string w konwencji camelCase na snake_case.
     *
     * @param camelStr String w konwencji camelCase (np. "userName").
     * @return String w konwencji snake_case (np. "user_name").
     */
    fun toSnakeCase(camelStr: String): String {
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