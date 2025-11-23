package org.octavius.data.exception

//---------------------------------------------TypeRegistryException----------------------------------------------------

enum class TypeRegistryExceptionMessage {
    // Loading errors
    INITIALIZATION_FAILED,       // General top-level loading error
    CLASSPATH_SCAN_FAILED,       // Error during annotation scanning
    DB_QUERY_FAILED,             // Error querying database for types
    // Access / inconsistency errors
    PG_TYPE_NOT_FOUND,           // PostgreSQL type with given name not found
    KOTLIN_CLASS_NOT_MAPPED,     // Kotlin class is not mapped to any PG type
    PG_TYPE_NOT_MAPPED,          // PG type is not mapped to any Kotlin class
    DYNAMIC_TYPE_NOT_FOUND,      // Class for dynamic type not found
    WRONG_FIELD_NUMBER_IN_COMPOSITE, // Wrong number of fields in composite
}

class TypeRegistryException(
    val messageEnum: TypeRegistryExceptionMessage,
    val typeName: String? = null, // PG type name or Kotlin class name related to the problem
    cause: Throwable? = null
) : DatabaseException(messageEnum.name, cause) {
    override fun toString(): String {
        return """
        -------------------------------
        |     TYPE REGISTRY FAILED     
        | message: ${generateDeveloperMessage(this.messageEnum, typeName) }
        | typeName: $typeName
        ---------------------------------
        """.trimIndent()
    }
}

private fun generateDeveloperMessage(messageEnum: TypeRegistryExceptionMessage, typeName: String?): String {
    return when (messageEnum) {
        TypeRegistryExceptionMessage.INITIALIZATION_FAILED -> "Critical error: Failed to initialize TypeRegistry."
        TypeRegistryExceptionMessage.CLASSPATH_SCAN_FAILED -> "Failed to scan classpath for annotations."
        TypeRegistryExceptionMessage.DB_QUERY_FAILED -> "Failed to fetch type definitions from database."
        TypeRegistryExceptionMessage.PG_TYPE_NOT_FOUND -> "PostgreSQL type not found in registry: '$typeName'."
        TypeRegistryExceptionMessage.KOTLIN_CLASS_NOT_MAPPED -> "Class '$typeName' is not a registered PostgreSQL type. Check the @PgType annotation."
        TypeRegistryExceptionMessage.PG_TYPE_NOT_MAPPED -> "No mapped Kotlin class found for PostgreSQL type: '$typeName'."
        TypeRegistryExceptionMessage.DYNAMIC_TYPE_NOT_FOUND -> "No registered class found for dynamic type: '$typeName'."
        TypeRegistryExceptionMessage.WRONG_FIELD_NUMBER_IN_COMPOSITE -> "Composite '$typeName' from database has a different number of fields than in the registry."
    }
}