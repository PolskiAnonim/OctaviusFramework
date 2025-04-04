package org.octavius.novels.form

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.form.control.ControlState
import org.octavius.novels.navigator.LocalNavigator
import org.octavius.novels.util.Converters.camelToSnakeCase

abstract class Form {
    protected val formSchema: FormControls by lazy {
        createSchema()
    }

    protected abstract fun createSchema(): FormControls

    protected val formState: MutableMap<String, ControlState<*>> = mutableMapOf()

    protected abstract fun defineTableRelations(): List<TableRelation>

    fun loadData(id: Int) {
        val tableRelations = defineTableRelations()
        val data = DatabaseManager.getEntityWithRelations(id, tableRelations)

        for ((controlName, control) in formSchema.controls) {
            if (control.fieldName != null && control.tableName != null) {
                val columnName = camelToSnakeCase(control.fieldName)
                val tableName = camelToSnakeCase(control.tableName)
                val value = data[ColumnInfo(tableName, columnName)]
                formState[controlName] = control.setInitValue(value)
            }
        }
    }

    @Composable
    fun display() {
        val navigator = LocalNavigator.current
        val scrollState = rememberScrollState()

        Scaffold(
            bottomBar = {
                BottomAppBar {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        OutlinedButton(
                            onClick = { navigator.removeScreen() }
                        ) {
                            Text("Anuluj")
                        }

                        Button(
                            onClick = { /* TODO: Zapis formularza */ }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = "Zapisz",
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            Text("Zapisz")
                        }
                    }
                }
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .verticalScroll(scrollState) // Dodajemy przewijanie pionowe
            ) {
                formSchema.order.forEach {
                    formSchema.controls[it]?.render(it, formSchema.controls, formState)
                }

                // Dodajemy dodatkowy odstęp na dole, aby zapewnić, że dolne elementy
                // nie będą zasłonięte przez dolny pasek
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}