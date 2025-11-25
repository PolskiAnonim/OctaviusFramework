package org.octavius.data.util

object CaseConverter {

    /**
     * Główna funkcja konwertująca wartość z jednej konwencji na drugą.
     */
    fun convert(value: String, from: CaseConvention, to: CaseConvention): String {
        if (from == to) {
            return value
        }

        val words = splitToWords(value, from)
        return joinWords(words, to)
    }

    private fun splitToWords(value: String, convention: CaseConvention): List<String> {
        return when (convention) {
            CaseConvention.SNAKE_CASE_LOWER,
            CaseConvention.SNAKE_CASE_UPPER -> value.split('_')

            CaseConvention.PASCAL_CASE,
            CaseConvention.CAMEL_CASE -> {
                // Rozdziela CamelCase/PascalCase (np. "myValue" -> "my", "Value")
                // Regex łapie wielką literę, która jest poprzedzona małą literą lub cyfrą
                value.split(Regex("(?<=[a-z0-9])(?=[A-Z])"))
            }
        }
    }

    private fun joinWords(words: List<String>, convention: CaseConvention): String {
        return when (convention) {
            CaseConvention.SNAKE_CASE_UPPER ->
                words.joinToString("_") { it.uppercase() }

            CaseConvention.SNAKE_CASE_LOWER ->
                words.joinToString("_") { it.lowercase() }

            CaseConvention.PASCAL_CASE ->
                words.joinToString("") { it.lowercase().replaceFirstChar { char -> char.titlecase() } }

            CaseConvention.CAMEL_CASE ->
                words.mapIndexed { index, word ->
                    val lower = word.lowercase()
                    if (index == 0) lower else lower.replaceFirstChar { char -> char.titlecase() }
                }.joinToString("")
        }
    }
}

// --- Extension Functions (Kompatybilność wsteczna dla reszty projektu) ---

/**
 * Zamienia dowolny tekst w stylu Camel/Pascal na snake_case.
 * Używane np. do generowania nazw kolumn z nazw pól (userId -> user_id).
 */
fun String.toSnakeCase(): String {
    // Zakładamy, że kod w Kotlinie jest w CamelCase lub PascalCase, a chcemy snake_case
    return CaseConverter.convert(this, CaseConvention.CAMEL_CASE, CaseConvention.SNAKE_CASE_LOWER)
}

/**
 * Zamienia snake_case na camelCase.
 * Używane np. przy mapowaniu z bazy (user_id -> userId).
 */
fun String.toCamelCase(): String {
    return CaseConverter.convert(this, CaseConvention.SNAKE_CASE_LOWER, CaseConvention.CAMEL_CASE)
}

/**
 * Zamienia snake_case na PascalCase.
 * Używane np. przy mapowaniu nazw klas (table_name -> TableName).
 */
fun String.toPascalCase(): String {
    return CaseConverter.convert(this, CaseConvention.SNAKE_CASE_LOWER, CaseConvention.PASCAL_CASE)
}