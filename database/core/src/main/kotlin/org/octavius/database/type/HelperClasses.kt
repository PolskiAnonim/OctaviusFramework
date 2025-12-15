package org.octavius.database.type

import kotlin.reflect.KClass

// --- Modele danych ---

internal enum class TypeCategory {
    STANDARD, ENUM, COMPOSITE, ARRAY, DYNAMIC
}

internal data class PgEnumDefinition(
    val typeName: String,
    val valueToEnumMap: Map<String, Enum<*>>,
    val kClass: KClass<out Enum<*>>
) {
    val enumToValueMap: Map<Enum<*>, String> = valueToEnumMap.map { it.value to it.key }.toMap()
}

internal data class PgCompositeDefinition(
    val typeName: String,
    val attributes: Map<String, String>, // colName -> colType
    val kClass: KClass<*>
)

internal data class PgArrayDefinition(
    val typeName: String,
    val elementTypeName: String
)