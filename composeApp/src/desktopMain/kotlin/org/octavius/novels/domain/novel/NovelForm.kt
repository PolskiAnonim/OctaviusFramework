package org.octavius.novels.domain.novel

import org.octavius.novels.domain.PublicationLanguage
import org.octavius.novels.domain.PublicationStatus
import org.octavius.novels.form.*
import org.octavius.novels.form.control.ComparisonType
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.DependencyType
import org.octavius.novels.form.control.type.*

class NovelForm(id: Int? = null) : Form() {

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
                "SELECT publication_type, status, track_progress FROM publications WHERE title_id = ?",
                listOf(loadedId)
            ).map { row ->
                mapOf(
                    "publicationType" to row[ColumnInfo("publications", "publication_type")],
                    "status" to row[ColumnInfo("publications", "status")],
                    "trackProgress" to row[ColumnInfo("publications", "track_progress")]
                )
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
                "titles" to TextListControl(ColumnInfo("titles", "titles"), "Tytuły"),
                "language" to EnumControl(
                    ColumnInfo("titles", "language"),
                    "Język oryginału",
                    PublicationLanguage::class,
                    required = true
                )
            ),
            listOf("titleInfo", "titles", "language")
        )
    }

    override fun processFormData(formData: Map<String, Map<String, ControlResultData>>): List<SaveOperation> {
        val result = mutableListOf<SaveOperation>()

        var publicationStatus = ControlResultData(PublicationStatus.NotReading, true)
        // Obsłuż tabelę główną
        formData["novels"]?.let { data ->
            result.add(
                if (loadedId != null) SaveOperation.Update(
                    "novels",
                    data,
                    loadedId!!
                ) else SaveOperation.Insert("novels", data)
            )
            publicationStatus = data["status"]!!
        }

        if (publicationStatus.value == PublicationStatus.NotReading) {
            if (publicationStatus.dirty && loadedId != null) {
                result.add(SaveOperation.Delete("novel_volumes", loadedId!!))
            }
        } else {
            // Czytam
            if (publicationStatus.dirty) {
                result.add(
                    SaveOperation.Insert(
                        "novel_volumes", formData["novel_volumes"]!!,
                        listOf(ForeignKey("id", "novels", loadedId))
                    )
                )
            } else {
                result.add(SaveOperation.Update("novel_volumes", formData["novel_volumes"]!!, loadedId!!))
            }
        }

        return result
    }
}