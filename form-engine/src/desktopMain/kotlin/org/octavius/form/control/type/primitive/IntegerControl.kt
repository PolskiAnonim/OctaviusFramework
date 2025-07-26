package org.octavius.form.control.type.primitive

import org.octavius.database.ColumnInfo
import org.octavius.form.control.base.ControlDependency
import org.octavius.form.control.base.ControlValidator
import org.octavius.form.control.base.IntegerValidation
import org.octavius.form.control.validator.primitive.IntegerValidator

/**
 * Kontrolka do wprowadzania liczb całkowitych.
 * Rozszerza PrimitiveNumberControl, dostarczając logikę parsowania dla Int.
 */
class IntegerControl(
    columnInfo: ColumnInfo?,
    label: String?,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null,
    validationOptions: IntegerValidation? = null
) : PrimitiveNumberControl<Int>(
    label,
    columnInfo,
    required,
    dependencies,
    validationOptions = validationOptions
) {
    override val validator: ControlValidator<Int> = IntegerValidator(validationOptions)

    override fun parseValue(text: String): Int? {
        return text.toIntOrNull()
    }
}