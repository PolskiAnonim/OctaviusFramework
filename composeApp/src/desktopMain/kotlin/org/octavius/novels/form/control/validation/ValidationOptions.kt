package org.octavius.novels.form.control.validation

/**
 * Opcje walidacji dla kontrolek formularza.
 * Definiuje reguły walidacji które można zastosować do różnych typów kontrolek.
 */
sealed class ValidationOptions

/**
 * Opcje walidacji dla pól tekstowych.
 */
data class TextValidation(
    val minLength: Int? = null,
    val maxLength: Int? = null,
    val pattern: Regex? = null,
    val patternErrorMessage: String? = null
) : ValidationOptions()

/**
 * Opcje walidacji dla liczb całkowitych.
 */
data class IntegerValidation(
    val min: Int? = null,
    val max: Int? = null,
    val step: Int? = null
) : ValidationOptions()

/**
 * Opcje walidacji dla liczb zmiennoprzecinkowych.
 */
data class DoubleValidation(
    val min: Double? = null,
    val max: Double? = null,
    val step: Double? = null,
    val decimalPlaces: Int? = null
) : ValidationOptions()

/**
 * Opcje walidacji dla list tekstowych.
 */
data class TextListValidation(
    val minItems: Int? = null,
    val maxItems: Int? = null,
    val itemValidation: TextValidation? = null
) : ValidationOptions()

/**
 * Opcje walidacji dla kontrolek powtarzalnych.
 */
data class RepeatableValidation(
    val minItems: Int? = null,
    val maxItems: Int? = null,
    val uniqueFields: List<String> = emptyList()
) : ValidationOptions()