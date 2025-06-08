package org.octavius.novels.form.control.type.primitive

import androidx.compose.runtime.Composable
import org.octavius.novels.domain.ColumnInfo
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.base.Control
import org.octavius.novels.form.control.base.ControlValidator
import org.octavius.novels.form.control.validator.DefaultValidator

/**
 * Kontrolka do przechowywania ukrytych wartości w formularzu.
 *
 * Nie renderuje żadnego widocznego elementu interfejsu, ale przechowuje
 * wartość która zostanie uwzględniona w wynikach formularza. Używana
 * do przekazywania wartości technicznych, identyfikatorów czy stanu.
 */
class HiddenControl<T : Any>(columnInfo: ColumnInfo?) : Control<T>(
    label = null,
    columnInfo,
    required = null,
    dependencies = null,
    hasStandardLayout = false // Nie ma nic
) {
    override val validator: ControlValidator<T> = DefaultValidator()


    @Composable
    override fun Display(controlName: String, controlState: ControlState<T>, isRequired: Boolean) {
        //Brak widocznej zawartości
    }
}