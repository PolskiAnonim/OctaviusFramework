package org.octavius.novels.form.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.navigator.LocalNavigator
import org.octavius.novels.navigator.Screen

/**
 * Klasa będąca UI formularza - należy do niej wstawić klasę która odpowiada za jego obsługę
 */
abstract class FormScreen : Screen {
    protected abstract val formHandler: FormHandler

    /**
     * Tworzenie wyglądu formularza
     */
    @Composable
    override fun Content(paddingValues: PaddingValues) {
        val navigator = LocalNavigator.current
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .verticalScroll(scrollState) // Dodajemy przewijanie pionowe
        ) {
            // Wyświetlanie błędów globalnych
            val globalErrors by formHandler.errorManager.globalErrors
            if (globalErrors.isNotEmpty()) {
                GlobalErrorsCard(errors = globalErrors)
            }

            // Renderowanie kontrolek
            formHandler.getControlsInOrder().forEach { controlName ->
                formHandler.getControl(controlName)!!.let { control ->
                    formHandler.getControlState(controlName)!!.let { state ->
                        control.Render(
                            controlName = controlName,
                            controlState = state,
                            controls = formHandler.getAllControls(),
                            states = formHandler.getAllStates() // Now reactive thanks to mutableStateMapOf
                        )
                    }
                }
            }

            // Przyciski akcji
            ActionButtons(
                onSave = {
                    if (formHandler.onSaveClicked()) {
                        navigator.showSnackbar("Formularz został zapisany pomyślnie")
                        navigator.removeScreen()
                    } else {
                        navigator.showSnackbar("Formularz zawiera błędy")
                    }
                },
                onCancel = {
                    formHandler.onCancelClicked()
                    navigator.removeScreen()
                }
            )
        }
    }

    /**
     * Przyciski akcji
     */
    @Composable
    private fun ActionButtons(onSave: () -> Unit, onCancel: () -> Unit) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            OutlinedButton(onClick = onCancel) {
                Text("Anuluj")
            }

            Button(onClick = onSave) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = "Zapisz",
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text("Zapisz")
            }
        }
    }

    /**
     * Komponent wyświetlający błędy globalne
     */
    @Composable
    private fun GlobalErrorsCard(errors: List<String>) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.errorContainer
            )
        ) {
            Column(
                modifier = Modifier.padding(16.dp)
            ) {
                Text(
                    text = "Błędy:",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
                errors.forEach { error ->
                    Text(
                        text = "• $error",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }
    }

}