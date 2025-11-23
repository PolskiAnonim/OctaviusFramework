package org.octavius.data.transaction

class TransactionPlan {
    private val _steps = mutableListOf<Pair<StepHandle<*>, TransactionStep<*>>>()
    val steps: List<Pair<StepHandle<*>, TransactionStep<*>>>
        get() = _steps.toList()

    fun <T> add(step: TransactionStep<T>): StepHandle<T> {
        val handle = StepHandle<T>()
        _steps.add(handle to step)
        return handle
    }

    /**
     * Dodaje wszystkie kroki z innego planu transakcji do bieżącego planu.
     *
     * @param otherPlan Plan transakcji, którego kroki zostaną dodane.
     */
    fun addPlan(otherPlan: TransactionPlan) {
        // Bezpośrednie dodanie wszystkich par (uchwyt, krok) z innego planu
        _steps.addAll(otherPlan.steps)
    }
}
