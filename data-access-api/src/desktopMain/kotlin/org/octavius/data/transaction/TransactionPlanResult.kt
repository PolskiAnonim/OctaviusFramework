package org.octavius.data.transaction

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
            throw IllegalArgumentException("No step found for handle $handle")
        }
    }
}