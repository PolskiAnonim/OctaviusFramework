package org.octavius.data.util

/**
 * Konwertuje string w konwencji snake_case na camelCase.
 *
* @return String w konwencji camelCase (np. "userName").
 */
fun String.toCamelCase(): String = camelAndPascalCase(this, false)

/**
 * Konwertuje string w konwencji snake_case na PascalCase.
 *
 * @return String w konwencji PascalCase (np. "UserName").
 */
fun String.toPascalCase(): String = camelAndPascalCase(this, true)

/**
 * Konwertuje string w konwencji Pascal/camelCase na snake_case.
 *
 * @return String w konwencji snake_case (np. "user_name").
 */
fun String.toSnakeCase(): String = this.fold(StringBuilder()) { acc, char ->
    if (char.isUpperCase()) {
        if (acc.isNotEmpty()) acc.append('_')
        acc.append(char.lowercase())
    } else {
        acc.append(char)
    }
}.toString()


private fun camelAndPascalCase(snakeStr: String, firstLarge: Boolean = false): String {
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