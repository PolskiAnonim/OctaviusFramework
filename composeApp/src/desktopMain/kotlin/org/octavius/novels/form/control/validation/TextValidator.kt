package org.octavius.novels.form.control.validation

import org.octavius.novels.form.ControlState

/**
 * Walidator dla kontrolek tekstowych z obsługą opcji walidacji.
 */
class TextValidator(
    private val validationOptions: TextValidation? = null
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
                    errors.add("Minimalna długość to $minLength znaków")
                }
            }

            // Sprawdź maksymalną długość
            options.maxLength?.let { maxLength ->
                if (value.length > maxLength) {
                    errors.add("Maksymalna długość to $maxLength znaków")
                }
            }

            // Sprawdź wzorzec
            options.pattern?.let { pattern ->
                if (!pattern.matches(value)) {
                    errors.add(options.patternErrorMessage ?: "Nieprawidłowy format")
                }
            }
        }

        errorManager.setFieldErrors(controlName, errors)
    }
}