package org.octavius.data.exception

//---------------------------------------------TypeRegistryException----------------------------------------------------

enum class TypeRegistryExceptionMessage {
    // Błędy ładowania
    INITIALIZATION_FAILED,       // Ogólny, nadrzędny błąd ładowania
    CLASSPATH_SCAN_FAILED,       // Błąd podczas skanowania adnotacji
    DB_QUERY_FAILED,             // Błąd podczas odpytywania bazy o typy
    // Błędy dostępu / niespójności
    PG_TYPE_NOT_FOUND,           // Nie znaleziono typu PG o podanej nazwie
    KOTLIN_CLASS_NOT_MAPPED,     // Klasa Kotlina nie jest zmapowana na żaden typ PG
    PG_TYPE_NOT_MAPPED,          // Typ PG nie jest zmapowany na żadną klasę Kotlina
    DYNAMIC_TYPE_NOT_FOUND,       // Nie znaleziono klasy dla dynamicznego typu
    WRONG_FIELD_NUMBER_IN_COMPOSITE, // Zła ilość pól w kompozycie
}

class TypeRegistryException(
    val messageEnum: TypeRegistryExceptionMessage,
    val typeName: String? = null, // Nazwa typu PG lub klasy Kotlina, której dotyczy problem
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
        TypeRegistryExceptionMessage.INITIALIZATION_FAILED -> "Krytyczny błąd: Nie udało się zainicjalizować TypeRegistry."
        TypeRegistryExceptionMessage.CLASSPATH_SCAN_FAILED -> "Nie udało się przeskanować classpath w poszukiwaniu adnotacji."
        TypeRegistryExceptionMessage.DB_QUERY_FAILED -> "Nie udało się pobrać definicji typów z bazy danych."
        TypeRegistryExceptionMessage.PG_TYPE_NOT_FOUND -> "Nie znaleziono w rejestrze typu PostgreSQL: '$typeName'."
        TypeRegistryExceptionMessage.KOTLIN_CLASS_NOT_MAPPED -> "Klasa '$typeName' nie jest zarejestrowanym typem PostgreSQL. Sprawdź adnotację @PgType."
        TypeRegistryExceptionMessage.PG_TYPE_NOT_MAPPED -> "Nie znaleziono zmapowanej klasy Kotlina dla typu PostgreSQL: '$typeName'."
        TypeRegistryExceptionMessage.DYNAMIC_TYPE_NOT_FOUND -> "Nie znaleziono zarejestrowanej klasy dla dynamicznego typu: '$typeName'."
        TypeRegistryExceptionMessage.WRONG_FIELD_NUMBER_IN_COMPOSITE -> "Kompozyt '$typeName' z bazy posiada inną ilość pól niż w rejestrze"
    }
}