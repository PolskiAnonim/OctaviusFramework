package org.octavius.localization

import kotlinx.serialization.json.*
import org.octavius.config.Config

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
object Translations {

    /** Konfiguracja JSON do parsowania plików tłumaczeń */
    private val json = Json { ignoreUnknownKeys = true }
    
    /** Załadowane tłumaczenia jako drzewo JSON */
    private var translations: JsonElement

    /**
     * Inicjalizacja systemu tłumaczeń.
     * 
     * Ładuje plik tłumaczeń na podstawie kodu języka z EnvConfig.
     * Nazwa pliku: "translations_{languageCode}.json"
     * 
     * W przypadku błędu (brak pliku, błąd parsowania) ustawia pusty
     * obiekt JSON aby zapobiec crashom aplikacji.
     */
    init {
        translations = loadTranslations()
    }

    private fun loadTranslations(): JsonObject {
        val languageCode = Config.language
        val fileName = "translations_$languageCode.json"
        var combinedJson = JsonObject(emptyMap())

        try {
            // Używamy getResources (w liczbie mnogiej), aby znaleźć wszystkie pasujące pliki w classpath
            val resourceUrls = Thread.currentThread().contextClassLoader.getResources(fileName)

            for (url in resourceUrls) {
                val jsonString = url.readText()
                val jsonElement = json.parseToJsonElement(jsonString)

                if (jsonElement is JsonObject) {
                    // Łączymy nowo wczytany JSON z już istniejącym
                    combinedJson = mergeJsonObjects(combinedJson, jsonElement)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            // W razie błędu zwracamy to, co udało się załadować do tej pory, lub pusty obiekt
            return combinedJson.ifEmpty { JsonObject(emptyMap()) }
        }
        return combinedJson
    }

    /**
     * Rekursywnie łączy dwa obiekty JSON.
     * Jeśli klucz istnieje w obu obiektach i wartością jest kolejny obiekt,
     * są one również łączone. W przeciwnym razie wartość z `source` nadpisuje `target`.
     */
    private fun mergeJsonObjects(target: JsonObject, source: JsonObject): JsonObject {
        val result = target.toMutableMap()

        source.forEach { (key, sourceValue) ->
            val targetValue = target[key]

            // Jeśli oba klucze wskazują na obiekty JSON, połącz je rekursywnie
            if (targetValue is JsonObject && sourceValue is JsonObject) {
                result[key] = mergeJsonObjects(targetValue, sourceValue)
            } else {
                // W przeciwnym razie, wartość ze źródła (później załadowanego modułu) wygrywa
                result[key] = sourceValue
            }
        }
        return JsonObject(result)
    }

    /**
     * Pobiera tłumaczenie dla podanego klucza z opcjonalnymi parametrami.
     *
     * Obsługuje zagnieżdżone klucze using dot notation (np. "navigation.back").
     * Parametry w szablonie są oznaczone jako {0}, {1}, {2} itp.
     *
     * @param key Klucz tłumaczenia w formacie dot notation
     * @param args Parametry do wstawienia w szablon
     * @return Przetłumaczony i sformatowany tekst, lub oryginalny klucz jeśli nie znaleziono tłumaczenia
     *
     * Przykłady:
     * ```kotlin
     * Translations.get("navigation.back") // "Powrót"
     * Translations.get("welcome.user", "Jan") // "Witaj, Jan!"
     * Translations.get("missing.key") // "missing.key"
     * ```
     */
    fun get(key: String, vararg args: Any): String {
        val template = findValue(key)?.let {
            (it as? JsonPrimitive)?.contentOrNull
        } ?: return key // Zwróć klucz, jeśli nie znaleziono tłumaczenia

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
     * Struktura JSON:
     * ```json
     * {
     *   "items": {
     *     "one": "Znaleziono {0} element",
     *     "few": "Znaleziono {0} elementy",
     *     "many": "Znaleziono {0} elementów"
     *   }
     * }
     * ```
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

        val template = (pluralForms[pluralKey] ?: pluralForms["many"])
            ?.let { (it as? JsonPrimitive)?.contentOrNull }
            ?: return key

        return formatString(template, count, *args)
    }

    /**
     * Znajduje wartość w drzewie JSON using dot notation.
     * 
     * Przechodzi przez ścieżkę zagnieżdżonych obiektów aby znaleźć
     * odpowiedni element JSON.
     * 
     * @param key Klucz w formacie dot notation (np. "navigation.back")
     * @return Element JSON lub null jeśli nie znaleziono
     */
    private fun findValue(key: String): JsonElement? {
        val path = key.split('.')
        var currentElement: JsonElement? = translations

        for (segment in path) {
            if (currentElement !is JsonObject) return null
            currentElement = currentElement[segment]
        }
        return currentElement
    }

    /**
     * Formatuje szablon tekstu zamieniając placeholder na wartości parametrów.
     * 
     * Placeholders są w formacie {0}, {1}, {2} itp. i są zamieniane
     * na odpowiednie parametry w kolejności.
     * 
     * @param template Szablon tekstu z placeholders
     * @param args Parametry do wstawienia
     * @return Sformatowany tekst
     * 
     * Przykład:
     * ```kotlin
     * formatString("Witaj, {0}! Masz {1} wiadomości.", "Jan", 5)
     * // "Witaj, Jan! Masz 5 wiadomości."
     * ```
     */
    private fun formatString(template: String, vararg args: Any): String {
        var result = template
        args.forEachIndexed { index, arg ->
            result = result.replace("{$index}", arg.toString())
        }
        return result
    }
}