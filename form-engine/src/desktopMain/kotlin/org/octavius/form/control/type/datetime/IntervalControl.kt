package org.octavius.form.control.type.datetime

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import org.octavius.data.contract.ColumnInfo
import org.octavius.form.control.base.*
import org.octavius.ui.datetime.IntervalPickerDialog
import org.octavius.ui.datetime.PickerTextField
import org.octavius.form.control.validator.datetime.IntervalValidator
import kotlin.time.Duration

class IntervalControl(
    columnInfo: ColumnInfo?,
    label: String?,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null,
    validationOptions: IntervalValidation? = null,
    actions: List<ControlAction<Duration>>? = null
) : Control<Duration>(
    label, columnInfo, required, dependencies, validationOptions, actions
) {
    override val validator: ControlValidator<Duration> = IntervalValidator(validationOptions)

    private fun formatDuration(duration: Duration?): String {
        if (duration == null) return ""
        return duration.toComponents { hours, minutes, seconds, _ ->
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        }
    }

    @Composable
    override fun Display(renderContext: RenderContext, controlState: ControlState<Duration>, isRequired: Boolean) {
        val scope = rememberCoroutineScope()
        var showDialog by remember { mutableStateOf(false) }

        PickerTextField(
            value = formatDuration(controlState.value.value),
            onClick = { showDialog = true },
            onClear = if (!isRequired) {
                {
                    controlState.value.value = null
                    executeActions(renderContext, null, scope)
                }
            } else null,
            isRequired = isRequired
        )

        if (showDialog) {
            IntervalPickerDialog(
                initialValue = controlState.value.value,
                onDismiss = { showDialog = false },
                onConfirm = { newDuration ->
                    controlState.value.value = newDuration
                    executeActions(renderContext, newDuration, scope)
                    showDialog = false
                }
            )
        }
    }
}