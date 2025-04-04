package org.octavius.novels.form

import org.octavius.novels.domain.NovelStatus
import org.octavius.novels.form.control.ComparisonType
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.DependencyType
import org.octavius.novels.form.control.type.*

class NovelForm(id: Int? = null) : Form() {

    init {
        initialize(id)
    }

    private fun initialize(novelId: Int?) {
        if (novelId != null) {
            // Ładowanie istniejącej nowelki
            loadData(novelId)
        } else {
            // Inicjalizacja formularza dla nowej nowelki
            clearForm()
        }
    }

    override fun defineTableRelations(): List<TableRelation> {
        return listOf(
            TableRelation("novels"), // Główna tabela
            TableRelation("novel_volumes", "novels.id = novel_volumes.id") // Powiązana tabela
        )
    }


    override fun createSchema(): FormControls {
        return FormControls(
            mapOf(
                "novelInfo" to SectionControl(
                    ctrls = listOf("titles", "novelType", "originalLanguage", "status"),
                    collapsible = false,
                    initiallyExpanded = true,
                    columns = 1,
                    label = "Informacje o nowelce"
                ),
                "titles" to TextListControl("titles", "novels", "Tytuły"),
                "novelType" to TextControl("novel_type", "novels", "Typ nowelki"),
                "originalLanguage" to TextControl("original_language", "novels", "Język oryginału"),
                "status" to EnumControl("status", "novels", "Status czytania", NovelStatus::class, required = true),
                // Sekcja tomów (widoczna tylko dla określonych statusów)
                "volumesSection" to SectionControl(
                    ctrls = listOf("volumes", "translatedVolumes", "originalCompleted"),
                    collapsible = true,
                    initiallyExpanded = true,
                    columns = 1,
                    label = "Informacje o tomach",
                    dependencies = mapOf(
                        "statusDependency" to ControlDependency(
                            controlName = "status",
                            value = NovelStatus.notReading,
                            dependencyType = DependencyType.Visible,
                            comparisonType = ComparisonType.NotEquals
                        )
                    )
                ),
                "volumes" to IntegerControl("volumes", "novel_volumes", "Liczba tomów", required = true),
                "translatedVolumes" to IntegerControl("translated_volumes", "novel_volumes", "Przetłumaczone tomy", required = true),
                "originalCompleted" to BooleanControl("original_completed", "novel_volumes", "Oryginał ukończony", required = true)
            ),
            listOf("novelInfo", "volumesSection")
        )
    }

    override fun processFormData(formData: Map<String, Map<String, ControlResultData>>): List<SaveOperation> {
        val result = mutableListOf<SaveOperation>()

        var novelStatus = ControlResultData(NovelStatus.notReading, true)
        // Obsłuż tabelę główną
        formData["novels"]?.let { data ->
            result.add(
                if (loadedId != null) SaveOperation.Update(
                    "novels",
                    data,
                    loadedId!!
                ) else SaveOperation.Insert("novels", data)
            )
            novelStatus = data["status"]!!
        }

        if (novelStatus.value == NovelStatus.notReading) {
            if (novelStatus.dirty && loadedId != null) {
                result.add(SaveOperation.Delete("novel_volumes", loadedId!!))
            }
        } else {
            // Czytam
            if (novelStatus.dirty) {
                result.add(SaveOperation.Insert("novel_volumes", formData["novel_volumes"]!!,
                    listOf(ForeignKey("id","novels.id", loadedId))))
            } else {
                result.add(SaveOperation.Update("novel_volumes", formData["novel_volumes"]!!, loadedId!!))
            }
        }

        return result
    }

}