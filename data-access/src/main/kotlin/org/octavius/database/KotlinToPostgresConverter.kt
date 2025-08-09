package org.octavius.database

import kotlinx.serialization.json.JsonObject
import org.octavius.data.contract.PgTyped
import org.octavius.util.Converters
import org.postgresql.util.PGobject
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.primaryConstructor

/**
 * Wynik ekspansji zapytania.
 * @param expandedSql Zapytanie z rozszerzonymi placeholderami (np. `ARRAY[...]`, `ROW(...)`).
 * @param expandedParams Mapa spłaszczonych parametrów do użycia w `PreparedStatement`.
 */
data class ExpandedQuery(
    val expandedSql: String,
    val expandedParams: Map<String, Any?>
)

/**
 * Konwertuje złożone typy Kotlin na odpowiednie konstrukcje SQL dla PostgreSQL.
 *
 * Umożliwia używanie w zapytaniach zaawansowanych typów bez manualnej konwersji.
 * Obsługiwane transformacje:
 * - `List<T>` -> `ARRAY[...]`
 * - `data class` -> `ROW(...)::type_name`
 * - `Enum` -> `PGobject` (z mapowaniem `CamelCase` na `snake_case`)
 * - `JsonObject` -> `JSONB`
 * - `PgTyped` -> jak wyżej oraz dodaje rzutowanie `::type_name` - należy uważać na data class
 */
class KotlinToPostgresConverter {

    /**
     * Przetwarza zapytanie SQL, rozszerzając parametry złożone.
     *
     * @param sql Zapytanie z nazwanymi parametrami (np. `:param`).
     * @param params Mapa parametrów do ekspansji.
     * @return `ExpandedQuery` z przetworzonym SQL i parametrami.
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

    /** Rozszerza pojedynczy parametr, delegując do odpowiedniej funkcji. */
    private fun expandParameter(paramName: String, paramValue: Any?): Pair<String, Map<String, Any?>> {
        return when {
            paramValue is PgTyped -> {
                val (innerPlaceholder, innerParams) = expandParameter(paramName, paramValue.value)
                // 2. Do wyniku (który może być już np. "ARRAY[:p1, :p2]") doklej rzutowanie.
                val finalPlaceholder = innerPlaceholder + "::" + paramValue.pgType
                finalPlaceholder to innerParams
            }
            paramValue is List<*> -> expandArrayParameter(paramName, paramValue)
            isDataClass(paramValue) -> expandRowParameter(paramName, paramValue!!)
            paramValue is JsonObject -> createJsonParameter(paramName, paramValue)
            paramValue is Enum<*> -> createEnumParameter(paramName, paramValue)
            else -> ":$paramName" to mapOf(paramName to paramValue)
        }
    }

    /** Tworzy parametr `jsonb` z obiektu `JsonObject`. */
    private fun createJsonParameter(paramName: String, paramValue: JsonObject): Pair<String, Map<String, Any?>> {
        val pgObject = PGobject().apply {
            value = paramValue.toString()
            type = "jsonb"
        }
        return ":$paramName" to mapOf(paramName to pgObject)
    }

    /** Tworzy parametr dla enuma, mapując `CamelCase` na `snake_case` dla typu i wartości. */
    private fun createEnumParameter(paramName: String, enumValue: Enum<*>): Pair<String, Map<String, Any?>> {
        val pgObject = PGobject().apply {
            value = Converters.camelToSnakeCase(enumValue.name).uppercase()
            type = Converters.camelToSnakeCase(enumValue::class.simpleName ?: "")
        }
        return ":$paramName" to mapOf(paramName to pgObject)
    }

    /** Rozszerza listę na konstrukcję `ARRAY[...]`, rekurencyjnie przetwarzając elementy. */
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

    /** Rozszerza `data class` na konstrukcję `ROW(...)::type_name`, rekurencyjnie przetwarzając pola. */
    private fun expandRowParameter(paramName: String, compositeValue: Any): Pair<String, Map<String, Any?>> {
        val kClass = compositeValue::class

        // 1. Pobierz główny konstruktor klasy (dla zachowania kolejności)
        val constructor = kClass.primaryConstructor
            ?: throw IllegalArgumentException("Klasa ${kClass.simpleName} musi posiadać główny konstruktor, aby można ją było rozwinąć.")

        // 2. Stwórz mapę właściwości (KProperty) po nazwie, aby łatwo je odnaleźć.
        // Dostęp do wartości jest możliwy tylko przez KProperty, nie przez KParameter.
        val propertiesByName = kClass.memberProperties.associateBy { it.name }

        val expandedParams = mutableMapOf<String, Any?>()

        // 3. Iteruj po PARAMETRACH KONSTRUKTORA, a nie po właściwościach.
        // To gwarantuje prawidłową kolejność.
        val placeholders = constructor.parameters.mapIndexed { index, param ->
            // Znajdź właściwość odpowiadającą parametrowi konstruktora po nazwie.
            val property = propertiesByName[param.name]
                ?: throw IllegalStateException("Nie można znaleźć właściwości dla parametru konstruktora '${param.name}' w klasie ${kClass.simpleName}.")

            // Pobierz wartość tej właściwości z konkretnej instancji `compositeValue`.
            val value = property.getter.call(compositeValue)

            val fieldParamName = "${paramName}_f${index + 1}" // Unikalna nazwa dla każdego pola
            val (placeholder, params) = expandParameter(fieldParamName, value)
            expandedParams.putAll(params)
            placeholder
        }

        // Nazwa typu w bazie danych, np. z camelCase na snake_case
        val dbTypeName = kClass.simpleName?.let { Converters.camelToSnakeCase(it) } ?: ""
        val rowPlaceholder = "ROW(${placeholders.joinToString(", ")})::$dbTypeName"

        return rowPlaceholder to expandedParams
    }

    /** Sprawdza, czy obiekt jest instancją `data class`. */
    private fun isDataClass(obj: Any?): Boolean {
        return obj != null && obj::class.isData
    }
}