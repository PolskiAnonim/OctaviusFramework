package org.octavius.ui.error

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import org.octavius.localization.Translations

@Composable
fun GlobalErrorDialog(
    errorDetails: ErrorDetails,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = errorDetails.title) },
        text = { Text(text = errorDetails.message) },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(Translations.get("error.dialog.dismiss"))
            }
        }
    )
}