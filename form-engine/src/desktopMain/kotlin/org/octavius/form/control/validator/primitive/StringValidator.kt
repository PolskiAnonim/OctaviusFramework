package org.octavius.form.control.validator.primitive

import org.octavius.form.control.base.ControlState
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.base.RenderContext
import org.octavius.form.control.base.StringValidation
import org.octavius.localization.T

/**
 * Walidator dla kontrolek tekstowych z obsługą opcji walidacji.
 */
class StringValidator(
    private val validationOptions: StringValidation? = null
) : ControlValidator<String>() {

    override fun validateSpecific(renderContext: RenderContext, state: ControlState<*>) {
        val value = state.value.value as? String ?: return
        val errors = mutableListOf<String>()

        if (value.isBlank()) {
            errorManager.setFieldErrors(renderContext.fullPath, errors)
            return
        }

        validationOptions?.let { options ->
            // Sprawdź minimalną długość
            options.minLength?.let { minLength ->
                if (value.length < minLength) {
                    errors.add(T.get("validation.minLength", minLength))
                }
            }

            // Sprawdź maksymalną długość
            options.maxLength?.let { maxLength ->
                if (value.length > maxLength) {
                    errors.add(T.get("validation.maxLength", maxLength))
                }
            }

            // Sprawdź wzorzec
            options.pattern?.let { pattern ->
                if (!pattern.matches(value)) {
                    errors.add(options.patternErrorMessage ?: T.get("validation.invalidFormat"))
                }
            }
        }

        errorManager.setFieldErrors(renderContext.fullPath, errors)
    }
}