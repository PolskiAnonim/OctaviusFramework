package org.octavius.modules.asian.form

import org.octavius.database.DatabaseManager
import org.octavius.database.DatabaseStep
import org.octavius.database.DatabaseValue
import org.octavius.database.TableRelation
import org.octavius.form.ControlResultData
import org.octavius.form.component.FormDataManager
import org.octavius.form.control.type.repeatable.RepeatableResultValue

class AsianMediaFormDataManager : FormDataManager() {

    override fun defineTableRelations(): List<TableRelation> {
        return listOf(
            TableRelation("titles"),
        )
    }

    override fun initData(loadedId: Int?): Map<String, Any?> {
        // Dla nowego formularza, załaduj pustą listę publikacji
        return if (loadedId == null) {
            mapOf(
                "publications" to listOf<Map<String, Any?>>()
            )
        } else {
            // Dla istniejącego, załaduj publikacje z bazy
            val publications = DatabaseManager.getFetcher().fetchList(
                "publications p LEFT JOIN publication_volumes pv ON p.id = pv.publication_id",
                "p.id, p.publication_type, p.status, p.track_progress, pv.volumes, pv.translated_volumes, pv.chapters, pv.translated_chapters, pv.original_completed",
                "p.title_id = :title", params = mapOf("title" to loadedId),
                ).map { row ->
                val data = mutableMapOf<String, Any?>()
                data["id"] = row["id"]
                data["publicationType"] = row["publication_type"]
                data["status"] = row["status"]
                data["trackProgress"] = row["track_progress"]

                data["volumes"] = row[ "volumes"]
                data["translatedVolumes"] = row["translated_volumes"]
                data["chapters"] = row["chapters"]
                data["translatedChapters"] = row["translated_chapters"]
                data["originalCompleted"] = row["original_completed"]

                data
            }

            mapOf("publications" to publications)
        }
    }


    override fun processFormData(formData: Map<String, ControlResultData>, loadedId: Int?): List<DatabaseStep> {
        val databaseSteps = mutableListOf<DatabaseStep>()

        // =================================================================================
        // Główna encja 'titles' i jej referencja ID
        // =================================================================================
        val titleIdRef: DatabaseValue

        val titleData = mapOf(
            "titles" to DatabaseValue.Value(formData["titles"]!!.currentValue),
            "language" to DatabaseValue.Value(formData["language"]!!.currentValue)
        )

        if (loadedId != null) {
            // TRYB EDYCJI: ID jest znane.
            titleIdRef = DatabaseValue.Value(loadedId)
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
                filter = mapOf("id" to DatabaseValue.Value(pubId))
            ))
        }

        // --- Zmodyfikowane publikacje ---
        publicationsResult.modifiedRows.forEach { rowData ->
            val pubId = rowData["id"]!!.initialValue as Int
            val publicationIdRef = DatabaseValue.Value(pubId) // ID tej publikacji jest znane.

            val publicationData = mapOf(
                "publication_type" to DatabaseValue.Value(rowData["publicationType"]!!.currentValue),
                "status" to DatabaseValue.Value(rowData["status"]!!.currentValue),
                "track_progress" to DatabaseValue.Value(rowData["trackProgress"]!!.currentValue)
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
                "publication_type" to DatabaseValue.Value(rowData["publicationType"]!!.currentValue),
                "status" to DatabaseValue.Value(rowData["status"]!!.currentValue),
                "track_progress" to DatabaseValue.Value(rowData["trackProgress"]!!.currentValue),
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

        return databaseSteps
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
                "volumes" to DatabaseValue.Value(rowData["volumes"]!!.currentValue),
                "translated_volumes" to DatabaseValue.Value(rowData["translatedVolumes"]!!.currentValue),
                "chapters" to DatabaseValue.Value(rowData["chapters"]!!.currentValue),
                "translated_chapters" to DatabaseValue.Value(rowData["translatedChapters"]!!.currentValue),
                "original_completed" to DatabaseValue.Value(rowData["originalCompleted"]!!.currentValue)
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