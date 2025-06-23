package org.octavius.localization

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import org.octavius.config.EnvConfig

object Translations {

    private val json = Json { ignoreUnknownKeys = true }
    private var translations: JsonElement


    init {
        val languageCode = EnvConfig.language

        try {
            val fileName ="translations_$languageCode.json"
            val jsonString = this::class.java.classLoader.getResource(fileName)!!.readText()
            translations = json.parseToJsonElement(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            // W przypadku błędu, ustawiamy pusty obiekt JSON, by uniknąć crashy
            translations = JsonObject(emptyMap())
        }
    }

    /**
     * Zwraca przetłumaczony tekst.
     */
    fun get(key: String, vararg args: Any): String {
        val template = findValue(key)?.let {
            (it as? JsonPrimitive)?.contentOrNull
        } ?: return key // Zwróć klucz, jeśli nie znaleziono tłumaczenia

        return formatString(template, *args)
    }

    /**
     * Zwraca poprawną formę mnogą.
     */
    fun getPlural(key: String, count: Int, vararg args: Any): String {
        val pluralForms = findValue(key) as? JsonObject ?: return key

        val pluralKey = when {
            count == 1 -> "one"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "few"
            else -> "many"
        }

        val template = (pluralForms[pluralKey] ?: pluralForms["many"])
            ?.let { (it as? JsonPrimitive)?.contentOrNull }
            ?: return key

        return formatString(template, count, *args)
    }

    private fun findValue(key: String): JsonElement? {
        val path = key.split('.')
        var currentElement: JsonElement? = translations

        for (segment in path) {
            if (currentElement !is JsonObject) return null
            currentElement = currentElement[segment]
        }
        return currentElement
    }

    private fun formatString(template: String, vararg args: Any): String {
        var result = template
        args.forEachIndexed { index, arg ->
            result = result.replace("{$index}", arg.toString())
        }
        return result
    }
}