package org.octavius.novels.domain.novel

import org.octavius.novels.domain.NovelLanguage
import org.octavius.novels.domain.NovelStatus
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.ControlResultData
import org.octavius.novels.form.ForeignKey
import org.octavius.novels.form.Form
import org.octavius.novels.form.FormControls
import org.octavius.novels.form.SaveOperation
import org.octavius.novels.form.TableRelation
import org.octavius.novels.form.control.ComparisonType
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.DependencyType
import org.octavius.novels.form.control.type.BooleanControl
import org.octavius.novels.form.control.type.EnumControl
import org.octavius.novels.form.control.type.IntegerControl
import org.octavius.novels.form.control.type.SectionControl
import org.octavius.novels.form.control.type.TextControl
import org.octavius.novels.form.control.type.TextListControl

class NovelForm(id: Int? = null) : Form() {

    init {
        if (id != null) {
            // Ładowanie istniejącej nowelki
            loadData(id)
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
                    columns = 2,
                    label = "Informacje o nowelce"
                ),
                "titles" to TextListControl(ColumnInfo("novels", "titles"), "Tytuły"),
                "novelType" to TextControl(ColumnInfo("novels", "novel_type"), "Typ nowelki"),
                "originalLanguage" to EnumControl(
                    ColumnInfo("novels", "original_language"),
                    "Język oryginału",
                    NovelLanguage::class,
                    required = true
                ),
                "status" to EnumControl(
                    ColumnInfo("novels", "status"),
                    "Status czytania",
                    NovelStatus::class,
                    required = true
                ),
                // Sekcja tomów (widoczna tylko dla określonych statusów)
                "volumesSection" to SectionControl(
                    ctrls = listOf("volumes", "translatedVolumes", "originalCompleted"),
                    collapsible = true,
                    initiallyExpanded = true,
                    columns = 2,
                    label = "Informacje o tomach",
                    dependencies = mapOf(
                        "statusDependency" to ControlDependency(
                            controlName = "status",
                            value = NovelStatus.NotReading,
                            dependencyType = DependencyType.Visible,
                            comparisonType = ComparisonType.NotEquals
                        )
                    )
                ),
                "volumes" to IntegerControl(ColumnInfo("novel_volumes", "volumes"), "Liczba tomów", required = true),
                "translatedVolumes" to IntegerControl(
                    ColumnInfo("novel_volumes", "translated_volumes"),
                    "Przetłumaczone tomy",
                    required = true
                ),
                "originalCompleted" to BooleanControl(
                    ColumnInfo("novel_volumes", "original_completed"),
                    "Oryginał ukończony",
                    required = true
                )
            ),
            listOf("novelInfo", "volumesSection")
        )
    }

    override fun processFormData(formData: Map<String, Map<String, ControlResultData>>): List<SaveOperation> {
        val result = mutableListOf<SaveOperation>()

        var novelStatus = ControlResultData(NovelStatus.NotReading, true)
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

        if (novelStatus.value == NovelStatus.NotReading) {
            if (novelStatus.dirty && loadedId != null) {
                result.add(SaveOperation.Delete("novel_volumes", loadedId!!))
            }
        } else {
            // Czytam
            if (novelStatus.dirty) {
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