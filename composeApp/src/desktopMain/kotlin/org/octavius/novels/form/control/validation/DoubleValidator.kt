package org.octavius.novels.form.control.validation

import org.octavius.novels.form.ControlState
import kotlin.math.pow
import kotlin.math.round

/**
 * Walidator dla kontrolek liczb zmiennoprzecinkowych z obsługą opcji walidacji.
 */
class DoubleValidator(
    private val validationOptions: DoubleValidation? = null
) : ControlValidator<Double>() {

    override fun validateSpecific(controlName: String, state: ControlState<*>) {
        val value = state.value.value as? Double ?: return
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

            // Sprawdź miejsca dziesiętne
            options.decimalPlaces?.let { decimalPlaces ->
                val multiplier = 10.0.pow(decimalPlaces)
                val rounded = round(value * multiplier) / multiplier
                if (value != rounded) {
                    errors.add("Maksymalnie $decimalPlaces miejsc po przecinku")
                }
            }

            // Sprawdź krok
            options.step?.let { step ->
                val remainder = value % step
                if (remainder != 0.0 && (remainder - step).let { kotlin.math.abs(it) } > 1e-10) {
                    errors.add("Wartość musi być wielokrotnością $step")
                }
            }
        }

        errorManager.setFieldErrors(controlName, errors)
    }
}