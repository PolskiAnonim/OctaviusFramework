package org.octavius.novels.form

import org.octavius.novels.domain.NovelStatus
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.DependencyType
import org.octavius.novels.form.control.type.*

class NovelForm : Form() {

    override fun defineTableRelations(): List<TableRelation> {
        return listOf(
            TableRelation("novels"), // Główna tabela
            TableRelation("novel_volumes", "novels.id = novel_volumes.id") // Powiązana tabela
        )
    }

    override fun createSchema(): FormControls {
        return FormControls(
            mapOf(
                "id" to HiddenControl<Int>("id", "novels"),
                "novelInfo" to SectionControl(
                        ctrls = listOf("titles", "novelType", "originalLanguage", "status"),
                        collapsible = false,
                        initiallyExpanded = true,
                        columns = 1,
                        label = "Informacje o nowelce"
                ),
                "titles" to TextListControl("titles", "novels", "Tytuły"),
                "novelType" to EnumControl("novelType", "novels", "Typ nowelki"),
                "originalLanguage" to TextControl("originalLanguage", "novels", "Język oryginału"),
                "status" to EnumControl("status", "novels", "Status czytania"),
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
                                value = listOf(
                                    NovelStatus.reading.name,
                                    NovelStatus.completed.name,
                                    NovelStatus.planToRead.name
                                ),
                                dependencyType = DependencyType.OneOf
                            )
                        )
                )
                ,
                "volumes" to IntegerControl("volumes", "novelVolumes", "Liczba tomów"),
                "translatedVolumes" to IntegerControl("translatedVolumes", "novelVolumes", "Przetłumaczone tomy"),
                "originalCompleted" to BooleanControl("originalCompleted", "novelVolumes", "Oryginał ukończony")
            ),
            listOf("novelInfo", "volumesSection")
        )
    }

}