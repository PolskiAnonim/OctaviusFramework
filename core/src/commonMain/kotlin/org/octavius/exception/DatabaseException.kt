package org.octavius.exception

/** Bazowy, zapieczętowany wyjątek dla wszystkich błędów warstwy danych. */
sealed class DatabaseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/** Błędy związane z ładowaniem lub używaniem TypeRegistry (błąd konfiguracji). */
class TypeRegistryException(message: String, cause: Throwable? = null) : DatabaseException(message, cause)

/** Błędy podczas konwersji danych między Kotlin a PostgreSQL. */
class DataConversionException(
    message: String,
    val value: Any?,
    val targetType: String,
    cause: Throwable? = null
) : DatabaseException(message, cause) {
    override fun toString(): String {
        return """${super.toString()}
        |   Original Value: $value
        |   Target Type: $targetType
        """.trimMargin()
    }
}

/** Błędy podczas wykonywania zapytań SQL. */
class QueryExecutionException(
    message: String,
    val sql: String,
    val params: Map<String, Any?>,
    cause: Throwable? = null
) : DatabaseException(message, cause) {
    override fun toString(): String {
        return """${super.toString()}
        |   SQL: $sql
        |   Params: $params
        """.trimMargin()
    }
}

/** Błędy podczas mapowania wiersza ResultSet na obiekt data class. */
class DataMappingException(
    message: String,
    val targetClass: String,
    val rowData: Map<String, Any?>, // Mapa z danymi wiersza, który się nie powiódł
    cause: Throwable? = null
) : DatabaseException(message, cause) {
    override fun toString(): String {
        return """${super.toString()}
        |   Target Class: $targetClass
        |   Problematic Row Data: $rowData
        """.trimMargin()
    }
}

/**
 * Rzucany, gdy referencja do wyniku z poprzedniego kroku (`DatabaseValue.FromStep`)
 * jest nieprawidłowa i nie może zostać rozwiązana.
 *
 * @param message Komunikat błędu.
 * @param referencedStepIndex Indeks kroku, do którego odnosiła się referencja.
 * @param missingKey Klucz, którego nie udało się znaleźć w wyniku (jeśli dotyczy).
 */
class StepDependencyException(
    override val message: String,
    val referencedStepIndex: Int,
    val missingKey: String? = null
) : DatabaseException(message) {
    override fun toString(): String {
        return """${super.toString()}
        |   Step Index: $referencedStepIndex
        |   Missing Key: $missingKey
        """.trimMargin()
    }
}