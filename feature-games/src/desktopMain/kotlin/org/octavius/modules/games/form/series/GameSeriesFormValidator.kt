package org.octavius.modules.games.form.series

import org.octavius.data.DataResult
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormValidator
import org.octavius.form.control.base.FormResultData
import org.octavius.localization.T

class GameSeriesFormValidator(private val entityId: Int?) : FormValidator() {

    override fun defineActionValidations(): Map<String, (FormResultData) -> Boolean> {
        return mapOf("save" to { formData -> validateTitleAgainstDatabase(formData) })
    }

    /**
     * Sprawdza, czy podany tytuł serii jest unikalny w bazie danych.
     * Jeśli edytujemy istniejącą serię, jej własny tytuł jest ignorowany w sprawdzaniu.
     *
     * @param formResultData Dane z formularza.
     * @return `true` jeśli tytuł jest unikalny lub wystąpił błąd, `false` jeśli tytuł już istnieje.
     */
    private fun validateTitleAgainstDatabase(formResultData: FormResultData): Boolean {
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

        val result = dataAccess.select("COUNT(*)").from("series").where(whereClause).toField<Long>(params)

        return when (result) {
            is DataResult.Success -> {
                if ((result.value ?: 0L) > 0) {
                    // Tytuł już istnieje. Ustawiamy błąd dla konkretnego pola 'name'.
                    errorManager.setFieldErrors("name", listOf(T.get("games.validation.duplicatedSeries")))
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