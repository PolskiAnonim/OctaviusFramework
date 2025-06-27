package org.octavius.database

import org.octavius.util.Converters
import org.postgresql.util.PGobject
import kotlin.reflect.full.memberProperties

data class ExpandedQuery(
    val expandedSql: String,
    val expandedParams: Map<String, Any?>
)

class ParameterExpandHelper {
    
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
    
    private fun expandParameter(paramName: String, paramValue: Any?): Pair<String, Map<String, Any?>> {
        return when {
            paramValue is List<*> -> expandArrayParameter(paramName, paramValue)
            isDataClass(paramValue) -> expandRowParameter(paramName, paramValue!!)
            paramValue is Enum<*> -> createEnumParameter(paramName, paramValue)
            else -> ":$paramName" to mapOf(paramName to paramValue)
        }
    }
    
    private fun createEnumParameter(paramName: String, enumValue: Enum<*>): Pair<String, Map<String, Any?>> {
        val pgObject = PGobject().apply {
            value = Converters.camelToSnakeCase(enumValue.name).uppercase()
            type = Converters.camelToSnakeCase(enumValue::class.simpleName ?: "")
        }
        return ":$paramName" to mapOf(paramName to pgObject)
    }
    
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
        
        val rowPlaceholder = "ROW(${placeholders.joinToString(", ")})::${Converters.camelToSnakeCase(kClass.simpleName ?: "")}"
        return rowPlaceholder to expandedParams
    }
    
    private fun isDataClass(obj: Any?): Boolean {
        return obj != null && obj::class.isData
    }
}