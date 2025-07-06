package org.octavius.database

import kotlinx.serialization.json.JsonObject
import org.octavius.util.Converters
import org.postgresql.util.PGobject
import kotlin.reflect.full.memberProperties

/**
 * Helper do ekspansji złożonych parametrów PostgreSQL w zapytaniach SQL.
 *
 * Główna klasa odpowiedzialna za automatyczne przekształcanie złożonych typów Kotlin
 * na odpowiednie konstrukcje SQL PostgreSQL. Umożliwia używanie w zapytaniach SQL
 * zaawansowanych typów danych bez manualnej konwersji.
 *
 * Obsługiwane typy:
 * - **List<T>**: Konwersja na ARRAY[param1, param2, ...] PostgreSQL
 * - **Data class**: Konwersja na ROW(field1, field2, ...)::type_name
 * - **Enum**: Konwersja na PGobject z odpowiednim typem i wartością
 * - **JsonObject**: Konwersja na JSONB PostgreSQL
 * - **Zagnieżdżone struktury**: Rekurencyjne przetwarzanie złożonych typów
 *
 * @see ExpandedQuery
 * @see DatabaseManager
 */

/**
 * Wynik ekspansji zapytania SQL z parametrami.
 *
 * Kontener przechowujący wynik przetworzenia zapytania SQL przez [ParameterExpandHelper].
 * Zawiera zmodyfikowane zapytanie SQL oraz mapę parametrów gotowych do użycia
 * w prepared statements.
 *
 * @param expandedSql Zapytanie SQL z rozszerzonymi placeholderami (np. ARRAY[], ROW())
 * @param expandedParams Mapa parametrów do podstawienia w zapytaniu
 */
data class ExpandedQuery(
    val expandedSql: String,
    val expandedParams: Map<String, Any?>
)

/**
 * Klasa pomocnicza do ekspansji złożonych parametrów PostgreSQL.
 *
 * Główna implementacja logiki konwersji złożonych typów Kotlin na konstrukcje SQL PostgreSQL.
 * Umożliwia bezproblemowe używanie zaawansowanych typów danych w zapytaniach SQL bez
 * manualnej konwersji.
 *
 * **Obsługiwane przekształcenia:**
 * - **List<T>** → ARRAY[param1, param2, ...] PostgreSQL
 * - **Data class** → ROW(field1, field2, ...)::type_name
 * - **Enum** → PGobject z odpowiednim typem i wartością (CamelCase → snake_case)
 * - **JsonObject** → JSONB PostgreSQL
 * - **Zagnieżdżone struktury** → Rekurencyjne przetwarzanie (listy w listach, data class w listach)
 *
 * **Przykład użycia:**
 * ```kotlin
 * val helper = ParameterExpandHelper()
 * val result = helper.expandParametersInQuery(
 *     "SELECT * FROM users WHERE id = ANY(:ids) AND status = :status",
 *     mapOf(
 *         "ids" to listOf(1, 2, 3),
 *         "status" to UserStatus.Active
 *     )
 * )
 * // Wynik: "SELECT * FROM users WHERE id = ANY(ARRAY[:ids_p1, :ids_p2, :ids_p3]) AND status = :status"
 * ```
 */
class ParameterExpandHelper {

    /**
     * Główna metoda ekspansji parametrów w zapytaniu SQL.
     *
     * Punkt wejścia dla procesu konwersji. Przeszukuje zapytanie SQL w poszukiwaniu
     * named parameters (format ":param") i zamienia złożone typy na odpowiednie
     * konstrukcje PostgreSQL. Dla każdego parametru określa jego typ i deleguje
     * konwersję do odpowiedniej funkcji specjalistycznej.
     *
     * @param sql Zapytanie SQL z named parameters (np. ":param", ":ids")
     * @param params Mapa parametrów do ekspansji (nazwa → wartość)
     * @return ExpandedQuery z rozszerzonym SQL i parametrami gotowymi do wykonania
     */
    fun expandParametersInQuery(sql: String, params: Map<String, Any?>): ExpandedQuery {
        var expandedSql = sql
        val expandedParams = mutableMapOf<String, Any?>()

        params.forEach { (paramName, paramValue) ->
            val placeholder = ":$paramName"
            if (expandedSql.contains(placeholder)) {
                // Ekspanduj parametr na odpowiednią konstrukcję SQL
                val (newPlaceholder, newParams) = expandParameter(paramName, paramValue)
                expandedSql = expandedSql.replace(placeholder, newPlaceholder)
                expandedParams.putAll(newParams)
            } else {
                expandedParams[paramName] = paramValue
            }
        }

        return ExpandedQuery(expandedSql, expandedParams)
    }

    /**
     * Ekspanduje pojedynczy parametr na odpowiedni placeholder i dodatkowe parametry.
     *
     * Analizuje typ parametru i deleguje konwersję do odpowiedniej funkcji specjalistycznej.
     * Każdy typ ma swoją unikalną logikę konwersji na konstrukcje SQL PostgreSQL.
     *
     * @param paramName Nazwa parametru (używana do generowania unikalnych nazw)
     * @param paramValue Wartość parametru do ekspansji
     * @return Para: placeholder SQL i mapa dodatkowych parametrów
     */
    private fun expandParameter(paramName: String, paramValue: Any?): Pair<String, Map<String, Any?>> {
        return when {
            paramValue is List<*> -> expandArrayParameter(paramName, paramValue)
            isDataClass(paramValue) -> expandRowParameter(paramName, paramValue!!)
            paramValue is JsonObject -> createJsonParameter(paramName, paramValue)
            paramValue is Enum<*> -> createEnumParameter(paramName, paramValue)
            else -> ":$paramName" to mapOf(paramName to paramValue)
        }
    }

    /**
     * Tworzy parametr dla wartości JSON.
     *
     * Konwertuje JsonObject na PGobject typu JSONB dla PostgreSQL.
     *
     * @param paramName Nazwa parametru
     * @param paramValue Obiekt JSON do konwersji
     * @return Para: placeholder i parametr PGobject
     */
    private fun createJsonParameter(paramName: String, paramValue: JsonObject): Pair<String, Map<String, Any?>> {
        val pgObject = PGobject().apply {
            value = paramValue.toString()
            type = "jsonb"
        }
        return ":$paramName" to mapOf(paramName to pgObject)
    }

    /**
     * Tworzy parametr dla wartości enum.
     *
     * Konwertuje enum Kotlin na PGobject z odpowiednim typem PostgreSQL.
     * Realizuje mapowanie między konwencjami nazewniczymi:
     * - Nazwa enum: CamelCase → snake_case (np. "UserStatus" → "user_status")
     * - Wartość enum: CamelCase → SNAKE_CASE (np. "NotStarted" → "NOT_STARTED")
     *
     * @param paramName Nazwa parametru
     * @param enumValue Wartość enum do konwersji
     * @return Para: placeholder i parametr PGobject
     */
    private fun createEnumParameter(paramName: String, enumValue: Enum<*>): Pair<String, Map<String, Any?>> {
        val pgObject = PGobject().apply {
            value = Converters.camelToSnakeCase(enumValue.name).uppercase()
            type = Converters.camelToSnakeCase(enumValue::class.simpleName ?: "")
        }
        return ":$paramName" to mapOf(paramName to pgObject)
    }

    /**
     * Ekspanduje parametr tablicowy na konstrukcję ARRAY[].
     *
     * Konwertuje List<T> na konstrukcję ARRAY[elem1, elem2, ...] PostgreSQL.
     * Rekurencyjnie ekspanduje każdy element listy, obsługując zagnieżdżone
     * struktury (listy w listach, data class w listach, enum w listach).
     * Puste listy są konwertowane na literał '{}' PostgreSQL.
     *
     * @param paramName Nazwa parametru tablicowego (używana do generowania nazw elementów)
     * @param arrayValue Lista elementów do ekspansji
     * @return Para: placeholder ARRAY[] i mapa parametrów elementów
     */
    private fun expandArrayParameter(paramName: String, arrayValue: List<*>): Pair<String, Map<String, Any?>> {
        if (arrayValue.isEmpty()) {
            return "'{}'" to emptyMap() // Pusta tablica PostgreSQL
        }

        val expandedParams = mutableMapOf<String, Any?>()
        val placeholders = arrayValue.mapIndexed { index, value ->
            val elementParamName = "${paramName}_p${index + 1}" // Unikalna nazwa dla każdego elementu
            val (placeholder, params) = expandParameter(elementParamName, value)
            expandedParams.putAll(params)
            placeholder
        }

        val arrayPlaceholder = "ARRAY[${placeholders.joinToString(", ")}]"
        return arrayPlaceholder to expandedParams
    }

    /**
     * Ekspanduje data class na konstrukcję ROW()::type_name.
     *
     * Konwertuje data class Kotlin na konstrukcję ROW(field1, field2, ...)::type_name PostgreSQL.
     * Używa Kotlin reflection do pobrania wszystkich właściwości data class
     * i rekurencyjnie ekspanduje każdą właściwość. Nazwa typu jest konwertowana
     * z CamelCase na snake_case.
     *
     * @param paramName Nazwa parametru kompozytowego (używana do generowania nazw pól)
     * @param compositeValue Instancja data class do ekspansji
     * @return Para: placeholder ROW()::type i mapa parametrów pól
     */
    private fun expandRowParameter(paramName: String, compositeValue: Any): Pair<String, Map<String, Any?>> {
        val kClass = compositeValue::class
        val properties = kClass.memberProperties

        val expandedParams = mutableMapOf<String, Any?>()
        val placeholders = properties.mapIndexed { index, property ->
            val value = property.getter.call(compositeValue)
            val fieldParamName = "${paramName}_f${index + 1}" // Unikalna nazwa dla każdego pola
            val (placeholder, params) = expandParameter(fieldParamName, value)
            expandedParams.putAll(params)
            placeholder
        }

        val rowPlaceholder =
            "ROW(${placeholders.joinToString(", ")})::${Converters.camelToSnakeCase(kClass.simpleName ?: "")}"
        return rowPlaceholder to expandedParams
    }

    /**
     * Sprawdza czy obiekt jest data class.
     *
     * Pomocnicza funkcja do identyfikacji data class Kotlin. Data class
     * mają specjalną właściwość isData w kotlin-reflect.
     *
     * @param obj Obiekt do sprawdzenia
     * @return true jeśli obiekt jest data class, false w przeciwnym razie
     */
    private fun isDataClass(obj: Any?): Boolean {
        return obj != null && obj::class.isData
    }
}