package org.octavius.feature.books.form.book

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

class BookFormValidator(private val entityId: Int?) : FormValidator() {

    override fun defineActionValidations(): Map<String, (FormResultData) -> Boolean> {
        return mapOf("save" to { formData -> validateTitleUniqueness(formData) })
    }

    private fun validateTitleUniqueness(formResultData: FormResultData): Boolean {
        val titlePl = formResultData.getCurrentAs<String>("titlePl")

        val whereClause = listOfNotNull(
            QueryFragment("title_pl = :title_pl", mapOf("title_pl" to titlePl)),
            entityId?.let { QueryFragment("id != :id", mapOf("id" to entityId)) }
        ).join(" AND ")

        val result = dataAccess.select("COUNT(*)")
            .from("books.books")
            .where(whereClause.sql)
            .toField<Long>(whereClause.params)

        return when (result) {
            is DataResult.Success -> {
                if ((result.value ?: 0L) > 0) {
                    errorManager.setFieldErrors("titlePl", listOf(Tr.Books.Validation.titleExists()))
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
