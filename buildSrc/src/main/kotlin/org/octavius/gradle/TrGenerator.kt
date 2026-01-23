package org.octavius.gradle


/**
 * Generuje główny plik Tr.kt z registry i type-safe accessorami.
 */
internal class TrGenerator(private val packageName: String) {
    private val builder = StringBuilder()
    private var indentLevel = 0

    private fun indent() = "    ".repeat(indentLevel)

    private fun appendLine(line: String = "") {
        if (line.isEmpty()) {
            builder.appendLine()
        } else {
            builder.appendLine("${indent()}$line")
        }
    }

    fun generate(entries: Map<String, TranslationEntry>, defaultLang: String): String {
        builder.clear()

        appendLine("@file:Suppress(\"unused\", \"RedundantVisibilityModifier\")")
        appendLine()
        appendLine("package $packageName")
        appendLine()
        appendLine("/**")
        appendLine(" * Type-safe accessors for translations with runtime language switching.")
        appendLine(" *")
        appendLine(" * This file is auto-generated. Do not edit manually.")
        appendLine(" *")
        appendLine(" * Usage:")
        appendLine(" * ```kotlin")
        appendLine(" * Tr.Action.save()           // Get translation")
        appendLine(" * Tr.currentLanguage = \"en\"  // Switch language")
        appendLine(" * Tr.register(\"de\", TranslationsDe)  // Register new language")
        appendLine(" * ```")
        appendLine(" */")
        appendLine("public object Tr {")
        indentLevel++

        // Registry i currentLanguage
        appendLine()
        appendLine("private val registry = mutableMapOf<kotlin.String, TranslationData>()")
        appendLine()
        appendLine("/** Currently active language */")
        appendLine("public var currentLanguage: kotlin.String = \"$defaultLang\"")
        appendLine()

        // Register function
        appendLine("/** Register translation data for a language */")
        appendLine("public fun register(lang: kotlin.String, data: TranslationData) {")
        indentLevel++
        appendLine("registry[lang] = data")
        indentLevel--
        appendLine("}")
        appendLine()

        // Init block
        val langPascal = toPascalCase(defaultLang)
        appendLine("init {")
        indentLevel++
        appendLine("register(\"$defaultLang\", Translations$langPascal)")
        indentLevel--
        appendLine("}")
        appendLine()

        // Private data accessor
        appendLine("private val data: TranslationData")
        indentLevel++
        appendLine("get() = registry[currentLanguage] ?: error(\"Language \\\"${'$'}currentLanguage\\\" not registered\")")
        indentLevel--
        appendLine()

        // Lookup functions
        appendLine("private fun lookup(key: kotlin.String, vararg args: kotlin.Any): kotlin.String {")
        indentLevel++
        appendLine("val template = data.simple[key] ?: return key")
        appendLine("return formatString(template, *args)")
        indentLevel--
        appendLine("}")
        appendLine()

        appendLine("private fun lookupPlural(key: kotlin.String, count: kotlin.Int, vararg args: kotlin.Any): kotlin.String {")
        indentLevel++
        appendLine("val forms = data.plural[key] ?: return key")
        appendLine("val form = when {")
        indentLevel++
        appendLine("count == 1 -> forms.one ?: forms.many")
        appendLine("count % 10 in 2..4 && count % 100 !in 12..14 -> forms.few ?: forms.many")
        appendLine("else -> forms.many")
        indentLevel--
        appendLine("}")
        appendLine("return formatString(form, count, *args)")
        indentLevel--
        appendLine("}")
        appendLine()

        appendLine("private fun formatString(template: kotlin.String, vararg args: kotlin.Any): kotlin.String {")
        indentLevel++
        appendLine("if (args.isEmpty()) return template")
        appendLine("var result = template")
        appendLine("args.forEachIndexed { index, arg ->")
        indentLevel++
        appendLine("result = result.replace(\"{${'$'}index}\", arg.toString())")
        indentLevel--
        appendLine("}")
        appendLine("return result")
        indentLevel--
        appendLine("}")
        appendLine()

        // Backward compatibility - pozwala używać Tr.get() zamiast T.get()
        appendLine("// ========== Backward Compatibility ==========")
        appendLine()
        appendLine("/** Backward compatible access to translations by key */")
        appendLine("public fun get(key: kotlin.String, vararg args: kotlin.Any): kotlin.String = lookup(key, *args)")
        appendLine()
        appendLine("/** Backward compatible access to plural translations by key */")
        appendLine("public fun getPlural(key: kotlin.String, count: kotlin.Int, vararg args: kotlin.Any): kotlin.String = lookupPlural(key, count, *args)")
        appendLine()

        // Type-safe accessors
        generateEntries(entries, "")

        indentLevel--
        appendLine("}")
        appendLine()
        appendLine("/** @deprecated Use Tr directly */")
        appendLine("@Deprecated(\"Use Tr directly\", ReplaceWith(\"Tr\"))")
        appendLine("public typealias T = Tr")

        return builder.toString()
    }

    private fun generateEntries(entries: Map<String, TranslationEntry>, keyPrefix: String) {
        val sortedEntries = entries.entries.sortedBy { it.key }

        for ((key, entry) in sortedEntries) {
            val fullKey = if (keyPrefix.isEmpty()) key else "$keyPrefix.$key"

            when (entry) {
                is TranslationEntry.Simple -> {
                    val funcName = escapeName(toCamelCase(key))
                    appendLine("/** `$fullKey` */")
                    appendLine("public fun $funcName(): kotlin.String = lookup(\"$fullKey\")")
                    appendLine()
                }

                is TranslationEntry.Parameterized -> {
                    val funcName = escapeName(toCamelCase(key))
                    val params = (0 until entry.paramCount).joinToString(", ") { "arg$it: kotlin.Any" }
                    val args = (0 until entry.paramCount).joinToString(", ") { "arg$it" }
                    appendLine("/** `$fullKey` - Template: `${entry.value}` */")
                    appendLine("public fun $funcName($params): kotlin.String = lookup(\"$fullKey\", $args)")
                    appendLine()
                }

                is TranslationEntry.Plural -> {
                    val funcName = escapeName(toCamelCase(key))
                    // Sprawdź czy formy pluralne mają parametry (poza {0} który jest count)
                    val sampleForm = entry.forms.values.firstOrNull() ?: ""
                    val extraParams = PARAM_REGEX.findAll(sampleForm)
                        .map { it.groupValues[1].toInt() }
                        .filter { it > 0 }
                        .toList()

                    if (extraParams.isNotEmpty()) {
                        val maxParam = extraParams.max()
                        val params = (1..maxParam).joinToString(", ") { "arg$it: kotlin.Any" }
                        val args = (1..maxParam).joinToString(", ") { "arg$it" }
                        appendLine("/** `$fullKey` (plural) */")
                        appendLine("public fun $funcName(count: kotlin.Int, $params): kotlin.String = lookupPlural(\"$fullKey\", count, $args)")
                    } else {
                        appendLine("/** `$fullKey` (plural) */")
                        appendLine("public fun $funcName(count: kotlin.Int): kotlin.String = lookupPlural(\"$fullKey\", count)")
                    }
                    appendLine()
                }

                is TranslationEntry.Nested -> {
                    val objectName = escapeName(toPascalCase(key))
                    appendLine("public object $objectName {")
                    indentLevel++
                    generateEntries(entry.children, fullKey)
                    indentLevel--
                    appendLine("}")
                    appendLine()
                }
            }
        }
    }
}