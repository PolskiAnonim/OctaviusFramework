package org.octavius.form.component

import org.octavius.navigation.Screen


sealed class FormActionResult {
    // Akcje zmieniające UI
    data class Navigate(val screen: Screen) : FormActionResult() // Przekierowanie
    object CloseScreen : FormActionResult() // Akcja "Anuluj"
    //Akcje generyczne
    object ValidationFailed : FormActionResult() // Błędy walidacji
    object Failure : FormActionResult() // Ogólny błąd
    object Success : FormActionResult() // Generyczny sukces, np. po zapisie
}

interface FormActionTrigger {
    fun triggerAction(actionKey: String, validates: Boolean): FormActionResult
}