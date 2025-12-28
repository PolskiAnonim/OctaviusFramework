package org.octavius.modules.games.form.game

import org.octavius.data.DataResult
import org.octavius.data.QueryFragment
import org.octavius.data.builder.toField
import org.octavius.data.join
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormValidator
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.getCurrentAs
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
        val title = formResultData.getCurrentAs<String>("name")

        val whereClause = listOfNotNull(
            QueryFragment("name = :title", mapOf("title" to title)),
            entityId?.let { QueryFragment("id != :id", mapOf("id" to entityId)) }
        ).join(" AND ")

        val result = dataAccess.select("COUNT(*)").from("games").where(whereClause.sql).toField<Long>(whereClause.params)

        return when (result) {
            is DataResult.Success -> {
                if ((result.value ?: 0) > 0) {
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