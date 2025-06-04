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

/**
 * Zestaw funkcji renderujących wspólne elementy kontrolek.
 * Zapewnia spójny wygląd etykiet, błędów i innych elementów UI.
 */

/**
 * Renderuje etykietę kontrolki z opcjonalną gwiazdką dla pól wymaganych.
 */
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

/**
 * Renderuje standardową etykietę kontrolki z paddingiem.
 * Używana przez większość kontrolek formularza.
 */
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

/**
 * Renderuje etykietę dla kontrolki checkbox bez dodatkowego paddingu.
 * Checkboxy mają własne rozmieszczenie elementów.
 */
@Composable
fun RenderCheckboxLabel(label: String?, isRequired: Boolean) {
    if (label == null) return

    RenderLabel(label, isRequired)
}

/**
 * Renderuje komunikat błędu walidacji kontrolki.
 * Wyświetla się tylko gdy kontrolka ma błąd walidacji.
 * 
 * @param ctrlState stan kontrolki zawierający informacje o błędzie
 */
@Composable
fun RenderError(ctrlState: ControlState<*>) {
    if (ctrlState.error.value == null) return

    Text(
        text = ctrlState.error.value ?: "",
        color = MaterialTheme.colorScheme.error,
        style = MaterialTheme.typography.bodySmall,
        modifier = Modifier.padding(start = 24.dp, bottom = 8.dp)
    )
}