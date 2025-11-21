package org.octavius.data.exception

enum class StepDependencyExceptionMessage {
    // Błędy walidacji planu
    DEPENDENCY_ON_FUTURE_STEP,
    UNKNOWN_STEP_HANDLE,

    // Błędy dostępu do wyników
    RESULT_NOT_FOUND,
    NULL_SOURCE_RESULT,
    ROW_INDEX_OUT_OF_BOUNDS,

    // Błędy dotyczące struktury wyników
    RESULT_NOT_LIST,
    RESULT_NOT_MAP_LIST,
    INVALID_ROW_ACCESS_ON_NON_LIST,

    // Błędy dostępu do kolumn/pól
    COLUMN_NOT_FOUND,
    SCALAR_NOT_FOUND,

    TRANSFORMATION_FAILED
}

// Wiadomość do logów
private fun generateDeveloperMessage(
    messageEnum: StepDependencyExceptionMessage,
    stepIndex: Int,
    args: Array<out Any>
): String {
    return when (messageEnum) {
        StepDependencyExceptionMessage.DEPENDENCY_ON_FUTURE_STEP -> "Step attempts to use a result from a future or current step ${args.getOrNull(0)}."
        StepDependencyExceptionMessage.UNKNOWN_STEP_HANDLE -> "Validation failed: Found a handle that doesn't exist in the plan."
        StepDependencyExceptionMessage.RESULT_NOT_FOUND -> "Result for source step $stepIndex not found. It might have not been executed yet."
        StepDependencyExceptionMessage.NULL_SOURCE_RESULT -> "Cannot extract data because the result of source step $stepIndex is null."
        StepDependencyExceptionMessage.ROW_INDEX_OUT_OF_BOUNDS -> "Row index ${args.getOrNull(0)} is out of bounds for result of step $stepIndex (size: ${args.getOrNull(1)})."
        StepDependencyExceptionMessage.RESULT_NOT_LIST -> "Expected the result of step $stepIndex to be a List, but it was not."
        StepDependencyExceptionMessage.RESULT_NOT_MAP_LIST -> "Expected the result of step $stepIndex to be a List of Maps (from toList()), but its elements are not Maps."
        StepDependencyExceptionMessage.INVALID_ROW_ACCESS_ON_NON_LIST -> "Cannot access row at index ${args.getOrNull(0)} because the result of step $stepIndex is not a List. Only index 0 is allowed."
        StepDependencyExceptionMessage.COLUMN_NOT_FOUND -> "Column '${args.getOrNull(0)}' not found in the result of step $stepIndex."
        StepDependencyExceptionMessage.SCALAR_NOT_FOUND -> "Attempted to get the default scalar value ('result') from step $stepIndex, but the result is not a scalar or is a map without such a key."
        StepDependencyExceptionMessage.TRANSFORMATION_FAILED -> "User-defined transformation (.map {}) failed for value derived from step $stepIndex. Cause: ${args.getOrNull(0)}"
    }
}

/**
 * Rzucany, gdy referencja do wyniku z poprzedniego kroku (`TransactionValue.FromStep`)
 * jest nieprawidłowa i nie może zostać rozwiązana.
 *
 * @param message Komunikat błędu.
 * @param referencedStepIndex Indeks kroku, do którego odnosiła się referencja.
 * @param args Argumenty przydatne dla wiadomości
 */
class StepDependencyException(
    val messageEnum: StepDependencyExceptionMessage,
    val referencedStepIndex: Int,
    vararg val args: Any,
    cause: Throwable? = null
) : DatabaseException(messageEnum.name, cause) {
    constructor(messageEnum: StepDependencyExceptionMessage, stepIndex: Int)
            : this(messageEnum, stepIndex, *emptyArray<Any>())

    override fun toString(): String {
        return """
        -------------------------------
        |     STEP DEPENDENCY FAILED     
        | message: ${generateDeveloperMessage(this.messageEnum, referencedStepIndex, args)}
        | referencedStepIndex: $referencedStepIndex
        ---------------------------------
        """.trimIndent()
    }
}