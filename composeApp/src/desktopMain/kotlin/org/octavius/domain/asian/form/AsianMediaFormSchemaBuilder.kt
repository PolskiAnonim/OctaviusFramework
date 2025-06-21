package org.octavius.domain.asian.form

import org.octavius.domain.PublicationLanguage
import org.octavius.domain.PublicationStatus
import org.octavius.domain.PublicationType
import org.octavius.form.ColumnInfo
import org.octavius.form.component.FormSchema
import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.*
import org.octavius.form.control.type.collection.TextListControl
import org.octavius.form.control.type.container.SectionControl
import org.octavius.form.control.type.primitive.BooleanControl
import org.octavius.form.control.type.primitive.HiddenControl
import org.octavius.form.control.type.primitive.IntegerControl
import org.octavius.form.control.type.repeatable.RepeatableControl
import org.octavius.form.control.type.selection.EnumControl

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
                    required = true,
                    validationOptions = TextListValidation(minItems = 1)
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
                    validationOptions = RepeatableValidation(
                        uniqueFields = listOf("publicationType"),
                        minItems = 1,
                        maxItems = 7
                    ),
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
                        comparisonType = ComparisonType.Equals,
                        scope = DependencyScope.Local
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
                        comparisonType = ComparisonType.Equals,
                        scope = DependencyScope.Local
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
                        comparisonType = ComparisonType.Equals,
                        scope = DependencyScope.Local
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
                        comparisonType = ComparisonType.Equals,
                        scope = DependencyScope.Local
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
                        comparisonType = ComparisonType.Equals,
                        scope = DependencyScope.Local
                    )
                )
            )
        )
    }
}