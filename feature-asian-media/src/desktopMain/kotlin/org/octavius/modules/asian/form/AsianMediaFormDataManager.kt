package org.octavius.modules.asian.form

import org.octavius.data.contract.DataResult
import org.octavius.data.contract.toDatabaseValue
import org.octavius.data.contract.transaction.DatabaseValue
import org.octavius.data.contract.transaction.StepReference
import org.octavius.data.contract.transaction.TransactionPlan
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.FormActionResult
import org.octavius.form.component.FormDataManager
import org.octavius.form.component.TableRelation
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.getCurrent
import org.octavius.form.control.type.repeatable.RepeatableResultValue

class AsianMediaFormDataManager : FormDataManager() {

    override fun defineTableRelations(): List<TableRelation> {
        return listOf(
            TableRelation("titles"),
        )
    }

    override fun initData(loadedId: Int?, payload: Map<String, Any?>?): Map<String, Any?> {
        val initialData = mutableMapOf<String, Any?>()

        if (loadedId != null) {
            initialData.putAll(loadPublications(loadedId))
        } else {
            initialData["publications"] = emptyList<Map<String, Any?>>()
        }

        return if (payload != null) {
            initialData + payload
        } else {
            initialData
        }
    }

    /**
     * Prywatna metoda pomocnicza do ładowania powiązanych publikacji.
     * Utrzymuje metodę initData w czystości.
     */
    private fun loadPublications(titleId: Int): Map<String, Any?> {
        val result = dataAccess.select(
            "p.id, p.publication_type, p.status, p.track_progress, pv.volumes, pv.translated_volumes, pv.chapters, pv.translated_chapters, pv.original_completed"
        ).from("publications p LEFT JOIN publication_volumes pv ON p.id = pv.publication_id"
        ).where("p.title_id = :title").toList(mapOf("title" to titleId))

        return when (result) {
            is DataResult.Success -> {
                val publications = result.value.map { row ->
                    buildMap {
                        put("id", row["id"])
                        put("publicationType", row["publication_type"])
                        put("status", row["status"])
                        put("trackProgress", row["track_progress"])
                        put("volumes", row["volumes"])
                        put("translatedVolumes", row["translated_volumes"])
                        put("chapters", row["chapters"])
                        put("translatedChapters", row["translated_chapters"])
                        put("originalCompleted", row["original_completed"])
                    }
                }

                mapOf("publications" to publications)
            }
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                mapOf("publications" to emptyList<Map<String, Any?>>())
            }
        }
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
        val steps = plan.build()
        val result = batchExecutor.execute(steps)
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
        val titleIdRef: DatabaseValue // Zamiast Any?, używamy docelowego typu

        val titleData = mapOf(
            "titles" to formResultData.getCurrent("titles"),
            "language" to formResultData.getCurrent("language")
        )

        if (loadedId != null) {
            // TRYB EDYCJI: ID jest znane.
            titleIdRef = loadedId.toDatabaseValue()
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
                val publicationIdRef = pubId.toDatabaseValue() // ID tej publikacji jest znane.
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

        val transactionSteps = plan.build()
        val result = batchExecutor.execute(transactionSteps)
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
        publicationIdRef: DatabaseValue
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