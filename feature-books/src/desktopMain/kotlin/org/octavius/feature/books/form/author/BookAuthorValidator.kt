package org.octavius.feature.books.form.author

import org.octavius.data.DataResult
import org.octavius.data.QueryFragment
import org.octavius.data.builder.toField
import org.octavius.data.join
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormValidator
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.getCurrentAs
import org.octavius.localization.Tr

class BookAuthorValidator(private val entityId: Int?) : FormValidator() {

    override fun defineActionValidations(): Map<String, (FormResultData) -> Boolean> {
        return mapOf("save" to { formData -> validateNameUniqueness(formData) })
    }

    private fun validateNameUniqueness(formResultData: FormResultData): Boolean {
        val name = formResultData.getCurrentAs<String>("name")

        val whereClause = listOfNotNull(
            QueryFragment("name = :name", mapOf("name" to name)),
            entityId?.let { QueryFragment("id != :id", mapOf("id" to entityId)) }
        ).join(" AND ")

        val result = dataAccess.select("COUNT(*)")
            .from("books.authors")
            .where(whereClause.sql)
            .toField<Long>(whereClause.params)

        return when (result) {
            is DataResult.Success -> {
                if ((result.value ?: 0L) > 0) {
                    errorManager.setFieldErrors("name", listOf(Tr.Books.Authors.Validation.nameExists()))
                    false
                } else {
                    true
                }
            }
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                false
            }
        }
    }
}