package org.octavius.modules.asian.form

import org.octavius.data.DataResult
import org.octavius.data.builder.execute
import org.octavius.data.transaction.TransactionPlan
import org.octavius.data.transaction.TransactionValue
import org.octavius.data.transaction.toTransactionValue
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormActionResult
import org.octavius.form.component.FormDataManager
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.getCurrent
import org.octavius.form.control.base.getCurrentAs
import org.octavius.form.control.base.getInitialAs
import org.octavius.form.control.type.repeatable.RepeatableResultValue

class AsianMediaFormDataManager : FormDataManager() {

    private fun loadAsianMediaData(loadedId: Int?) = loadData(loadedId) {
        from("asian_media.titles", "t")

        // Proste mapowania z tabeli 'titles'
        map("id")
        map("titles")
        map("language")

        // Relacja 1-do-N z 'categories'
        mapRelatedList("publications") {
            from("asian_media.publications", "p")
            join("LEFT JOIN asian_media.publication_volumes pv ON pv.publication_id = p.id")
            linkedBy("p.title_id")
            map("id")
            map("publicationType")
            map("status")
            map("trackProgress")
            map("volumes")
            map("translatedVolumes")
            map("chapters")
            map("translatedChapters")
            map("originalCompleted")
        }
    }

    override fun initData(loadedId: Int?, payload: Map<String, Any?>?): Map<String, Any?> {
        val loadedData = loadAsianMediaData(loadedId)

        // Kolejność łączenia: Załadowane z DB -> Payload (nadpisuje wszystko)
        return loadedData + (payload ?: emptyMap())
    }

    override fun definedFormActions(): Map<String, (FormResultData, Int?) -> FormActionResult> {
        return mapOf(
            "save" to { formData, loadedId -> processSave(formData, loadedId) },
            "delete" to { _, loadedId -> processDelete(loadedId!!) /* Istnienie ID zapewnia logika ukrywania przycisku */ },
            "cancel" to { _, _ -> FormActionResult.CloseScreen }
        )
    }

    fun processDelete(loadedId: Int): FormActionResult {
        // Wykorzystanie CASCADE
        val plan = TransactionPlan()
        plan.add(
            dataAccess.deleteFrom("asian_media.titles")
                .where("id = :id")
                .asStep()
                .execute("id" to loadedId)
        )

        return when (val result = dataAccess.executeTransactionPlan(plan)) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                FormActionResult.Failure
            }
            is DataResult.Success -> FormActionResult.CloseScreen
        }
    }

    fun processSave(formResultData: FormResultData, loadedId: Int?): FormActionResult {
        val plan = TransactionPlan()

        // =================================================================================
        // KROK 1: Główna encja 'titles'
        // =================================================================================
        val titleIdRef: TransactionValue

        val titleData = mapOf(
            "titles" to formResultData.getCurrent("titles"),
            "language" to formResultData.getCurrent("language")
        )

        if (loadedId != null) {
            // TRYB EDYCJI: ID jest znane.
            titleIdRef = loadedId.toTransactionValue()
            plan.add(
                dataAccess.update("asian_media.titles")
                    .setValues(titleData)
                    .where("id = :id")
                    .asStep()
                    .execute(titleData + mapOf("id" to titleIdRef))
            )
        } else {
            titleIdRef = plan.add(
                dataAccess.insertInto("asian_media.titles")
                    .values(titleData)
                    .returning("id")
                    .asStep()
                    .toField<Int>(titleData)
            ).field()
        }

        // =================================================================================
        // KROK 2: Obsługa pod-encji 'publications'
        // =================================================================================
        val publicationsResult = formResultData.getCurrentAs<RepeatableResultValue>("publications")

        // --- TRYB EDYCJI: Rozpatrujemy zmiany ---
        if (loadedId != null) {
            // Usunięte publikacje
            publicationsResult.deletedRows.forEach { rowData ->
                val pubId = rowData.getInitialAs<Int>("id")
                plan.add(
                    dataAccess.deleteFrom("asian_media.publications")
                        .where("id = :id")
                        .asStep()
                        .execute("id" to pubId)
                )
            }

            // Zmodyfikowane publikacje
            publicationsResult.modifiedRows.forEach { rowData ->
                val pubId = rowData.getInitialAs<Int>("id")
                val publicationIdRef = pubId.toTransactionValue()
                val publicationData = mapOf(
                    "publication_type" to rowData.getCurrent("publicationType"),
                    "status" to rowData.getCurrent("status"),
                    "track_progress" to rowData.getCurrent("trackProgress")
                )

                plan.add(
                    dataAccess.update("asian_media.publications")
                        .setValues(publicationData)
                        .where("id = :id")
                        .asStep()
                        .execute(publicationData + mapOf("id" to publicationIdRef))
                )

                // Warunkowo zaktualizuj powiązane 'publication_volumes'
                addPublicationVolumesUpdateOperation(plan, rowData, publicationIdRef)
            }
        }

        // --- TRYB TWORZENIA lub DODAWANIA NOWYCH (wspólna logika) ---
        // W trybie tworzenia `allCurrentRows` = `addedRows`
        val rowsToAdd = if (loadedId == null) publicationsResult.allCurrentRows else publicationsResult.addedRows

        rowsToAdd.forEach { rowData ->
            val publicationData = mapOf(
                "publication_type" to rowData.getCurrent("publicationType"),
                "status" to rowData.getCurrent("status"),
                "track_progress" to rowData.getCurrent("trackProgress"),
                "title_id" to titleIdRef
            )

            val newPublicationIdRef = plan.add(
                dataAccess.insertInto("asian_media.publications")
                    .values(publicationData)
                    .returning("id")
                    .asStep()
                    .toField<Int>(publicationData)
            ).field()

            addPublicationVolumesUpdateOperation(plan, rowData, newPublicationIdRef)
        }

        // Wykonanie całego planu
        return when (val result = dataAccess.executeTransactionPlan(plan)) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                FormActionResult.Failure
            }
            is DataResult.Success -> FormActionResult.CloseScreen
        }
    }

    private fun addPublicationVolumesUpdateOperation(
        plan: TransactionPlan,
        rowData: FormResultData,
        publicationIdRef: TransactionValue
    ) {
        if (rowData.getCurrentAs<Boolean>("trackProgress")) {
            val volumesData = mapOf(
                "volumes" to rowData.getCurrent("volumes"),
                "translated_volumes" to rowData.getCurrent("translatedVolumes"),
                "chapters" to rowData.getCurrent("chapters"),
                "translated_chapters" to rowData.getCurrent("translatedChapters"),
                "original_completed" to rowData.getCurrent("originalCompleted")
            )

            plan.add(
                dataAccess.update("asian_media.publication_volumes")
                    .setValues(volumesData)
                    .where("publication_id = :publication_id")
                    .asStep()
                    .execute(volumesData + mapOf("publication_id" to publicationIdRef))
            )
        }
    }
}