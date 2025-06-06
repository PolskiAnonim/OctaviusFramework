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

    override fun validateSpecific(state: ControlState<*>) {
        val value = state.value.value as? Double ?: return

        validationOptions?.let { options ->
            // Sprawdź wartość minimalną
            options.min?.let { min ->
                if (value < min) {
                    state.error.value = "Wartość musi być większa lub równa $min"
                    return
                }
            }

            // Sprawdź wartość maksymalną
            options.max?.let { max ->
                if (value > max) {
                    state.error.value = "Wartość musi być mniejsza lub równa $max"
                    return
                }
            }

            // Sprawdź miejsca dziesiętne
            options.decimalPlaces?.let { decimalPlaces ->
                val multiplier = 10.0.pow(decimalPlaces)
                val rounded = round(value * multiplier) / multiplier
                if (value != rounded) {
                    state.error.value = "Maksymalnie $decimalPlaces miejsc po przecinku"
                    return
                }
            }

            // Sprawdź krok
            options.step?.let { step ->
                val remainder = value % step
                if (remainder != 0.0 && (remainder - step).let { kotlin.math.abs(it) } > 1e-10) {
                    state.error.value = "Wartość musi być wielokrotnością $step"
                    return
                }
            }
        }

        state.error.value = null
    }
}