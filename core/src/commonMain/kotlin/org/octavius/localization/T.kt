package org.octavius.localization

import kotlinx.serialization.json.*
import org.octavius.util.loadResource

// Ta stała może być w przyszłości dynamiczna, np. z ustawień
const val LANGUAGE = "pl"

/**
 * System lokalizacji aplikacji oparty na plikach JSON.
 * 
 * Zapewnia zaawansowane funkcje tłumaczeń w tym:
 * - Ładowanie tłumaczeń z plików JSON na podstawie konfiguracji języka
 * - Obsługa zagnieżdżonych kluczy (dot notation)
 * - Formatowanie szablonów z parametrami
 * - System pluralizacji (one/few/many)
 * - Fallback przy braku klucza
 */

/**
 * Singleton object zarządzający tłumaczeniami aplikacji.
 * 
 * Ładuje tłumaczenia przy starcie aplikacji na podstawie ustawienia języka
 * i udostępnia metody do pobierania przetłumaczonych tekstów.
 * 
 * Struktura pliku JSON:
 * ```json
 * {
 *   "navigation": {
 *     "back": "Powrót",
 *     "home": "Strona główna"
 *   },
 *   "messages": {
 *     "count": {
 *       "one": "Znaleziono {0} element",
 *       "few": "Znaleziono {0} elementy", 
 *       "many": "Znaleziono {0} elementów"
 *     }
 *   }
 * }
 * ```
 * 
 * Użycie:
 * ```kotlin
 * val text = Translations.get("navigation.back")
 * val formatted = Translations.get("welcome.user", userName)
 * val plural = Translations.getPlural("messages.count", itemCount)
 * ```
 */
object T {

    /** Konfiguracja JSON do parsowania plików tłumaczeń */
    private val json = Json { ignoreUnknownKeys = true }

    /**
     * Leniwie inicjalizowane, połączone tłumaczenia ze wszystkich modułów.
     * Ładowanie odbywa się tylko raz, przy pierwszym wywołaniu `Translations.get()` lub podobnej metody.
     */
    private val translations: JsonObject by lazy {
        loadMergedTranslations()
    }

    /**
     * Znajduje wszystkie pliki `translations_{język}.json`
     * parsuje je i łączy w jeden, spójny obiekt JsonObject.
     *
     * @return Połączony obiekt JsonObject zawierający wszystkie tłumaczenia.
     */
    private fun loadMergedTranslations(): JsonObject {
        val fileName = "translations_$LANGUAGE.json"

        val jsonString = loadResource(fileName)
        return if (jsonString.isNullOrBlank()) JsonObject(emptyMap())
        else json.parseToJsonElement(jsonString).jsonObject
    }

    /**
     * Pobiera tłumaczenie dla podanego klucza z opcjonalnymi parametrami.
     *
     * @param key Klucz tłumaczenia w formacie "dot.notation".
     * @param args Parametry do wstawienia w szablon ({0}, {1}, ...).
     * @return Przetłumaczony i sformatowany tekst, lub `key` jeśli nie znaleziono tłumaczenia.
     */
    fun get(key: String, vararg args: Any): String {
        val template = findValue(key)?.jsonPrimitive?.contentOrNull ?: return key
        return formatString(template, *args)
    }

    /**
     * Pobiera tłumaczenie z polską pluralizacją.
     * 
     * Automatycznie wybiera odpowiednią formę na podstawie liczby:
     * - "one": dla count == 1
     * - "few": dla count % 10 in 2..4 && count % 100 !in 12..14
     * - "many": dla pozostałych przypadków
     * 
     * Liczba jest automatycznie wstawiana jako pierwszy parametr {0}.
     * 
     * @param key Klucz tłumaczenia wskazujący na obiekt z formami plural
     * @param count Liczba determinująca formę plural
     * @param args Dodatkowe parametry do wstawienia w szablon (począwszy od {1})
     * @return Przetłumaczony tekst z odpowiednią formą plural
     *
     * 
     * Przykłady:
     * ```kotlin
     * Translations.getPlural("items", 1) // "Znaleziono 1 element"
     * Translations.getPlural("items", 3) // "Znaleziono 3 elementy"
     * Translations.getPlural("items", 15) // "Znaleziono 15 elementów"
     * ```
     */
    fun getPlural(key: String, count: Int, vararg args: Any): String {
        val pluralForms = findValue(key) as? JsonObject ?: return key

        val pluralKey = when {
            count == 1 -> "one"
            count % 10 in 2..4 && count % 100 !in 12..14 -> "few"
            else -> "many"
        }

        // Fallback do "many" jeśli specyficzna forma nie istnieje
        val template = (pluralForms[pluralKey] ?: pluralForms["many"])?.jsonPrimitive?.contentOrNull ?: return key
        return formatString(template, count, *args)
    }

    /**
     * Znajduje wartość w drzewie JSON używając "dot.notation".
     */
    private fun findValue(key: String): JsonElement? {
        val path = key.split('.')
        var currentElement: JsonElement? = translations

        for (segment in path) {
            currentElement = (currentElement as? JsonObject)?.get(segment) ?: return null
        }
        return currentElement
    }

    /**
     * Formatuje szablon tekstu zamieniając placeholder-y na wartości parametrów.
     */
    private fun formatString(template: String, vararg args: Any): String {
        if (args.isEmpty()) return template
        var result = template
        args.forEachIndexed { index, arg ->
            result = result.replace("{$index}", arg.toString())
        }
        return result
    }
}