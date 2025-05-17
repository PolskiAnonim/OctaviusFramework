package org.octavius.novels.form.control

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.form.ControlState


@Composable
private fun RenderLabel(label: String, isRequired: Boolean) {
    Text(
        text = label,
        style = MaterialTheme.typography.titleMedium
    )

    if (isRequired) {
        Text(
            text = "*",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.error
        )
    }
}

@Composable
fun RenderNormalLabel(label: String?, isRequired: Boolean) {
    if (label == null) return

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
    ) {
        RenderLabel(label, isRequired)
    }
}

@Composable
fun RenderCheckboxLabel(label: String?, isRequired: Boolean) {
    if (label == null) return

    RenderLabel(label, isRequired)
}

@Composable
fun RenderError(ctrlState: ControlState<*>) {
    if (ctrlState.error.value == null) return
    // Wyświetlanie komunikatu o błędzie

    Text(
        text = ctrlState.error.value ?: "",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 24.dp, bottom = 8.dp) // Wyrównane z tekstem etykiety
    )

}