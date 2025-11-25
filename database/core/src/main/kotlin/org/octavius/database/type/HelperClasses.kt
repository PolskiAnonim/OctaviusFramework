package org.octavius.database.type

import org.octavius.data.util.CaseConvention

// --- Modele danych ---

internal enum class TypeCategory {
    STANDARD, ENUM, COMPOSITE, ARRAY, DYNAMIC
}

internal data class PgEnumDefinition(
    val typeName: String,
    val values: List<String>,
    val pgConvention: CaseConvention,
    val kotlinConvention: CaseConvention,
    val classFullPath: String // FQN klasy
)

internal data class PgCompositeDefinition(
    val typeName: String,
    val attributes: Map<String, String>, // colName -> colType
    val classFullPath: String // FQN klasy
)

internal data class PgArrayDefinition(
    val typeName: String,
    val elementTypeName: String
)