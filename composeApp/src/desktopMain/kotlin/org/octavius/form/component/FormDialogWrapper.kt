package org.octavius.form.component

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

@Composable
fun FormDialogWrapper(
    formScreen: FormScreen,
    onDismiss: () -> Unit,
    title: String = ""
) {
    var isOpen by remember { mutableStateOf(true) }
    
    if (isOpen) {
        Dialog(
            onDismissRequest = {
                isOpen = false
                onDismiss()
            },
            properties = DialogProperties(
                dismissOnBackPress = true,
                dismissOnClickOutside = false,
                usePlatformDefaultWidth = false
            )
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .fillMaxHeight(0.8f)
                    .padding(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxSize()
                ) {
                    if (title.isNotEmpty()) {
                        Text(
                            text = title,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(16.dp)
                        )
                        HorizontalDivider()
                    }
                    
                    Box(modifier = Modifier.weight(1f)) {
                        formScreen.Content()
                    }
                }
            }
        }
    }
}