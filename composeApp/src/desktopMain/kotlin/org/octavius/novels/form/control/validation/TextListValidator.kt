package org.octavius.novels.form.control.validation

import org.octavius.novels.form.ControlState

/**
 * Walidator dla kontrolek list tekstowych z obsługą opcji walidacji.
 */
class TextListValidator(
    private val validationOptions: TextListValidation? = null
) : ControlValidator<List<String>>() {

    override fun validateSpecific(controlName: String, state: ControlState<*>) {
        @Suppress("UNCHECKED_CAST")
        val value = state.value.value as? List<String> ?: return
        val errors = mutableListOf<String>()

        validationOptions?.let { options ->
            // Sprawdź minimalną liczbę elementów
            options.minItems?.let { minItems ->
                if (value.size < minItems) {
                    errors.add("Wymagane minimum $minItems elementów")
                }
            }

            // Sprawdź maksymalną liczbę elementów
            options.maxItems?.let { maxItems ->
                if (value.size > maxItems) {
                    errors.add("Maksymalnie $maxItems elementów")
                }
            }

            // Sprawdź walidację pojedynczych elementów
            options.itemValidation?.let { itemValidation ->
                TextValidator(itemValidation)
                value.forEachIndexed { index, item ->
                    val tempErrors = mutableListOf<String>()
                    val itemState = ControlState<String>()
                    itemState.value.value = item

                    // Sprawdź walidację elementu bez używania ErrorManagera (tymczasowo)
                    if (item.isNotBlank()) {
                        itemValidation.minLength?.let { minLength ->
                            if (item.length < minLength) {
                                tempErrors.add("Element ${index + 1}: Minimalna długość to $minLength znaków")
                            }
                        }
                        itemValidation.maxLength?.let { maxLength ->
                            if (item.length > maxLength) {
                                tempErrors.add("Element ${index + 1}: Maksymalna długość to $maxLength znaków")
                            }
                        }
                        itemValidation.pattern?.let { pattern ->
                            if (!pattern.matches(item)) {
                                tempErrors.add("Element ${index + 1}: ${itemValidation.patternErrorMessage ?: "Nieprawidłowy format"}")
                            }
                        }
                    }
                    errors.addAll(tempErrors)
                }
            }
        }

        errorManager?.setFieldErrors(controlName, errors)
    }
}