package org.octavius.gradle

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.gradle.api.Project
import org.gradle.api.tasks.TaskProvider
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Generuje type-safe klasy Kotlin na podstawie plików tłumaczeń JSON.
 *
 * Generowane pliki:
 * - `Translations{Lang}.kt` - płaskie mapy z tłumaczeniami per język
 * - `Tr.kt` - registry pattern + type-safe accessors
 *
 * Przykład użycia wygenerowanego kodu:
 * ```kotlin
 * Tr.Action.save()              // zamiast T.get("action.save")
 * Tr.Form.Actions.itemLabel(1)  // zamiast T.get("form.actions.itemLabel", 1)
 * Tr.Games.Form.category(5)     // zamiast T.getPlural("games.form.category", 5)
 *
 * // Zmiana języka w runtime
 * Tr.currentLanguage = "en"
 * ```
 */

private val PLURAL_KEYS = setOf("one", "few", "many")
private val PARAM_REGEX = Regex("""\{(\d+)\}""")

/**
 * Reprezentuje typ wpisu w JSON tłumaczeń.
 */
private sealed class TranslationEntry {
    /** Prosty tekst bez parametrów */
    data class Simple(val value: String) : TranslationEntry()

    /** Tekst z parametrami {0}, {1}, ... */
    data class Parameterized(val value: String, val paramCount: Int) : TranslationEntry()

    /** Forma pluralna (one/few/many) */
    data class Plural(val forms: Map<String, String>) : TranslationEntry()

    /** Zagnieżdżony obiekt */
    data class Nested(val children: Map<String, TranslationEntry>) : TranslationEntry()
}

/**
 * Parsuje mapę JSON do drzewa TranslationEntry.
 */
private fun parseTranslationMap(map: Map<String, Any?>): Map<String, TranslationEntry> {
    val result = mutableMapOf<String, TranslationEntry>()

    for ((key, value) in map) {
        result[key] = when (value) {
            is String -> {
                val params = PARAM_REGEX.findAll(value).map { it.groupValues[1].toInt() }.toList()
                if (params.isNotEmpty()) {
                    TranslationEntry.Parameterized(value, params.max() + 1)
                } else {
                    TranslationEntry.Simple(value)
                }
            }
            is Map<*, *> -> {
                @Suppress("UNCHECKED_CAST")
                val childMap = value as Map<String, Any?>

                // Sprawdź czy to forma pluralna
                if (childMap.keys.all { it in PLURAL_KEYS } && childMap.values.all { it is String }) {
                    @Suppress("UNCHECKED_CAST")
                    TranslationEntry.Plural(childMap as Map<String, String>)
                } else {
                    TranslationEntry.Nested(parseTranslationMap(childMap))
                }
            }
            else -> TranslationEntry.Simple(value?.toString() ?: "")
        }
    }

    return result
}

/**
 * Spłaszcza drzewo tłumaczeń do płaskich map.
 */
private fun flattenTranslations(
    entries: Map<String, TranslationEntry>,
    prefix: String = ""
): Pair<Map<String, String>, Map<String, Map<String, String>>> {
    val simple = mutableMapOf<String, String>()
    val plural = mutableMapOf<String, Map<String, String>>()

    for ((key, entry) in entries) {
        val fullKey = if (prefix.isEmpty()) key else "$prefix.$key"

        when (entry) {
            is TranslationEntry.Simple -> simple[fullKey] = entry.value
            is TranslationEntry.Parameterized -> simple[fullKey] = entry.value
            is TranslationEntry.Plural -> plural[fullKey] = entry.forms
            is TranslationEntry.Nested -> {
                val (childSimple, childPlural) = flattenTranslations(entry.children, fullKey)
                simple.putAll(childSimple)
                plural.putAll(childPlural)
            }
        }
    }

    return simple to plural
}

/**
 * Konwertuje snake_case lub kebab-case na PascalCase.
 */
private fun toPascalCase(input: String): String {
    return input.split("_", "-")
        .joinToString("") { word ->
            word.replaceFirstChar { it.uppercaseChar() }
        }
}

/**
 * Konwertuje string na camelCase (pierwsza litera mała).
 */
private fun toCamelCase(input: String): String {
    val pascal = toPascalCase(input)
    return pascal.replaceFirstChar { it.lowercaseChar() }
}

/**
 * Sprawdza czy nazwa jest zarezerwowanym słowem kluczowym Kotlin.
 */
private fun isKotlinKeyword(name: String): Boolean {
    val keywords = setOf(
        "as", "break", "class", "continue", "do", "else", "false", "for", "fun",
        "if", "in", "interface", "is", "null", "object", "package", "return",
        "super", "this", "throw", "true", "try", "typealias", "typeof", "val",
        "var", "when", "while"
    )
    return name in keywords
}

/**
 * Escapuje nazwę jeśli jest słowem kluczowym.
 */
private fun escapeName(name: String): String {
    return if (isKotlinKeyword(name)) "`$name`" else name
}

/**
 * Escapuje string do użycia w kodzie Kotlin.
 */
private fun escapeString(s: String): String {
    return s.replace("\\", "\\\\")
        .replace("\"", "\\\"")
        .replace("\n", "\\n")
        .replace("\r", "\\r")
        .replace("\t", "\\t")
        .replace("$", "\\$")
}

/**
 * Generuje plik z danymi tłumaczeń dla konkretnego języka.
 */
private class LanguageDataGenerator(private val packageName: String) {

    fun generate(lang: String, simple: Map<String, String>, plural: Map<String, Map<String, String>>): String {
        val langPascal = toPascalCase(lang)
        val builder = StringBuilder()

        builder.appendLine("@file:Suppress(\"unused\", \"RedundantVisibilityModifier\")")
        builder.appendLine()
        builder.appendLine("package $packageName")
        builder.appendLine()
        builder.appendLine("/**")
        builder.appendLine(" * Translation data for language: $lang")
        builder.appendLine(" *")
        builder.appendLine(" * This file is auto-generated. Do not edit manually.")
        builder.appendLine(" */")
        builder.appendLine("public object Translations$langPascal : TranslationData {")
        builder.appendLine()

        // Simple translations
        builder.appendLine("    override val simple: Map<String, String> = mapOf(")
        simple.entries.sortedBy { it.key }.forEachIndexed { index, (key, value) ->
            val comma = if (index < simple.size - 1) "," else ""
            builder.appendLine("        \"$key\" to \"${escapeString(value)}\"$comma")
        }
        builder.appendLine("    )")
        builder.appendLine()

        // Plural translations
        builder.appendLine("    override val plural: Map<String, PluralForms> = mapOf(")
        plural.entries.sortedBy { it.key }.forEachIndexed { index, (key, forms) ->
            val comma = if (index < plural.size - 1) "," else ""
            val one = forms["one"]?.let { "\"${escapeString(it)}\"" } ?: "null"
            val few = forms["few"]?.let { "\"${escapeString(it)}\"" } ?: "null"
            val many = forms["many"]?.let { "\"${escapeString(it)}\"" } ?: "\"$key\""
            builder.appendLine("        \"$key\" to PluralForms($one, $few, $many)$comma")
        }
        builder.appendLine("    )")
        builder.appendLine("}")

        return builder.toString()
    }
}

/**
 * Generuje główny plik Tr.kt z registry i type-safe accessorami.
 */
private class TrGenerator(private val packageName: String) {
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

/**
 * Rejestruje task `generateTranslationAccessors` w projekcie root.
 *
 * Task skanuje wszystkie moduły w poszukiwaniu plików translations_*.json,
 * merguje je w pamięci i generuje:
 * - Translations{Lang}.kt - płaskie mapy z danymi per język
 * - Tr.kt - registry + type-safe accessors
 *
 * @param coreProject Projekt core gdzie zostaną wygenerowane pliki
 * @param targetPackage Package dla wygenerowanego kodu
 */
fun Project.registerGenerateTranslationAccessorsTask(
    coreProject: Project,
    targetPackage: String = "org.octavius.localization"
): TaskProvider<*> {
    return tasks.register("generateTranslationAccessors") {
        group = "build"
        description = "Generates type-safe Kotlin accessors for translations."

        val outputDir = coreProject.layout.buildDirectory.dir("generated/kotlin/commonMain")
        outputs.dir(outputDir)

        // Zbieramy wszystkie pliki translations jako inputy
        rootProject.subprojects.forEach { subproject ->
            val srcDir = subproject.file("src")
            if (srcDir.exists()) {
                inputs.dir(srcDir)
            }
        }

        doLast {
            val gson = Gson()
            val mapType = object : TypeToken<Map<String, Any?>>() {}.type
            val mergedByLang = mutableMapOf<String, MutableMap<String, Any?>>()

            // Skanujemy wszystkie podprojekty
            rootProject.subprojects.forEach { subproject ->
                subproject.file("src").walk().forEach { file ->
                    if (file.isFile && file.name.startsWith("translations_") && file.name.endsWith(".json")) {
                        val lang = file.name.substringAfter("translations_").substringBefore(".json")
                        val content = file.readText(Charsets.UTF_8)
                        if (content.isNotBlank()) {
                            logger.info("Found translation for '$lang' in ${subproject.name}/${file.relativeTo(subproject.projectDir)}")
                            try {
                                val sourceMap: Map<String, Any?> = gson.fromJson(content, mapType)
                                val targetMap = mergedByLang.getOrPut(lang) { mutableMapOf() }
                                mergeJsonMaps(targetMap, sourceMap)
                            } catch (e: Exception) {
                                logger.error("Failed to parse translation file: ${file.path}", e)
                            }
                        }
                    }
                }
            }

            if (mergedByLang.isEmpty()) {
                logger.warn("No translation files found!")
                return@doLast
            }

            val packagePath = targetPackage.replace(".", "/")
            val outputDirFile = outputDir.get().asFile

            // Generuj pliki dla każdego języka
            for ((lang, translationMap) in mergedByLang) {
                logger.lifecycle("Generating translations for language: $lang")

                val entries = parseTranslationMap(translationMap)
                val (simpleMap, pluralMap) = flattenTranslations(entries)

                // Generuj Translations{Lang}.kt
                val langGenerator = LanguageDataGenerator(targetPackage)
                val langCode = langGenerator.generate(lang, simpleMap, pluralMap)
                val langFile = File(outputDirFile, "$packagePath/Translations${toPascalCase(lang)}.kt")
                langFile.parentFile.mkdirs()
                langFile.writeText(langCode, StandardCharsets.UTF_8)
                logger.lifecycle("Generated: ${langFile.path}")
            }

            // Generuj Tr.kt (używamy pierwszego języka jako domyślnego)
            val (defaultLang, defaultMap) = mergedByLang.entries.first()
            val entries = parseTranslationMap(defaultMap)

            val trGenerator = TrGenerator(targetPackage)
            val trCode = trGenerator.generate(entries, defaultLang)
            val trFile = File(outputDirFile, "$packagePath/Tr.kt")
            trFile.writeText(trCode, StandardCharsets.UTF_8)
            logger.lifecycle("Generated: ${trFile.path}")

            // Statystyki
            fun countFunctions(entries: Map<String, TranslationEntry>): Int {
                return entries.values.sumOf { entry ->
                    when (entry) {
                        is TranslationEntry.Nested -> countFunctions(entry.children)
                        else -> 1
                    }
                }
            }
            val functionCount = countFunctions(entries)
            val (simpleCount, pluralCount) = flattenTranslations(entries)
            logger.lifecycle("Generated $functionCount accessors (${simpleCount.size} simple, ${pluralCount.size} plural)")
        }
    }
}
