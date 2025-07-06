package org.octavius.database

import kotlinx.serialization.json.JsonObject
import org.octavius.util.Converters
import org.postgresql.util.PGobject
import kotlin.reflect.full.memberProperties

/**
 * Helper do ekspansji złożonych parametrów PostgreSQL w zapytaniach SQL.
 *
 * Automatycznie przekształca złożone typy Kotlin (List, data class, enum) na odpowiednie
 * konstrukcje SQL PostgreSQL (ARRAY[], ROW(), enum values) z dodatkowymi parametrami.
 */

/**
 * Wynik ekspansji zapytania SQL z parametrami.
 *
 * @param expandedSql Zapytanie SQL z rozszerzonymi placeholderami
 * @param expandedParams Mapa parametrów do podstawienia w zapytaniu
 */
data class ExpandedQuery(
    val expandedSql: String,
    val expandedParams: Map<String, Any?>
)

/**
 * Klasa pomocnicza do ekspansji złożonych parametrów PostgreSQL.
 *
 * Obsługuje automatyczne przekształcanie:
 * - List<T> na ARRAY[param1, param2, ...]
 * - Data class na ROW(field1, field2, ...)::type_name
 * - Enum na PGobject z odpowiednim typem i wartością
 * - Zagnieżdżone struktury (listy w listach, data class w listach)
 *
 * Przykład:
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
     * Przeszukuje zapytanie SQL w poszukiwaniu named parameters i zamienia
     * złożone typy na odpowiednie konstrukcje PostgreSQL.
     *
     * @param sql Zapytanie SQL z named parameters (np. ":param")
     * @param params Mapa parametrów do ekspansji
     * @return ExpandedQuery z rozszerzonym SQL i parametrami
     */
    fun expandParametersInQuery(sql: String, params: Map<String, Any?>): ExpandedQuery {
        var expandedSql = sql
        val expandedParams = mutableMapOf<String, Any?>()

        params.forEach { (paramName, paramValue) ->
            val placeholder = ":$paramName"
            if (expandedSql.contains(placeholder)) {
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
     * @param paramName Nazwa parametru
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
     * Nazwa enum i wartość są konwertowane z CamelCase na snake_case.
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
     * Rekurencyjnie ekspanduje każdy element listy, obsługując zagnieżdżone
     * struktury (listy w listach, data class w listach).
     *
     * @param paramName Nazwa parametru tablicowego
     * @param arrayValue Lista elementów do ekspansji
     * @return Para: placeholder ARRAY[] i mapa parametrów elementów
     */
    private fun expandArrayParameter(paramName: String, arrayValue: List<*>): Pair<String, Map<String, Any?>> {
        if (arrayValue.isEmpty()) {
            return "'{}'" to emptyMap()
        }

        val expandedParams = mutableMapOf<String, Any?>()
        val placeholders = arrayValue.mapIndexed { index, value ->
            val elementParamName = "${paramName}_p${index + 1}"
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
     * Używa Kotlin reflection do pobrania wszystkich właściwości data class
     * i rekurencyjnie ekspanduje każdą właściwość.
     *
     * @param paramName Nazwa parametru kompozytowego
     * @param compositeValue Instancja data class do ekspansji
     * @return Para: placeholder ROW()::type i mapa parametrów pól
     */
    private fun expandRowParameter(paramName: String, compositeValue: Any): Pair<String, Map<String, Any?>> {
        val kClass = compositeValue::class
        val properties = kClass.memberProperties

        val expandedParams = mutableMapOf<String, Any?>()
        val placeholders = properties.mapIndexed { index, property ->
            val value = property.getter.call(compositeValue)
            val fieldParamName = "${paramName}_f${index + 1}"
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
     * @param obj Obiekt do sprawdzenia
     * @return true jeśli obiekt jest data class
     */
    private fun isDataClass(obj: Any?): Boolean {
        return obj != null && obj::class.isData
    }
}