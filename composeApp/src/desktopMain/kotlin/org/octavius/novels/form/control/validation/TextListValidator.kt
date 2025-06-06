package org.octavius.novels.form.control.validation

import org.octavius.novels.form.ControlState

/**
 * Walidator dla kontrolek list tekstowych z obsługą opcji walidacji.
 */
class TextListValidator(
    private val validationOptions: TextListValidation? = null
) : ControlValidator<List<String>>() {

    override fun validateSpecific(state: ControlState<*>) {
        @Suppress("UNCHECKED_CAST")
        val value = state.value.value as? List<String> ?: return

        validationOptions?.let { options ->
            // Sprawdź minimalną liczbę elementów
            options.minItems?.let { minItems ->
                if (value.size < minItems) {
                    state.error.value = "Wymagane minimum $minItems elementów"
                    return
                }
            }

            // Sprawdź maksymalną liczbę elementów
            options.maxItems?.let { maxItems ->
                if (value.size > maxItems) {
                    state.error.value = "Maksymalnie $maxItems elementów"
                    return
                }
            }

            // Sprawdź walidację pojedynczych elementów
            options.itemValidation?.let { itemValidation ->
                val textValidator = TextValidator(itemValidation)
                value.forEachIndexed { index, item ->
                    val itemState = ControlState<String>()
                    itemState.value.value = item
                    textValidator.validateSpecific(itemState)
                    
                    if (itemState.error.value != null) {
                        state.error.value = "Element ${index + 1}: ${itemState.error.value}"
                        return
                    }
                }
            }
        }

        state.error.value = null
    }
}