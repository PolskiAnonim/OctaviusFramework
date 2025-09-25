package org.octavius.report.configuration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import org.octavius.localization.T
import org.octavius.report.ReportEvent
import org.octavius.report.component.ReportState

@Composable
fun ReportConfigurationDialog(
    onEvent: (ReportEvent) -> Unit,
    reportName: String,
    reportState: ReportState,
    onDismiss: () -> Unit
) {
    var showSaveDialog by remember { mutableStateOf(false) }
    var configurations by remember { mutableStateOf(emptyList<ReportConfiguration>()) }
    val configManager = remember { ReportConfigurationManager() }

    LaunchedEffect(reportName) {
        configurations = configManager.listConfigurations(reportName)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .widthIn(min = 500.dp, max = 600.dp)
                .heightIn(min = 400.dp, max = 600.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                Text(T.get("report.configuration.management"), style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = { showSaveDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Default.Bookmark, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                    Text(T.get("report.configuration.saveCurrent"))
                }
                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(16.dp))
                Text(T.get("report.configuration.savedConfigurations"), style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.height(8.dp))
                if (configurations.isEmpty()) {
                    Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(T.get("report.configuration.noSavedConfigurations"), style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    LazyColumn(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(configurations) { config ->
                            ConfigurationItem(
                                configuration = config,
                                onLoad = {
                                    onEvent(ReportEvent.ApplyConfiguration(config))
                                    onDismiss()
                                },
                                onDelete = {
                                    if(configManager.deleteConfiguration(config.name, reportName)) {
                                        configurations = configurations.filter { it.id != config.id }
                                    }
                                }
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(T.get("action.close")) }
                }
            }
        }
    }

    if (showSaveDialog) {
        SaveConfigurationDialog(
            reportName = reportName,
            reportState = reportState,
            configManager = configManager,
            existingConfigurations = configurations,
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
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(12.dp).fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = configuration.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
                    if (configuration.isDefault) {
                        Surface(modifier = Modifier.padding(start = 8.dp), shape = MaterialTheme.shapes.small, color = MaterialTheme.colorScheme.primaryContainer) {
                            Text(text = T.get("report.configuration.defaultConfig").uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                configuration.description?.takeIf { it.isNotEmpty() }?.let {
                    Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onLoad) { Icon(Icons.Default.Download, contentDescription = T.get("action.load")) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = T.get("action.remove"), tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
fun SaveConfigurationDialog(
    reportName: String,
    reportState: ReportState,
    configManager: ReportConfigurationManager,
    existingConfigurations: List<ReportConfiguration>,
    onDismiss: () -> Unit,
    onSaved: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }
    var nameError by remember { mutableStateOf<String?>(null) }
    var generalError by remember { mutableStateOf<String?>(null) }

    val isOverwriting by remember(name) {
        derivedStateOf { existingConfigurations.any { it.name.equals(name.trim(), ignoreCase = true) } }
    }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Card(modifier = Modifier.width(400.dp)) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = T.get("report.configuration.saveConfiguration"), style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; nameError = null; generalError = null },
                    label = { Text(T.get("report.configuration.name") + "*") },
                    modifier = Modifier.fillMaxWidth(),
                    isError = nameError != null,
                    singleLine = true,
                    supportingText = {
                        if (nameError != null) Text(nameError!!, color = MaterialTheme.colorScheme.error)
                        else if (isOverwriting) Text(T.get("report.configuration.overwriteWarning"), color = MaterialTheme.colorScheme.tertiary)
                    }
                )

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(value = description, onValueChange = { description = it }, label = { Text(T.get("report.configuration.description")) }, modifier = Modifier.fillMaxWidth())
                Spacer(modifier = Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Checkbox(checked = isDefault, onCheckedChange = { isDefault = it })
                    Text(T.get("report.configuration.setAsDefault"))
                }
                if (generalError != null) Text(text = generalError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(top = 8.dp))
                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) { Text(T.get("action.cancel")) }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (name.isBlank()) {
                                nameError = T.get("report.configuration.nameRequired")
                                return@Button
                            }

                            val newConfiguration = ReportConfiguration(
                                name = name.trim(),
                                reportName = reportName,
                                description = description.trim().takeIf { it.isNotEmpty() },
                                isDefault = isDefault,
                                visibleColumns = reportState.visibleColumns.toList(),
                                columnOrder = reportState.columnKeysOrder,
                                sortOrder = reportState.sortOrder.map { (col, dir) -> SortConfiguration(col, dir) },
                                pageSize = reportState.pagination.pageSize,
                                filters = reportState.filterData.map { (col, data) -> FilterConfig(col, data.serialize()) }
                            )

                            if (configManager.saveConfiguration(newConfiguration)) {
                                onSaved()
                            } else {
                                generalError = T.get("report.configuration.errorSaving")
                            }
                        },
                        colors = if (isOverwriting) ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary) else ButtonDefaults.buttonColors()
                    ) {
                        Text(if (isOverwriting) T.get("action.overwrite") else T.get("action.save"))
                    }
                }
            }
        }
    }
}