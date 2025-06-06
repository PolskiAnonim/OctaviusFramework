package org.octavius.novels.form.control.validation

import org.octavius.novels.form.ControlState

/**
 * Walidator dla kontrolek tekstowych z obsługą opcji walidacji.
 */
class TextValidator(
    private val validationOptions: TextValidation? = null
) : ControlValidator<String>() {

    override fun validateSpecific(state: ControlState<*>) {
        val value = state.value.value as? String ?: return
        
        if (value.isBlank()) {
            state.error.value = null
            return
        }

        validationOptions?.let { options ->
            // Sprawdź minimalną długość
            options.minLength?.let { minLength ->
                if (value.length < minLength) {
                    state.error.value = "Minimalna długość to $minLength znaków"
                    return
                }
            }

            // Sprawdź maksymalną długość
            options.maxLength?.let { maxLength ->
                if (value.length > maxLength) {
                    state.error.value = "Maksymalna długość to $maxLength znaków"
                    return
                }
            }

            // Sprawdź wzorzec
            options.pattern?.let { pattern ->
                if (!pattern.matches(value)) {
                    state.error.value = options.patternErrorMessage ?: "Nieprawidłowy format"
                    return
                }
            }
        }

        state.error.value = null
    }
}