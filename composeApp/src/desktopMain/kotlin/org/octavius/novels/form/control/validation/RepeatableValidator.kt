package org.octavius.novels.form.control.validation

import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.type.repeatable.RepeatableRow

// Walidator dla RepeatableControl
class RepeatableValidator(
    private val uniqueFields: List<String>
) : ControlValidator<List<RepeatableRow>>() {

    override fun validateSpecific(state: ControlState<*>) {
        @Suppress("UNCHECKED_CAST")
        val rows = state.value.value as List<RepeatableRow>

        // Sprawdź unikalność
        if (uniqueFields.isNotEmpty()) {
            val seenValues = mutableSetOf<List<Any?>>()

            for ((index, row) in rows.withIndex()) {
                val uniqueKey = uniqueFields.map { field -> row.states[field]!!.value.value }

                if (!seenValues.add(uniqueKey)) {
                    state.error.value = "Duplikat wartości w wierszu ${index + 1}"
                    return
                }
            }
        }
    }
}