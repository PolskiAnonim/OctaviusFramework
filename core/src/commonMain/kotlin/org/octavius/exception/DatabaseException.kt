package org.octavius.exception

import org.octavius.data.transaction.TransactionStep

/**
 * Bazowy, zapieczętowany wyjątek dla wszystkich błędów warstwy danych.
 *
 * Wszystkie wyjątki związane z dostępem do bazy danych dziedziczą z tej klasy,
 * co umożliwia łatwe łapanie i obsługę błędów na różnych poziomach aplikacji.
 */
sealed class DatabaseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Błędy związane z ładowaniem lub używaniem TypeRegistry.
 *
 * Rzucane gdy:
 * - Nie można znaleźć klasy oznaczonej @PgType
 * - Brak mapowania między typem PostgreSQL a klasą Kotlin
 * - Błąd podczas skanowania classpath lub bazy danych
 */
class TypeRegistryException(message: String, cause: Throwable? = null) : DatabaseException(message, cause)

/**
 * Błędy podczas konwersji danych między typami Kotlin a PostgreSQL.
 *
 * Zawiera szczegółowe informacje o wartości, która nie mogła być skonwertowana,
 * oraz docelowym typie. Rzucane podczas:
 * - Konwersji wartości z bazy na typy Kotlin
 * - Ekspansji parametrów Kotlin na konstrukcje SQL
 */
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

/**
 * Błędy podczas wykonywania zapytań SQL.
 *
 * Zawiera pełny kontekst błędu: zapytanie SQL i parametry.
 * Rzucane gdy:
 * - Zapytanie SQL jest niepoprawne składniowo
 * - Naruszenie ograniczeń bazy danych
 * - Błędy połączenia z bazą danych
 */
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

/**
 * Błędy podczas mapowania wiersza ResultSet na obiekt data class.
 *
 * Zawiera informacje o klasie docelowej i danych wiersza, które nie mogły być zmapowane.
 * Rzucane gdy:
 * - Brak odpowiedniego konstruktora w data class
 * - Niezgodność typów między kolumnami a właściwościami klasy
 * - Błąd podczas tworzenia instancji obiektu
 */
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

/**
 * Wyjątek rzucany, gdy wykonanie konkretnego kroku w ramach transakcji wsadowej nie powiedzie się.
 *
 * Opakowuje oryginalny wyjątek (np. QueryExecutionException), dodając kontekst
 * o tym, który krok zawiódł.
 *
 * @param stepIndex Indeks (0-based) kroku, który się nie powiódł.
 * @param failedStep Sam obiekt kroku, który się nie powiódł, dla celów diagnostycznych.
 * @param cause Oryginalny wyjątek, który spowodował błąd.
 */
class BatchStepExecutionException(
    val stepIndex: Int,
    val failedStep: TransactionStep<*>,
    override val cause: Throwable
) : DatabaseException(
    // Tworzymy bardziej opisową wiadomość
    "Execution of transaction step $stepIndex failed. Cause: ${cause.message}",
    cause
)