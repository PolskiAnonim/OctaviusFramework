package org.octavius.report.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.octavius.report.ReportConfiguration
import org.octavius.report.ReportConfigurationManager
import org.octavius.report.filter.Filter

@Composable
fun ReportConfigurationDialog(
    reportName: String,
    reportState: ReportState,
    filters: Map<String, Filter>,
    onDismiss: () -> Unit,
    onConfigurationApplied: () -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var configurations by remember { mutableStateOf(emptyList<ReportConfiguration>()) }
    val configManager = remember { ReportConfigurationManager() }

    // Ładuj konfiguracje przy otwarciu dialogu
    LaunchedEffect(reportName) {
        configurations = configManager.listConfigurations(reportName)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            modifier = Modifier
                .width(500.dp)
                .height(400.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                Text(
                    text = "Konfiguracje raportu",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Button(
                        onClick = { showSaveDialog = true }
                    ) {
                        Text("Zapisz obecną konfigurację")
                    }
                    
                    Button(
                        onClick = {
                            configurations = configManager.listConfigurations(reportName)
                        }
                    ) {
                        Text("Odśwież")
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = "Zapisane konfiguracje:",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                LazyColumn(
                    modifier = Modifier.weight(1f)
                ) {
                    items(configurations) { config ->
                        ConfigurationItem(
                            configuration = config,
                            onLoad = {
                                configManager.applyConfiguration(config, reportState, filters)
                                onConfigurationApplied()
                                onDismiss()
                            },
                            onDelete = {
                                configManager.deleteConfiguration(config.name, reportName)
                                configurations = configManager.listConfigurations(reportName)
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Zamknij")
                    }
                }
            }
        }
    }

    if (showSaveDialog) {
        SaveConfigurationDialog(
            reportName = reportName,
            reportState = reportState,
            configManager = configManager,
            onDismiss = { showSaveDialog = false },
            onSaved = {
                showSaveDialog = false
                configurations = configManager.listConfigurations(reportName)
            }
        )
    }
}

@Composable
fun ConfigurationItem(
    configuration: ReportConfiguration,
    onLoad: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = configuration.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    if (configuration.description?.isNotEmpty() == true) {
                        Text(
                            text = configuration.description,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (configuration.isDefault) {
                        Text(
                            text = "Domyślna",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                
                Row {
                    TextButton(onClick = onLoad) {
                        Text("Wczytaj")
                    }
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Text("Usuń")
                    }
                }
            }
        }
    }
}

@Composable
fun SaveConfigurationDialog(
    reportName: String,
    reportState: ReportState,
    configManager: ReportConfigurationManager,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(dismissOnBackPress = true, dismissOnClickOutside = true)
    ) {
        Card(
            modifier = Modifier.width(400.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Zapisz konfigurację",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = name,
                    onValueChange = { 
                        name = it
                        errorMessage = ""
                    },
                    label = { Text("Nazwa konfiguracji") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = errorMessage.isNotEmpty()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Opis (opcjonalny)") },
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isDefault,
                        onCheckedChange = { isDefault = it }
                    )
                    Text("Ustaw jako domyślną")
                }
                
                if (errorMessage.isNotEmpty()) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Anuluj")
                    }
                    
                    Spacer(modifier = Modifier.width(8.dp))
                    
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                errorMessage = "Nazwa jest wymagana"
                                return@Button
                            }
                            
                            val success = configManager.saveConfiguration(
                                name = name.trim(),
                                reportName = reportName,
                                description = description.trim().takeIf { it.isNotEmpty() },
                                reportState = reportState,
                                isDefault = isDefault
                            )
                            
                            if (success) {
                                onSaved()
                            } else {
                                errorMessage = "Błąd podczas zapisywania konfiguracji"
                            }
                        }
                    ) {
                        Text("Zapisz")
                    }
                }
            }
        }
    }
}