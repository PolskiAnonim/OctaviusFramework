package org.octavius.form.control.validator.number

import org.octavius.form.ControlState
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.base.BigDecimalValidation
import org.octavius.localization.Translations
import java.math.BigDecimal

/**
 * Walidator dla kontrolek liczb o wysokiej precyzji (BigDecimal) z obsługą opcji walidacji.
 */
class BigDecimalValidator(
    private val validationOptions: BigDecimalValidation? = null
) : ControlValidator<BigDecimal>() {

    override fun validateSpecific(controlName: String, state: ControlState<*>) {
        val value = state.value.value as? BigDecimal ?: return
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

            // Sprawdź miejsca dziesiętne
            options.decimalPlaces?.let { decimalPlaces ->
                if (value.scale() > decimalPlaces) {
                    errors.add(Translations.get("validation.maxDecimalPlaces", decimalPlaces))
                }
            }

            // Sprawdź krok
            options.step?.let { step ->
                if (value.remainder(step).compareTo(BigDecimal.ZERO) != 0) {
                    errors.add(Translations.get("validation.multipleOf", step))
                }
            }
        }

        errorManager.setFieldErrors(controlName, errors)
    }
}