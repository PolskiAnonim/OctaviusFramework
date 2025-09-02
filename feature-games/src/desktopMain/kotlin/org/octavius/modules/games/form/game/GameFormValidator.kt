package org.octavius.modules.games.form.game

import org.octavius.data.contract.DataResult
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormValidator
import org.octavius.form.control.base.FormResultData
import org.octavius.localization.T

class GameFormValidator(private val entityId: Int?) : FormValidator() {

    override fun defineActionValidations(): Map<String, (formResultData: FormResultData) -> Boolean> {
        return mapOf(
            "save" to { formData -> checkTitleUniqueness(formData) }
        )
    }

    /**
     * Sprawdza, czy podany tytuł gry jest unikalny w bazie danych.
     * Jeśli edytujemy istniejącą grę, jej własny tytuł jest ignorowany w sprawdzaniu.
     *
     * @param formResultData Dane z formularza.
     * @return `true` jeśli tytuł jest unikalny lub wystąpił błąd, `false` jeśli tytuł już istnieje.
     */
    private fun checkTitleUniqueness(formResultData: FormResultData): Boolean {
        val title = formResultData["name"]?.currentValue as? String

        // Jeśli tytuł jest pusty, standardowa walidacja wymagalności już to obsłużyła.
        if (title.isNullOrBlank()) {
            return true
        }


        val params = mutableMapOf<String, Any>("title" to title)
        var whereClause = "name = :title"

        if (entityId != null) {
            whereClause += " AND id != :id"
            params["id"] = entityId
        }

        val result = dataFetcher.query().from("games").where(whereClause).toCount(params)

        return when (result) {
            is DataResult.Success -> {
                if (result.value > 0) {
                    // Tytuł już istnieje. Ustawiamy błąd dla konkretnego pola 'name'.
                    errorManager.setFieldErrors("name", listOf(T.get("games.validation.nameExists")))
                    false // Walidacja się nie powiodła.
                } else {
                    true // Tytuł jest unikalny. Walidacja powiodła się.
                }
            }
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                false
            }
        }
    }
}