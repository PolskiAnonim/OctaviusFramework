package org.octavius.data.exception

import org.octavius.data.transaction.TransactionStep

/**
 * Bazowy, zapieczętowany wyjątek dla wszystkich błędów warstwy danych.
 *
 * Wszystkie wyjątki związane z dostępem do bazy danych dziedziczą z tej klasy,
 * co umożliwia łatwe łapanie i obsługę błędów na różnych poziomach aplikacji.
 */
sealed class DatabaseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

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
        val nestedError = cause.toString().prependIndent("|   ")
        return """

------------------------------------
|  QUERY EXECUTION FAILED     
| message: $message
| sql: $sql
| params: $params
-------------------------------------
| Szczegóły błędu w kroku:
$nestedError
-------------------------------------
"""
    }
}

/**
 * Wyjątek rzucany, gdy wykonanie konkretnego kroku w ramach transakcji wsadowej nie powiedzie się.
 *
 * Opakowuje oryginalny wyjątek (np. QueryExecutionException), dodając kontekst
 * o tym, który krok zawiódł.
 *
 * @param stepIndex Indeks (0-based) kroku, który się nie powiódł.
 * @param cause Oryginalny wyjątek, który spowodował błąd.
 */
class TransactionStepExecutionException(
    val stepIndex: Int,
    override val cause: Throwable
) : DatabaseException(
    "Execution of transaction step $stepIndex failed",
    cause
) {
    override fun toString(): String {
        val nestedError = cause.toString().prependIndent("|   ")

        return """

-------------------------------------
| TRANSACTION STEP $stepIndex FAILED
-------------------------------------
| Szczegóły błędu w kroku:
$nestedError
-------------------------------------
"""
    }
}