package org.octavius.gradle

/**
 * Reprezentuje typ wpisu w JSON tłumaczeń.
 */
internal sealed class TranslationEntry {
    /** Prosty tekst bez parametrów */
    data class Simple(val value: String) : TranslationEntry()

    /** Tekst z parametrami {0}, {1}, ... */
    data class Parameterized(val value: String, val paramCount: Int) : TranslationEntry()

    /** Forma pluralna (one/few/many) */
    data class Plural(val forms: Map<String, String>) : TranslationEntry()

    /** Zagnieżdżony obiekt */
    data class Nested(val children: Map<String, TranslationEntry>) : TranslationEntry()
}

internal val PLURAL_KEYS = setOf("one", "few", "many")
internal val PARAM_REGEX = Regex("""\{(\d+)\}""")


/**
 * Konwertuje snake_case lub kebab-case na PascalCase.
 */
internal fun toPascalCase(input: String): String {
    return input.split("_", "-")
        .joinToString("") { word ->
            word.replaceFirstChar { it.uppercaseChar() }
        }
}

/**
 * Konwertuje string na camelCase (pierwsza litera mała).
 */
internal fun toCamelCase(input: String): String {
    val pascal = toPascalCase(input)
    return pascal.replaceFirstChar { it.lowercaseChar() }
}

/**
 * Escapuje nazwę jeśli jest słowem kluczowym.
 */
internal fun escapeName(name: String): String {
    return if (isKotlinKeyword(name)) "`$name`" else name
}

/**
 * Sprawdza czy nazwa jest zarezerwowanym słowem kluczowym Kotlin.
 */
internal fun isKotlinKeyword(name: String): Boolean {
    val keywords = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun",
        "if", "in", "interface", "is", "null", "object", "package", "return",
        "super", "this", "throw", "true", "try", "typealias", "typeof", "val",
        "var", "when", "while"
    )
    return name in keywords
}

internal fun mergeJsonMaps(target: MutableMap<String, Any?>, source: Map<String, Any?>) {
    for ((key, sourceValue) in source) {
        val targetValue = target[key]
        if (sourceValue is Map<*, *> && targetValue is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val newTarget = (targetValue as Map<String, Any?>).toMutableMap()
            @Suppress("UNCHECKED_CAST")
            mergeJsonMaps(newTarget, sourceValue as Map<String, Any?>)
            target[key] = newTarget
        } else {
            target[key] = sourceValue
        }
    }
}