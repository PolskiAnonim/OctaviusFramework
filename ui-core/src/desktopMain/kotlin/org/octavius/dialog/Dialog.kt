package org.octavius.dialog

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
* Uniwersalny komponent Composable do wyświetlania dialogów.
* Renderuje dialog na podstawie zawartości `DialogConfig`.
*
* @param config Konfiguracja dialogu. Jeśli jest `null`, dialog nie jest wyświetlany.
*/
@Composable
fun DialogWrapper(config: DialogConfig?) {
    if (config != null) {
        AlertDialog(
            onDismissRequest = config.onDismiss,
            title = { Text(text = config.title, color = config.titleColor ?: Color.Unspecified) },
            text = { Text(text = config.text, color = config.textColor ?: Color.Unspecified) },
            confirmButton = {
                config.confirmButtonText?.let { text ->
                    TextButton(
                        onClick = {
                            config.onConfirm?.invoke()
                            config.onDismiss() // Zamykamy dialog po potwierdzeniu
                        }
                    ) {
                        Text(text)
                    }
                }
            },
            dismissButton = {
                config.dismissButtonText?.let { text ->
                    TextButton(onClick = config.onDismiss) {
                        Text(text)
                    }
                }
            }
        )
    }
}