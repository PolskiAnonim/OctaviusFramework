package org.octavius.form.control.validator.datetime

import org.octavius.form.control.base.ControlState
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.base.DateTimeValidation
import org.octavius.form.control.base.RenderContext
import org.octavius.util.DateTimeAdapter
import org.octavius.localization.T

class DateTimeValidator<T: Any>(
    private val validationOptions: DateTimeValidation<T>? = null,
    private val adapter: DateTimeAdapter<T>
) : ControlValidator<T>() {

    override fun validateSpecific(renderContext: RenderContext, state: ControlState<*>) {
        val value = state.value.value as? T ?: return
        val errors = mutableListOf<String>()

        validationOptions?.let { options ->
            options.min?.let { min ->
                // Używamy standardowego porównania, zakładając, że typy T są Comparable
                @Suppress("UNCHECKED_CAST")
                if ((value as Comparable<T>) < min) {
                    errors.add(T.get("validation.minValue", adapter.format(min)))
                }
            }
            options.max?.let { max ->
                @Suppress("UNCHECKED_CAST")
                if ((value as Comparable<T>) > max) {
                    errors.add(T.get("validation.maxValue", adapter.format(max)))
                }
            }
        }
        errorManager.setFieldErrors(renderContext.fullPath, errors)
    }
}