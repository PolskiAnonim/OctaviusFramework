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
     * Adds all steps from another transaction plan to the current plan.
     *
     * @param otherPlan Transaction plan whose steps will be added.
     */
    fun addPlan(otherPlan: TransactionPlan) {
        // Direct addition of all (handle, step) pairs from another plan
        _steps.addAll(otherPlan.steps)
    }
}
