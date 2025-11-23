package org.octavius.data.transaction

import org.octavius.data.exception.StepDependencyException
import org.octavius.data.exception.StepDependencyExceptionMessage

/**
 * Przechowuje wyniki wykonanego planu transakcji w bezpieczny sposób.
 * Umożliwia pobieranie wyników za pomocą `StepHandle` zamiast indeksów.
 */
class TransactionPlanResult(private val results: Map<StepHandle<*>, Any?>) {

    /**
     * Pobiera wynik dla konkretnego kroku.
     * @param handle Uchwyt do kroku, którego wynik chcemy pobrać.
     * @return Wynik o typie zdefiniowanym w uchwycie (`T`)
     */
    @Suppress("UNCHECKED_CAST")
    fun <T> get(handle: StepHandle<T>): T {
        return if (results.containsKey(handle)) {
            results[handle] as T
        } else {
            throw StepDependencyException(StepDependencyExceptionMessage.UNKNOWN_STEP_HANDLE, -1)
        }
    }
}