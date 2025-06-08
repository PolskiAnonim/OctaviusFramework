package org.octavius.novels.form.control.validator.primitive

import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.base.ControlValidator
import org.octavius.novels.form.control.base.IntegerValidation

/**
 * Walidator dla kontrolek liczb całkowitych z obsługą opcji walidacji.
 */
class IntegerValidator(
    private val validationOptions: IntegerValidation? = null
) : ControlValidator<Int>() {

    override fun validateSpecific(controlName: String, state: ControlState<*>) {
        val value = state.value.value as? Int ?: return
        val errors = mutableListOf<String>()

        validationOptions?.let { options ->
            // Sprawdź wartość minimalną
            options.min?.let { min ->
                if (value < min) {
                    errors.add("Wartość musi być większa lub równa $min")
                }
            }

            // Sprawdź wartość maksymalną
            options.max?.let { max ->
                if (value > max) {
                    errors.add("Wartość musi być mniejsza lub równa $max")
                }
            }

            // Sprawdź krok
            options.step?.let { step ->
                if (value % step != 0) {
                    errors.add("Wartość musi być wielokrotnością $step")
                }
            }
        }

        errorManager.setFieldErrors(controlName, errors)
    }
}