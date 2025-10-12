package org.octavius.modules.asian.form

import org.octavius.data.DataResult
import org.octavius.data.transaction.TransactionValue
import org.octavius.data.transaction.StepReference
import org.octavius.data.transaction.TransactionPlan
import org.octavius.data.transaction.toTransactionValue
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormActionResult
import org.octavius.form.component.FormDataManager
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.getCurrent
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
            "delete" to { formData, loadedId -> processDelete(loadedId) },
            "cancel" to { _, _ -> FormActionResult.CloseScreen }
        )
    }

    fun processDelete(loadedId: Int?): FormActionResult {
        // Wykorzystanie CASCADE
        val plan = TransactionPlan(dataAccess)
        plan.delete("titles", mapOf("id" to loadedId))
        val result = dataAccess.executeTransactionPlan(plan.build())
        when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                return FormActionResult.Failure
            }
            is DataResult.Success<*> -> return FormActionResult.CloseScreen
        }
    }

    fun processSave(formResultData: FormResultData, loadedId: Int?): FormActionResult {
        // 1. Stwórz plan transakcji
        val plan = TransactionPlan(dataAccess)

        // =================================================================================
        // Główna encja 'titles' i jej referencja ID
        // =================================================================================
        val titleIdRef: TransactionValue // Zamiast Any?, używamy docelowego typu

        val titleData = mapOf(
            "titles" to formResultData.getCurrent("titles"),
            "language" to formResultData.getCurrent("language")
        )

        if (loadedId != null) {
            // TRYB EDYCJI: ID jest znane.
            titleIdRef = loadedId.toTransactionValue()
            plan.update(
                tableName = "titles",
                data = titleData,
                filter = mapOf("id" to titleIdRef)
            )
        } else {
            // TRYB TWORZENIA: Tworzymy krok i od razu dostajemy bezpieczną referencję.
            val titleInsertStep: StepReference = plan.insert(
                tableName = "titles",
                data = titleData,
                returning = listOf("id")
            )
            titleIdRef = titleInsertStep.get("id")
        }

        // =================================================================================
        // KROK 2: Obsługa pod-encji 'publications' (Repeatable Field)
        // =================================================================================
        val publicationsResult = formResultData["publications"]!!.currentValue as RepeatableResultValue

        if (loadedId != null) {
            // --- Usunięte publikacje ---
            publicationsResult.deletedRows.forEach { rowData ->
                val pubId = rowData["id"]!!.initialValue as Int
                // Usuwamy publikację. Tabela 'publication_volumes' zostanie usunięta kaskadowo przez DB.
                plan.delete(
                        tableName = "publications",
                        filter = mapOf("id" to pubId)
                    )
            }

            // --- Zmodyfikowane publikacje ---
            publicationsResult.modifiedRows.forEach { rowData ->
                val pubId = rowData["id"]!!.initialValue as Int
                val publicationIdRef = pubId.toTransactionValue() // ID tej publikacji jest znane.
                val publicationData = mapOf(
                    "publication_type" to rowData["publicationType"]!!.currentValue,
                    "status" to rowData["status"]!!.currentValue,
                    "track_progress" to rowData["trackProgress"]!!.currentValue
                )

                // Zaktualizuj samą publikację
                plan.update(
                        tableName = "publications",
                        data = publicationData,
                        filter = mapOf("id" to publicationIdRef)
                    )


                // Warunkowo zaktualizuj powiązane 'publication_volumes'
                addPublicationVolumesUpdateOperation(plan, rowData, publicationIdRef)
            }

            // --- Dodane publikacje ---
            publicationsResult.addedRows.forEach { rowData ->
                val publicationData = mapOf(
                    "publication_type" to rowData["publicationType"]!!.currentValue,
                    "status" to rowData["status"]!!.currentValue,
                    "track_progress" to rowData["trackProgress"]!!.currentValue,
                    // Używamy referencji do ID głównego tytułu (czy to nowego, czy edytowanego)
                    "title_id" to titleIdRef
                )

                // Wstaw nową publikację i ZWRÓĆ JEJ ID, bo będzie potrzebne w kolejnym kroku.
                val newPublicationIdRef = plan.insert(
                    tableName = "publications",
                    data = publicationData,
                    returning = listOf("id")
                ).get("id")
                // Warunkowo zaktualizuj 'publication_volumes' (które stworzył trigger) używając nowego ID.
                addPublicationVolumesUpdateOperation(plan, rowData, newPublicationIdRef)
            }
        } else {
            // --- TRYB TWORZENIA ---
            // Ignorujemy podział na dodane/zmienione/usunięte.
            // Wszystkie wiersze z formularza traktujemy jako NOWE.
            publicationsResult.allCurrentRows.forEach { rowData ->
                val publicationData = mapOf(
                    "publication_type" to rowData.getCurrent("publicationType"),
                    "status" to rowData.getCurrent("status"),
                    "track_progress" to rowData.getCurrent("trackProgress"),
                    "title_id" to titleIdRef // Używamy referencji do ID nowo tworzonego tytułu
                )

                val newPublicationIdRef = plan.insert(
                    tableName = "publications",
                    data = publicationData,
                    returning = listOf("id")
                ).get("id")

                addPublicationVolumesUpdateOperation(plan, rowData, newPublicationIdRef)
            }
        }

        val result = dataAccess.executeTransactionPlan(plan.build())
        when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                return FormActionResult.Failure
            }
            is DataResult.Success<*> -> return FormActionResult.CloseScreen
        }
    }

    /**
     * Metoda pomocnicza, która hermetyzuje logikę aktualizacji 'publication_volumes'.
     * Działa zarówno dla znanych ID (edycja), jak i dla referencji do przyszłych ID (dodawanie).
     *
     * @param plan Plan transakcji
     * @param rowData Dane z wiersza, z których pobieramy informacje o wolumenach i fladze 'track_progress'.
     * @param publicationIdRef Referencja do ID publikacji (może być Value lub FromResult).
     */
    private fun addPublicationVolumesUpdateOperation(
        plan: TransactionPlan,
        rowData: FormResultData,
        publicationIdRef: TransactionValue
    ) {
        if (rowData["trackProgress"]!!.currentValue == true) {
            val volumesData = mapOf(
                "volumes" to rowData.getCurrent("volumes"),
                "translated_volumes" to rowData.getCurrent("translatedVolumes"),
                "chapters" to rowData.getCurrent("chapters"),
                "translated_chapters" to rowData.getCurrent("translatedChapters"),
                "original_completed" to rowData.getCurrent("originalCompleted")
            )

            plan.update(
                tableName = "publication_volumes",
                data = volumesData,
                // Filtrujemy po ID publikacji, używając przekazanej referencji.
                filter = mapOf("publication_id" to publicationIdRef)
            )
        }
    }
}