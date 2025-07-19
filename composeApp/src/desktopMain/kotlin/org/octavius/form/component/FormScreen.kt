package org.octavius.form.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.localization.Translations
import org.octavius.navigation.Screen
import org.octavius.ui.component.LocalSnackbarManager

/**
 * Klasa będąca UI formularza - należy do niej wstawić klasę która odpowiada za jego obsługę
 */
abstract class FormScreen(
    private val onSaveSuccess: () -> Unit = {},
    private val onCancel: () -> Unit = {}
) : Screen {
    abstract override val title: String
    protected abstract val formHandler: FormHandler

    /**
     * Tworzenie wyglądu formularza
     */
    @Composable
    override fun Content() {
        val snackbarManager = LocalSnackbarManager.current
        val scrollState = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
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
                            controlState = state
                        )
                    }
                }
            }

            // Przyciski akcji
            ActionButtons(
                onSave = {
                    if (formHandler.onSaveClicked()) {
                        snackbarManager.showSnackbar(Translations.get("form.actions.savedSuccessfully"))
                        onSaveSuccess()
                    } else {
                        snackbarManager.showSnackbar(Translations.get("form.actions.containsErrors"))
                    }
                },
                onCancel = {
                    formHandler.onCancelClicked()
                    onCancel()
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
                Text(Translations.get("action.cancel"))
            }

            Button(onClick = onSave) {
                Icon(
                    imageVector = Icons.Default.Save,
                    contentDescription = Translations.get("action.save"),
                    modifier = Modifier.padding(end = 8.dp)
                )
                Text(Translations.get("action.save"))
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
                    text = Translations.get("form.actions.errorsLabel"),
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