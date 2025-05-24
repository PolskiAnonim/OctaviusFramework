package org.octavius.novels.domain.asian

import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.domain.PublicationLanguage
import org.octavius.novels.domain.PublicationStatus
import org.octavius.novels.domain.PublicationType
import org.octavius.novels.form.*
import org.octavius.novels.form.control.ComparisonType
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.DependencyType
import org.octavius.novels.form.control.type.*
import org.octavius.novels.form.control.type.repeatable.RepeatableResultValue
import kotlin.collections.forEach

class AsianMediaForm(id: Int? = null) : Form() {

    init {
        if (id != null) {
            loadData(id)
        } else {
            clearForm()
        }
    }

    override fun initData(): Map<String, Any?> {
        // Dla nowego formularza, załaduj pustą listę publikacji
        return if (loadedId == null) {
            mapOf(
                "publications" to listOf(
                    mapOf(
                        "publicationType" to PublicationType.WebNovel,
                        "status" to PublicationStatus.NotReading,
                        "trackProgress" to false
                    )
                )
            )
        } else {
            // Dla istniejącego, załaduj publikacje z bazy
            val publications = DatabaseManager.executeQuery(
                """
                SELECT 
                    p.id,
                    p.publication_type, 
                    p.status, 
                    p.track_progress,
                    pv.volumes,
                    pv.translated_volumes,
                    pv.chapters,
                    pv.translated_chapters,
                    pv.original_completed
                FROM publications p
                LEFT JOIN publication_volumes pv ON p.id = pv.publication_id
                WHERE p.title_id = ?
                """.trimIndent(),
                listOf(loadedId)
            ).map { row ->
                val data = mutableMapOf<String, Any?>()
                data["id"] = row[ColumnInfo("publications", "id")]
                data["publicationType"] = row[ColumnInfo("publications", "publication_type")]
                data["status"] = row[ColumnInfo("publications", "status")]
                data["trackProgress"] = row[ColumnInfo("publications", "track_progress")]

                // Dane z publication_volumes jeśli istnieją
                if (data["trackProgress"] == true) {
                    data["volumes"] = row[ColumnInfo("publication_volumes", "volumes")]
                    data["translatedVolumes"] = row[ColumnInfo("publication_volumes", "translated_volumes")]
                    data["chapters"] = row[ColumnInfo("publication_volumes", "chapters")]
                    data["translatedChapters"] = row[ColumnInfo("publication_volumes", "translated_chapters")]
                    data["originalCompleted"] = row[ColumnInfo("publication_volumes", "original_completed")]
                }

                data
            }

            mapOf("publications" to publications)
        }
    }

    override fun defineTableRelations(): List<TableRelation> {
        return listOf(
            TableRelation("titles"),
        )
    }

    override fun createSchema(): FormControls {
        return FormControls(
            mapOf(
                "titleInfo" to SectionControl(
                    ctrls = listOf("titles", "language"),
                    collapsible = false,
                    initiallyExpanded = true,
                    columns = 2,
                    label = "Informacje o tytule"
                ),
                "titles" to TextListControl(
                    ColumnInfo("titles", "titles"),
                    "Tytuły",
                    required = true
                ),
                "language" to EnumControl(
                    ColumnInfo("titles", "language"),
                    "Język oryginału",
                    PublicationLanguage::class,
                    required = true
                ),

                // Sekcja publikacji - używa RepeatableControl
                "publications" to RepeatableControl(
                    rowControls = createPublicationControls(),
                    rowOrder = listOf(
                        "publicationType",
                        "status",
                        "trackProgress",
                        "volumes",
                        "translatedVolumes",
                        "chapters",
                        "translatedChapters",
                        "originalCompleted"
                    ),
                    uniqueFields = listOf("publicationType"),
                    minRows = 1,
                    label = "Publikacje"
                )
            ),
            listOf("titleInfo", "publications")
        )
    }

    private fun createPublicationControls(): Map<String, Control<*>> {
        return mapOf(
            "id" to HiddenControl<Int>(null),
            "publicationType" to EnumControl(
                null,
                "Typ publikacji",
                PublicationType::class,
                required = true
            ),
            "status" to EnumControl(
                null,
                "Status czytania",
                PublicationStatus::class,
                required = true
            ),
            "trackProgress" to BooleanControl(
                null,
                "Śledzić postęp?"
            ),
            "volumes" to IntegerControl(
                null,
                "Liczba tomów",
                dependencies = mapOf(
                    "visible" to ControlDependency(
                        controlName = "trackProgress",
                        value = true,
                        dependencyType = DependencyType.Visible,
                        comparisonType = ComparisonType.Equals
                    )
                )
            ),
            "translatedVolumes" to IntegerControl(
                null,
                "Przetłumaczone tomy",
                dependencies = mapOf(
                    "visible" to ControlDependency(
                        controlName = "trackProgress",
                        value = true,
                        dependencyType = DependencyType.Visible,
                        comparisonType = ComparisonType.Equals
                    )
                )
            ),
            "chapters" to IntegerControl(
                null,
                "Liczba rozdziałów",
                dependencies = mapOf(
                    "visible" to ControlDependency(
                        controlName = "trackProgress",
                        value = true,
                        dependencyType = DependencyType.Visible,
                        comparisonType = ComparisonType.Equals
                    )
                )
            ),
            "translatedChapters" to IntegerControl(
                null,
                "Przetłumaczone rozdziały",
                dependencies = mapOf(
                    "visible" to ControlDependency(
                        controlName = "trackProgress",
                        value = true,
                        dependencyType = DependencyType.Visible,
                        comparisonType = ComparisonType.Equals
                    )
                )
            ),
            "originalCompleted" to BooleanControl(
                null,
                "Oryginał ukończony",
                dependencies = mapOf(
                    "visible" to ControlDependency(
                        controlName = "trackProgress",
                        value = true,
                        dependencyType = DependencyType.Visible,
                        comparisonType = ComparisonType.Equals
                    )
                )
            )
        )
    }

    override fun processFormData(formData: Map<String, ControlResultData>): List<SaveOperation> {
        val result = mutableListOf<SaveOperation>()

        // Obsługa tabeli titles
        val titleData = mutableMapOf<String, ControlResultData>()
        titleData["titles"] = formData["titles"]!!
        titleData["language"] = formData["language"]!!

        val titleId = if (loadedId != null) {
            result.add(SaveOperation.Update("titles", titleData, loadedId!!))
            loadedId!!
        } else {
            result.add(SaveOperation.Insert("titles", titleData))
            null // Będzie wypełnione przez DatabaseManager
        }

        // Obsługa publikacji - pobierz dane z RepeatableControl
        val publicationsResult = formData["publications"]!!.value as RepeatableResultValue

        // Usunięte publikacje
        publicationsResult.deletedRows.forEach { rowData ->
            val pubId = rowData["id"]!!.value as Int
            // publication_volumes zostanie usunięte kaskadowo
            result.add(SaveOperation.Delete("publications", pubId))
        }

        // Dodane publikacje
        publicationsResult.addedRows.forEach { rowData ->
            val publicationData = mutableMapOf<String, ControlResultData>()
            publicationData["publication_type"] = rowData["publicationType"]!!
            publicationData["status"] = rowData["status"]!!
            publicationData["track_progress"] = rowData["trackProgress"]!!


            result.add(
                SaveOperation.Insert(
                    "publications",
                    publicationData,
                    // Dla braku title value jest nullem i będzie podstawione w DatabaseManager
                    listOf(ForeignKey("title_id", "titles", titleId))
                )
            )

            // Jeśli track_progress = true TRIGGER sprawi iż tabelka będzie istnieć
            if (rowData["trackProgress"]!!.value == true) {
                val volumesData = mutableMapOf<String, ControlResultData>()
                volumesData["volumes"] = rowData["volumes"]!!
                volumesData["translated_volumes"] = rowData["translatedVolumes"]!!
                volumesData["chapters"] = rowData["chapters"]!!
                volumesData["translated_chapters"] = rowData["translatedChapters"]!!
                volumesData["original_completed"] = rowData["originalCompleted"]!!

                result.add(
                    SaveOperation.Insert(
                        "publication_volumes",
                        volumesData,
                        listOf(ForeignKey("publication_id", "publications"))
                    )
                )
            }
        }

        // Zmodyfikowane publikacje
        publicationsResult.modifiedRows.forEach { rowData ->
            val pubId = rowData["id"]!!.value as Int

            val publicationData = mutableMapOf<String, ControlResultData>()
            publicationData["publication_type"] = rowData["publicationType"]!!
            publicationData["status"] = rowData["status"]!!
            publicationData["track_progress"] = rowData["trackProgress"]!!

            result.add(
                SaveOperation.Update(
                    "asian_media.publications",
                    publicationData,
                    pubId
                )
            )

            // Obsługa publication_volumes
            val trackProgress = rowData["trackProgress"]!!.value as Boolean

            if (trackProgress) {
                val volumesData = mutableMapOf<String, ControlResultData>()
                volumesData["volumes"] = rowData["volumes"]!!
                volumesData["translated_volumes"] = rowData["translatedVolumes"]!!
                volumesData["chapters"] = rowData["chapters"]!!
                volumesData["translated_chapters"] = rowData["translatedChapters"]!!
                volumesData["original_completed"] = rowData["originalCompleted"]!!
            }
        }

        return result
    }
}