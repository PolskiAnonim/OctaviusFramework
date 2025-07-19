package org.octavius.modules.asian.form

import org.octavius.database.DatabaseManager
import org.octavius.form.ControlResultData
import org.octavius.form.ForeignKey
import org.octavius.form.SaveOperation
import org.octavius.form.TableRelation
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


    override fun processFormData(formData: Map<String, ControlResultData>, loadedId: Int?): List<SaveOperation> {
        val result = mutableListOf<SaveOperation>()

        // Obsługa tabeli titles
        val titleData = mutableMapOf<String, Any?>()
        titleData["titles"] = formData["titles"]!!.currentValue
        titleData["language"] = formData["language"]!!.currentValue

        val titleId = if (loadedId != null) {
            result.add(SaveOperation.Update("titles", titleData, loadedId))
            loadedId
        } else {
            result.add(SaveOperation.Insert("titles", titleData))
            null
        }

        // Obsługa publikacji
        val publicationsResult = formData["publications"]!!.currentValue as RepeatableResultValue

        // Usunięte publikacje
        publicationsResult.deletedRows.forEach { rowData ->
            val pubId = rowData["id"]!!.initialValue as Int
            // publication_volumes zostanie usunięte kaskadowo
            result.add(SaveOperation.Delete("publications", pubId))
        }

        // Dodane publikacje
        publicationsResult.addedRows.forEach { rowData ->
            val publicationData = mutableMapOf<String, Any?>()
            publicationData["publication_type"] = rowData["publicationType"]!!.currentValue
            publicationData["status"] = rowData["status"]!!.currentValue
            publicationData["track_progress"] = rowData["trackProgress"]!!.currentValue

            result.add(
                SaveOperation.Insert(
                    "publications",
                    publicationData,
                    // Dla braku titleId value jest nullem i będzie podstawione w DatabaseManager
                    listOf(ForeignKey("title_id", "titles", titleId))
                )
            )

            // Jeśli track_progress = true, UPDATE volumes (trigger już stworzył wiersz)
            if (rowData["trackProgress"]!!.currentValue == true) {
                val volumesData = mutableMapOf<String, Any?>()
                volumesData["volumes"] = rowData["volumes"]!!.currentValue
                volumesData["translated_volumes"] = rowData["translatedVolumes"]!!.currentValue
                volumesData["chapters"] = rowData["chapters"]!!.currentValue
                volumesData["translated_chapters"] = rowData["translatedChapters"]!!.currentValue
                volumesData["original_completed"] = rowData["originalCompleted"]!!.currentValue

                result.add(
                    SaveOperation.Update(
                        "publication_volumes",
                        volumesData,
                        id = null, // Nie wiadomo czy id istnieje
                        foreignKeys = listOf(ForeignKey("publication_id", "publications"))
                    )
                )
            }
        }

        // Zmodyfikowane publikacje
        publicationsResult.modifiedRows.forEach { rowData ->
            val pubId = rowData["id"]!!.initialValue as Int

            val publicationData = mutableMapOf<String, Any?>()
            publicationData["publication_type"] = rowData["publicationType"]!!.currentValue
            publicationData["status"] = rowData["status"]!!.currentValue
            publicationData["track_progress"] = rowData["trackProgress"]!!.currentValue

            result.add(
                SaveOperation.Update(
                    "publications",
                    publicationData,
                    pubId
                )
            )

            // Obsługa publication_volumes
            val trackProgress = rowData["trackProgress"]!!.currentValue as Boolean

            if (trackProgress) {
                val volumesData = mutableMapOf<String, Any?>()
                volumesData["volumes"] = rowData["volumes"]!!.currentValue
                volumesData["translated_volumes"] = rowData["translatedVolumes"]!!.currentValue
                volumesData["chapters"] = rowData["chapters"]!!.currentValue
                volumesData["translated_chapters"] = rowData["translatedChapters"]!!.currentValue
                volumesData["original_completed"] = rowData["originalCompleted"]!!.currentValue

                result.add(
                    SaveOperation.Update(
                        "publication_volumes",
                        volumesData,
                        foreignKeys = listOf(ForeignKey("publication_id", "publications", pubId))
                    )
                )
            }
        }

        return result
    }
}