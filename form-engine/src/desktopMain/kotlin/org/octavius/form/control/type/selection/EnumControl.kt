package org.octavius.form.control.type.selection

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import kotlinx.coroutines.CoroutineScope
import org.octavius.domain.EnumWithFormatter
import org.octavius.form.control.base.ControlAction
import org.octavius.form.control.base.ControlContext
import org.octavius.form.control.base.ControlDependency
import org.octavius.form.control.type.selection.dropdown.DropdownControlBase
import org.octavius.form.control.type.selection.dropdown.DropdownOption
import org.octavius.localization.Tr
import kotlin.reflect.KClass

/**
 * Kontrolka do wyboru wartości z enumeracji (enum) z listy rozwijanej.
 *
 * Automatycznie generuje opcje wyboru na podstawie wartości enumeracji.
 * Wymaga aby enum implementował interfejs EnumWithFormatter dla formatowania
 * tekstu wyświetlanego użytkownikowi. Obsługuje wyszukiwanie w opcjach.
 */
class EnumControl<T>(
    label: String?,
    private val enumClass: KClass<T>,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null,
    actions: List<ControlAction<T>>? = null
) : DropdownControlBase<T>(
    label, required, dependencies, actions
) where T : Enum<T>, T : EnumWithFormatter<T> {

    override fun getDisplayText(value: T?): String? {
        return value?.toDisplayString()
    }

    @Composable
    override fun ColumnScope.RenderMenuItems(
        controlContext: ControlContext,
        scope: CoroutineScope,
        controlState: MutableState<T?>,
        closeMenu: () -> Unit
    ) {
        val options = enumClass.java.enumConstants.map {
            DropdownOption(it, it.toDisplayString())
        }

        // Opcja "null"
        if (required != true) {
            DropdownMenuItem(
                text = { Text(Tr.Form.Dropdown.noSelection()) },
                onClick = {
                    controlState.value = null
                    executeActions(controlContext, null, scope)
                    closeMenu()
                }
            )
            HorizontalDivider()
        }

        // Lista opcji
        options.forEach { option ->
            DropdownMenuItem(
                text = { Text(option.displayText) },
                onClick = {
                    controlState.value = option.value
                    executeActions(controlContext, option.value, scope)
                    closeMenu()
                }
            )
        }
    }
}