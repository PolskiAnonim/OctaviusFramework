package org.octavius.form.control.validator.collection

import org.octavius.form.ControlState
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.base.StringListValidation
import org.octavius.localization.Translations

/**
 * Walidator dla kontrolek list tekstowych z obsługą opcji walidacji.
 */
class StringListValidator(
    private val validationOptions: StringListValidation? = null
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
                value.forEachIndexed { index, item ->
                    val tempErrors = mutableListOf<String>()
                    val itemState = ControlState<String>()
                    itemState.value.value = item

                    // Sprawdź walidację elementu bez używania ErrorManagera (tymczasowo)
                    if (item.isNotBlank()) {
                        itemValidation.minLength?.let { minLength ->
                            if (item.length < minLength) {
                                tempErrors.add(Translations.get("validation.itemMinLength", index + 1, minLength))
                            }
                        }
                        itemValidation.maxLength?.let { maxLength ->
                            if (item.length > maxLength) {
                                tempErrors.add(Translations.get("validation.itemMaxLength", index + 1, maxLength))
                            }
                        }
                        itemValidation.pattern?.let { pattern ->
                            if (!pattern.matches(item)) {
                                tempErrors.add(Translations.get(
                                    "validation.itemPatternError",
                                    index + 1,
                                    itemValidation.patternErrorMessage ?: Translations.get("validation.invalidFormat")
                                ))
                            }
                        }
                    }
                    errors.addAll(tempErrors)
                }
            }
        }

        errorManager.setFieldErrors(controlName, errors)
    }
}