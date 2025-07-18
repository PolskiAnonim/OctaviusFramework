package org.octavius.form.control.validator.primitive

import org.octavius.form.ControlState
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.base.IntegerValidation
import org.octavius.localization.Translations

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
                    errors.add(Translations.get("validation.minValue", min))
                }
            }

            // Sprawdź wartość maksymalną
            options.max?.let { max ->
                if (value > max) {
                    errors.add(Translations.get("validation.maxValue", max))
                }
            }

            // Sprawdź krok
            options.step?.let { step ->
                if (value % step != 0) {
                    errors.add(Translations.get("validation.multipleOf", step))
                }
            }
        }

        errorManager.setFieldErrors(controlName, errors)
    }
}