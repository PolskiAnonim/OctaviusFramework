package org.octavius.novels.form.control.type

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.validation.ControlValidator
import org.octavius.novels.form.control.validation.DefaultValidator


class TextControl(
    columnInfo: ColumnInfo?,
    label: String?,
    hidden: Boolean? = null,
    required: Boolean? = false,
    dependencies: Map<String, ControlDependency<*>>? = null
) : Control<String>(
    label,
    columnInfo,
    hidden,
    required,
    dependencies
) {
    override val validator: ControlValidator<String> = DefaultValidator()


    @Composable
    override fun Display(
        controlState: ControlState<String>?,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>
    ) {
        controlState!!.let { ctrlState ->
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = label ?: "",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
                )

                OutlinedTextField(
                    value = ctrlState.value.value ?: "",
                    onValueChange = {
                        ctrlState.value.value = it
                        ctrlState.dirty.value = true
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp, horizontal = 4.dp),
                    singleLine = true
                )

                if (ctrlState.error.value != null) {
                    Text(
                        text = ctrlState.error.value ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp)
                    )
                }
            }
        }
    }
}