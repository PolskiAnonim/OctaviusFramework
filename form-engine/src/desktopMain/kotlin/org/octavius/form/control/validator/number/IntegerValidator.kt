package org.octavius.form.control.validator.number

import org.octavius.form.control.base.ControlState
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.base.IntegerValidation
import org.octavius.form.control.base.ControlContext
import org.octavius.localization.T

/**
 * Walidator dla kontrolek liczb całkowitych z obsługą opcji walidacji.
 */
class IntegerValidator(
    private val validationOptions: IntegerValidation? = null
) : ControlValidator<Int>() {

    override fun validateSpecific(controlContext: ControlContext, state: ControlState<*>) {
        val value = state.value.value as? Int ?: return
        val errors = mutableListOf<String>()

        validationOptions?.let { options ->
            // Sprawdź wartość minimalną
            options.min?.let { min ->
                if (value < min) {
                    errors.add(T.get("validation.minValue", min))
                }
            }

            // Sprawdź wartość maksymalną
            options.max?.let { max ->
                if (value > max) {
                    errors.add(T.get("validation.maxValue", max))
                }
            }

            // Sprawdź krok
            options.step?.let { step ->
                if (value % step != 0) {
                    errors.add(T.get("validation.multipleOf", step))
                }
            }
        }

        errorManager.setFieldErrors(controlContext.fullStatePath, errors)
    }
}