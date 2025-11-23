package org.octavius.data.exception

/**
 * Base sealed exception for all data layer errors.
 *
 * All database access related exceptions inherit from this class,
 * enabling easy catching and handling of errors at different application levels.
 */
sealed class DatabaseException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Errors during SQL query execution.
 *
 * Contains full error context: SQL query and parameters.
 * Thrown when:
 * - SQL query is syntactically incorrect
 * - Database constraint violation
 * - Database connection errors
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
| Step error details:
$nestedError
-------------------------------------
"""
    }
}

/**
 * Exception thrown when execution of a specific step within a batch transaction fails.
 *
 * Wraps the original exception (e.g., QueryExecutionException), adding context
 * about which step failed.
 *
 * @param stepIndex Index (0-based) of the step that failed.
 * @param cause Original exception that caused the error.
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
| Step error details:
$nestedError
-------------------------------------
"""
    }
}