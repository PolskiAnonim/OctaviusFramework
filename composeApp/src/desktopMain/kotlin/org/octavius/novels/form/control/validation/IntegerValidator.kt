package org.octavius.novels.form.control.validation

import org.octavius.novels.form.ControlState

/**
 * Walidator dla kontrolek liczb całkowitych z obsługą opcji walidacji.
 */
class IntegerValidator(
    private val validationOptions: IntegerValidation? = null
) : ControlValidator<Int>() {

    override fun validateSpecific(state: ControlState<*>) {
        val value = state.value.value as? Int ?: return

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

            // Sprawdź krok
            options.step?.let { step ->
                if (value % step != 0) {
                    state.error.value = "Wartość musi być wielokrotnością $step"
                    return
                }
            }
        }

        state.error.value = null
    }
}