package org.octavius.form.control.type.datetime

import androidx.compose.runtime.*
import org.octavius.form.control.base.*
import org.octavius.form.control.validator.datetime.DateTimeValidator
import org.octavius.ui.datetime.DateTimePickerDialog
import org.octavius.ui.datetime.PickerTextField
import org.octavius.util.DateTimeAdapter

class DateTimeControl<T : Any>(
    private val adapter: DateTimeAdapter<T>,
    label: String?,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null,
    validationOptions: DateTimeValidation<T>? = null,
    actions: List<ControlAction<T>>? = null
) : Control<T>(label, required, dependencies, validationOptions, actions) {

    override val validator: ControlValidator<T> = DateTimeValidator(validationOptions, adapter)

    @Composable
    override fun Display(renderContext: RenderContext, controlState: ControlState<T>, isRequired: Boolean) {
        val scope = rememberCoroutineScope()
        var showDialog by remember { mutableStateOf(false) }

        PickerTextField(
            value = adapter.format(controlState.value.value),
            onClick = { showDialog = true },
            onClear = if (!isRequired) { {
                controlState.value.value = null
                executeActions(renderContext, null, scope)
            } } else null,
            isRequired = isRequired
        )

        if (showDialog) {
            DateTimePickerDialog(
                adapter = adapter,
                initialValue = controlState.value.value,
                onDismiss = { showDialog = false },
                onConfirm = { newValue ->
                    showDialog = false
                    controlState.value.value = newValue
                    executeActions(renderContext, newValue, scope)
                }
            )
        }
    }
}