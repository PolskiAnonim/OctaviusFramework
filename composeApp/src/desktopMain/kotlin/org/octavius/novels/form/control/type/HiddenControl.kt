package org.octavius.novels.form.control.type

import androidx.compose.runtime.Composable
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.validation.ControlValidator
import org.octavius.novels.form.control.validation.DefaultValidator

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
    dependencies = null
) {
    override val validator: ControlValidator<T> = DefaultValidator()


    @Composable
    override fun Display(
        controlState: ControlState<T>,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>,
        isRequired: Boolean
    ) {
        //Brak widocznej zawartości
    }
}