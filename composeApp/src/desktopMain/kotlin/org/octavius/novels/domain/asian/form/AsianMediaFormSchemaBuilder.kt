package org.octavius.novels.domain.asian.form

import org.octavius.novels.domain.PublicationLanguage
import org.octavius.novels.domain.PublicationStatus
import org.octavius.novels.domain.PublicationType
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.component.FormSchema
import org.octavius.novels.form.component.FormSchemaBuilder
import org.octavius.novels.form.control.ComparisonType
import org.octavius.novels.form.control.Control
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.DependencyType
import org.octavius.novels.form.control.type.BooleanControl
import org.octavius.novels.form.control.type.EnumControl
import org.octavius.novels.form.control.type.HiddenControl
import org.octavius.novels.form.control.type.IntegerControl
import org.octavius.novels.form.control.type.RepeatableControl
import org.octavius.novels.form.control.type.SectionControl
import org.octavius.novels.form.control.type.TextListControl

class AsianMediaFormSchemaBuilder : FormSchemaBuilder() {
    override fun build(): FormSchema {
        return FormSchema(
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
                "Śledzić postęp?",
                required = true
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
                required = true,
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
}