package org.octavius.form.control.validator.primitive

import org.octavius.form.ControlState
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.base.StringValidation
import org.octavius.localization.Translations

/**
 * Walidator dla kontrolek tekstowych z obsługą opcji walidacji.
 */
class StringValidator(
    private val validationOptions: StringValidation? = null
) : ControlValidator<String>() {

    override fun validateSpecific(controlName: String, state: ControlState<*>) {
        val value = state.value.value as? String ?: return
        val errors = mutableListOf<String>()

        if (value.isBlank()) {
            errorManager.setFieldErrors(controlName, errors)
            return
        }

        validationOptions?.let { options ->
            // Sprawdź minimalną długość
            options.minLength?.let { minLength ->
                if (value.length < minLength) {
                    errors.add(Translations.get("validation.minLength", minLength))
                }
            }

            // Sprawdź maksymalną długość
            options.maxLength?.let { maxLength ->
                if (value.length > maxLength) {
                    errors.add(Translations.get("validation.maxLength", maxLength))
                }
            }

            // Sprawdź wzorzec
            options.pattern?.let { pattern ->
                if (!pattern.matches(value)) {
                    errors.add(options.patternErrorMessage ?: Translations.get("validation.invalidFormat"))
                }
            }
        }

        errorManager.setFieldErrors(controlName, errors)
    }
}