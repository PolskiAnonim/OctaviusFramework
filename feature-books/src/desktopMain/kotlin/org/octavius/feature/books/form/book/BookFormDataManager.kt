package org.octavius.feature.books.form.book

import org.octavius.data.DataResult
import org.octavius.data.builder.execute
import org.octavius.data.builder.toField
import org.octavius.data.transaction.TransactionPlan
import org.octavius.data.transaction.TransactionValue
import org.octavius.data.transaction.assertNotNull
import org.octavius.data.transaction.toTransactionValue
import org.octavius.data.type.PgStandardType
import org.octavius.data.type.withPgType
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormActionResult
import org.octavius.form.component.FormDataManager
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.getCurrent
import org.octavius.form.control.base.getCurrentAs
import org.octavius.form.control.base.getInitialAs
import org.octavius.form.control.type.repeatable.RepeatableResultValue

class BookFormDataManager : FormDataManager() {

    private fun loadBookData(loadedId: Int?) = loadData(loadedId) {
        from("books.books", "b")

        map("id")
        map("titlePl")
        map("titleEng")
        map("status")

        // Relacja N-do-M z autorami
        mapRelatedList("authors") {
            from("books.book_to_authors", "bta")
            linkedBy("bta.book_id")
            map("authorId", "author_id")
        }
    }

    override fun initData(loadedId: Int?, payload: Map<String, Any?>): Map<String, Any?> {
        val loadedData = loadBookData(loadedId)

        val defaultData = if (loadedId == null) {
            mapOf(
                "authors" to emptyList<Map<String, Any?>>()
            )
        } else {
            emptyMap()
        }

        return defaultData + loadedData + payload
    }

    override fun definedFormActions(): Map<String, (FormResultData, Int?) -> FormActionResult> {
        return mapOf(
            "save" to { formData, loadedId -> processSave(formData, loadedId) },
            "delete" to { _, loadedId -> processDelete(loadedId) },
            "cancel" to { _, _ -> FormActionResult.CloseScreen }
        )
    }

    private fun processSave(formResultData: FormResultData, loadedId: Int?): FormActionResult {
        val plan = TransactionPlan()

        // =================================================================================
        // KROK 1: Główna encja 'books'
        // =================================================================================
        val bookIdRef: TransactionValue<Int>

        val bookData = mapOf(
            "title_pl" to formResultData.getCurrent("titlePl"),
            "title_eng" to formResultData.getCurrent("titleEng"),
            "status" to formResultData.getCurrent("status")
        )

        if (loadedId != null) {
            bookIdRef = loadedId.toTransactionValue()
            plan.add(
                dataAccess.update("books.books")
                    .setValues(bookData)
                    .setExpression("updated_at", "NOW()")
                    .where("id = :id")
                    .asStep()
                    .execute(bookData + mapOf("id" to bookIdRef))
            )
        } else {
            bookIdRef = plan.add(
                dataAccess.insertInto("books.books")
                    .values(bookData)
                    .returning("id")
                    .asStep()
                    .toField<Int>(bookData).assertNotNull()
            ).field()
        }

        // =================================================================================
        // KROK 2: Obsługa tabeli łączącej many-to-many (autorzy)
        // =================================================================================
        val authorsResult = formResultData.getCurrentAs<RepeatableResultValue>("authors")

        // Usunięci i zmodyfikowani autorzy (stare wartości do usunięcia)
        val deletedAuthors = authorsResult.deletedRows.map { rowData ->
            rowData.getInitialAs<Int>("authorId")
        } + authorsResult.modifiedRows.map { rowData ->
            rowData.getInitialAs<Int>("authorId")
        }

        if (deletedAuthors.isNotEmpty()) {
            plan.add(
                dataAccess.deleteFrom("books.book_to_authors bta")
                    .using("UNNEST(:ids_to_delete) AS t(id)")
                    .where("bta.author_id = t.id AND bta.book_id = :book_id")
                    .asStep()
                    .execute(
                        "ids_to_delete" to deletedAuthors.withPgType(PgStandardType.INT4_ARRAY),
                        "book_id" to bookIdRef
                    )
            )
        }

        // Dodani i zmodyfikowani autorzy (nowe wartości do wstawienia)
        val insertedAuthors = authorsResult.addedRows.map { rowData ->
            rowData.getCurrentAs<Int>("authorId")
        } + authorsResult.modifiedRows.map { rowData ->
            rowData.getCurrentAs<Int>("authorId")
        }

        if (insertedAuthors.isNotEmpty()) {
            plan.add(
                dataAccess.insertInto("books.book_to_authors")
                    .fromSelect(
                        dataAccess.select(":book_id", "author_id")
                            .from("UNNEST(:ids_to_insert) AS author_id")
                            .toSql()
                    )
                    .asStep()
                    .execute(
                        "ids_to_insert" to insertedAuthors.withPgType(PgStandardType.INT4_ARRAY),
                        "book_id" to bookIdRef
                    )
            )
        }

        return when (val result = dataAccess.executeTransactionPlan(plan)) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                FormActionResult.Failure
            }
            is DataResult.Success -> FormActionResult.CloseScreen
        }
    }

    private fun processDelete(loadedId: Int?): FormActionResult {
        if (loadedId == null) return FormActionResult.CloseScreen

        // CASCADE usunie powiązania w book_to_authors
        val result = dataAccess.deleteFrom("books.books")
            .where("id = :id")
            .execute(mapOf("id" to loadedId))

        return when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                FormActionResult.Failure
            }
            is DataResult.Success<*> -> FormActionResult.CloseScreen
        }
    }
}
