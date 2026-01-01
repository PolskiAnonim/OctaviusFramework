package org.octavius.form.control.type.selection.dropdown

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import org.octavius.form.control.base.*
import org.octavius.form.control.layout.RenderNormalLabel
import org.octavius.localization.T

/**
 * Super-bazowa klasa dla kontrolek dropdown.
 * Odpowiada wyłącznie za renderowanie ramki UI (ExposedDropdownMenuBox)
 * i deleguje renderowanie zawartości menu do podklas.
 */
abstract class DropdownControlBase<T : Any>(
    label: String?,
    required: Boolean?,
    dependencies: Map<String, ControlDependency<*>>?,
    actions: List<ControlAction<T>>?
) : Control<T>(
    label,
    required,
    dependencies,
    hasStandardLayout = false,
    actions = actions
) {

    protected abstract fun getDisplayText(value: T?): String?

    /**
     * Podklasy muszą zaimplementować tę metodę, aby wyrenderować
     * zawartość menu rozwijanego.
     */
    @Composable
    protected abstract fun ColumnScope.RenderMenuItems(
        controlContext: ControlContext,
        scope: CoroutineScope,
        controlState: MutableState<T?>,
        closeMenu: () -> Unit
    )

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Display(controlContext: ControlContext, controlState: ControlState<T>, isRequired: Boolean) {
        var expanded by remember { mutableStateOf(false) }
        val scope = rememberCoroutineScope()

        Column(modifier = Modifier.fillMaxWidth()) {
            RenderNormalLabel(label, isRequired)

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = getDisplayText(controlState.value.value)
                        ?: (if (!isRequired) T.get("form.dropdown.noSelection") else T.get("form.dropdown.selectOption")),
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable)
                )

                // Menu z opcjami
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    RenderMenuItems(
                        controlContext = controlContext,
                        scope = scope,
                        controlState = controlState.value,
                        closeMenu = { expanded = false }
                    )
                }
            }
            DisplayFieldErrors(controlContext)
        }
    }
}
