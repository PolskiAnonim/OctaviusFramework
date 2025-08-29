package org.octavius.modules.asian.form

import org.octavius.data.contract.DataResult
import org.octavius.data.contract.DatabaseStep
import org.octavius.data.contract.DatabaseValue
import org.octavius.data.contract.toDatabaseValue
import org.octavius.form.ControlResultData
import org.octavius.form.TableRelation
import org.octavius.form.component.FormDataManager
import org.octavius.form.control.type.repeatable.RepeatableResultValue
import org.octavius.navigation.AppRouter
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.FormActionResult

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

        val result = dataFetcher.select(
            "p.id, p.publication_type, p.status, p.track_progress, pv.volumes, pv.translated_volumes, pv.chapters, pv.translated_chapters, pv.original_completed",
            from = "publications p LEFT JOIN publication_volumes pv ON p.id = pv.publication_id"
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

    override fun definedFormActions(): Map<String, (Map<String, ControlResultData>, Int?) -> FormActionResult> {
        return mapOf(
            "SAVE" to { formData, loadedId -> processSave(formData, loadedId) },
            "CANCEL" to { _, _ -> FormActionResult.CloseScreen }
        )
    }

    fun processSave(formData: Map<String, ControlResultData>, loadedId: Int?): FormActionResult {
        val databaseSteps = mutableListOf<DatabaseStep>()

        // =================================================================================
        // Główna encja 'titles' i jej referencja ID
        // =================================================================================
        val titleIdRef: DatabaseValue

        val titleData = mapOf(
            "titles" to formData["titles"]!!.currentValue.toDatabaseValue(),
            "language" to formData["language"]!!.currentValue.toDatabaseValue()
        )

        if (loadedId != null) {
            // TRYB EDYCJI: ID jest znane.
            titleIdRef = loadedId.toDatabaseValue()
            databaseSteps.add(DatabaseStep.Update(
                tableName = "titles",
                data = titleData,
                filter = mapOf("id" to titleIdRef)
            ))
        } else {
            // TRYB TWORZENIA: ID będzie znane po wykonaniu operacji 0.
            titleIdRef = DatabaseValue.FromStep(0, "id")
            databaseSteps.add(DatabaseStep.Insert(
                tableName = "titles",
                data = titleData,
                returning = listOf("id")
            ))
        }

        // =================================================================================
        // KROK 2: Obsługa pod-encji 'publications' (Repeatable Field)
        // =================================================================================
        val publicationsResult = formData["publications"]!!.currentValue as RepeatableResultValue

        // --- Usunięte publikacje ---
        publicationsResult.deletedRows.forEach { rowData ->
            val pubId = rowData["id"]!!.initialValue as Int
            // Usuwamy publikację. Tabela 'publication_volumes' zostanie usunięta kaskadowo przez DB.
            databaseSteps.add(DatabaseStep.Delete(
                tableName = "publications",
                filter = mapOf("id" to pubId.toDatabaseValue())
            ))
        }

        // --- Zmodyfikowane publikacje ---
        publicationsResult.modifiedRows.forEach { rowData ->
            val pubId = rowData["id"]!!.initialValue as Int
            val publicationIdRef = pubId.toDatabaseValue() // ID tej publikacji jest znane.
            val publicationData = mapOf(
                "publication_type" to rowData["publicationType"]!!.currentValue.toDatabaseValue(),
                "status" to rowData["status"]!!.currentValue.toDatabaseValue(),
                "track_progress" to rowData["trackProgress"]!!.currentValue.toDatabaseValue()
            )

            // Zaktualizuj samą publikację
            databaseSteps.add(DatabaseStep.Update(
                tableName = "publications",
                data = publicationData,
                filter = mapOf("id" to publicationIdRef)
            ))

            // Warunkowo zaktualizuj powiązane 'publication_volumes'
            addPublicationVolumesUpdateOperation(databaseSteps, rowData, publicationIdRef)
        }

        // --- Dodane publikacje ---
        publicationsResult.addedRows.forEach { rowData ->
            // Zapisujemy indeks operacji, która wstawi nową publikację.
            val publicationInsertOpIndex = databaseSteps.size

            val publicationData = mapOf(
                "publication_type" to rowData["publicationType"]!!.currentValue.toDatabaseValue(),
                "status" to rowData["status"]!!.currentValue.toDatabaseValue(),
                "track_progress" to rowData["trackProgress"]!!.currentValue.toDatabaseValue(),
                // Używamy referencji do ID głównego tytułu (czy to nowego, czy edytowanego)
                "title_id" to titleIdRef
            )

            // Wstaw nową publikację i ZWRÓĆ JEJ ID, bo będzie potrzebne w kolejnym kroku.
            databaseSteps.add(DatabaseStep.Insert(
                tableName = "publications",
                data = publicationData,
                returning = listOf("id")
            ))

            // ID tej publikacji nie jest jeszcze znane. Tworzymy referencję do wyniku poprzedniej operacji.
            val newPublicationIdRef = DatabaseValue.FromStep(publicationInsertOpIndex, "id")

            // Warunkowo zaktualizuj 'publication_volumes' (które stworzył trigger) używając nowego ID.
            addPublicationVolumesUpdateOperation(databaseSteps, rowData, newPublicationIdRef)
        }
        val result = batchExecutor.execute(databaseSteps)
        when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                return FormActionResult.Failure
            }
            is DataResult.Success<*> -> return FormActionResult.Success
        }
    }

    /**
     * Metoda pomocnicza, która hermetyzuje logikę aktualizacji 'publication_volumes'.
     * Działa zarówno dla znanych ID (edycja), jak i dla referencji do przyszłych ID (dodawanie).
     *
     * @param databaseSteps Lista operacji, do której zostanie dodany ewentualny UPDATE.
     * @param rowData Dane z wiersza, z których pobieramy informacje o wolumenach i fladze 'track_progress'.
     * @param publicationIdRef Referencja do ID publikacji (może być Value lub FromResult).
     */
    private fun addPublicationVolumesUpdateOperation(
        databaseSteps: MutableList<DatabaseStep>,
        rowData: Map<String, ControlResultData>,
        publicationIdRef: DatabaseValue
    ) {
        if (rowData["trackProgress"]!!.currentValue == true) {
            val volumesData = mapOf(
                "volumes" to rowData["volumes"]!!.currentValue.toDatabaseValue(),
                "translated_volumes" to rowData["translatedVolumes"]!!.currentValue.toDatabaseValue(),
                "chapters" to rowData["chapters"]!!.currentValue.toDatabaseValue(),
                "translated_chapters" to rowData["translatedChapters"]!!.currentValue.toDatabaseValue(),
                "original_completed" to rowData["originalCompleted"]!!.currentValue.toDatabaseValue()
            )

            databaseSteps.add(DatabaseStep.Update(
                tableName = "publication_volumes",
                data = volumesData,
                // Filtrujemy po ID publikacji, używając przekazanej referencji.
                filter = mapOf("publication_id" to publicationIdRef)
            ))
        }
    }
}