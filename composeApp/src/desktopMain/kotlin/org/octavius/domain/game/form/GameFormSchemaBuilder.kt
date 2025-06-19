package org.octavius.domain.game.form

import org.octavius.form.ColumnInfo
import org.octavius.domain.GameStatus
import org.octavius.form.component.FormSchema
import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.*
import org.octavius.form.control.type.container.SectionControl
import org.octavius.form.control.type.primitive.*
import org.octavius.form.control.type.selection.DatabaseControl
import org.octavius.form.control.type.selection.EnumControl

class GameFormSchemaBuilder : FormSchemaBuilder() {
    override fun build(): FormSchema {
        return FormSchema(
            mapOf(
                // Podstawowe dane
                "visibleCharactersSection" to BooleanControl(
                    null,
                    "Widoczna sekcja postaci",
                    required = true
                ),
                "playTimeExists" to HiddenControl<Boolean>(
                    null
                ),
                "ratingsExists" to HiddenControl<Boolean>(
                    null
                ),
                "charactersExists" to HiddenControl<Boolean>(
                    null
                ),
                // Sekcja podstawowych informacji
                "name" to TextControl(
                    ColumnInfo("games", "name"),
                    "Nazwa gry",
                    required = true
                ),
                "series" to DatabaseControl(
                    ColumnInfo("games", "series"),
                    label = "Seria",
                    relatedTable = "series",
                    displayColumn = "name"
                ),
                "status" to EnumControl(
                    ColumnInfo("games", "status"),
                    "Status",
                    GameStatus::class,
                    required = true
                ),
                "basicInfo" to SectionControl(
                    ctrls = listOf("name", "series", "status", "visibleCharactersSection"),
                    collapsible = false,
                    initiallyExpanded = true,
                    columns = 1,
                    label = "Podstawowe informacje"
                ),
                // Sekcja czasu gry
                "playTimeHours" to DoubleControl(
                    ColumnInfo("play_time", "play_time_hours"),
                    "Czas gry (godziny)",
                    validationOptions = DoubleValidation(min = 0.0, decimalPlaces = 2)
                ),
                "completionCount" to IntegerControl(
                    ColumnInfo("play_time", "completion_count"),
                    "Liczba przejść",
                    required = true, // Automatycznie pomijana walidacja jak niewidoczna kontrolka
                    dependencies = mapOf(
                        "visible" to ControlDependency(
                            controlName = "status",
                            value = listOf(GameStatus.Playing, GameStatus.Played),
                            dependencyType = DependencyType.Visible,
                            comparisonType = ComparisonType.OneOf
                        )
                    ),
                    validationOptions = IntegerValidation(min = 0)
                ),
                "playTimeSection" to SectionControl(
                    ctrls = listOf("playTimeHours", "completionCount"),
                    collapsible = false,
                    initiallyExpanded = true,
                    columns = 2,
                    label = "Czas gry",
                    dependencies = mapOf(
                        "visible" to ControlDependency(
                            controlName = "status",
                            value = listOf(
                                GameStatus.Playing,
                                GameStatus.Played,
                                GameStatus.WithoutTheEnd
                            ),
                            dependencyType = DependencyType.Visible,
                            comparisonType = ComparisonType.OneOf
                        )
                    )
                ),
                // Sekcja ocen
                "storyRating" to IntegerControl(
                    ColumnInfo("ratings", "story_rating"),
                    "Ocena fabuły (1-10)",
                    validationOptions = IntegerValidation(min = 0, max = 10)
                ),
                "gameplayRating" to IntegerControl(
                    ColumnInfo("ratings", "gameplay_rating"),
                    "Ocena rozgrywki (1-10)",
                    required = true,
                    validationOptions = IntegerValidation(min = 0, max = 10)
                ),
                "atmosphereRating" to IntegerControl(
                    ColumnInfo("ratings", "atmosphere_rating"),
                    "Ocena atmosfery (1-10)",
                    validationOptions = IntegerValidation(min = 0, max = 10)
                ),
                "ratingsSection" to SectionControl(
                    ctrls = listOf("storyRating", "gameplayRating", "atmosphereRating"),
                    collapsible = true,
                    initiallyExpanded = true,
                    columns = 3,
                    label = "Oceny",
                    dependencies = mapOf(
                        "visible" to ControlDependency(
                            controlName = "status",
                            value = listOf(GameStatus.Played, GameStatus.WithoutTheEnd),
                            dependencyType = DependencyType.Visible,
                            comparisonType = ComparisonType.OneOf
                        )
                    )
                ),
                // Sekcja postaci
                "hasDistinctiveCharacter" to BooleanControl(
                    ColumnInfo("characters", "has_distinctive_character"),
                    "Charakterystyczne postacie",
                    required = true
                ),
                "hasDistinctiveProtagonist" to BooleanControl(
                    ColumnInfo("characters", "has_distinctive_protagonist"),
                    "Charakterystyczny protagonista",
                    required = true
                ),
                "hasDistinctiveAntagonist" to BooleanControl(
                    ColumnInfo("characters", "has_distinctive_antagonist"),
                    "Charakterystyczny antagonista",
                    required = true
                ),
                "charactersSection" to SectionControl(
                    ctrls = listOf("hasDistinctiveCharacter", "hasDistinctiveProtagonist", "hasDistinctiveAntagonist"),
                    collapsible = true,
                    initiallyExpanded = true,
                    columns = 1,
                    label = "Postacie",
                    dependencies = mapOf(
                        "visible" to ControlDependency(
                            controlName = "visibleCharactersSection",
                            value = true,
                            dependencyType = DependencyType.Visible,
                            comparisonType = ComparisonType.Equals
                        )
                    )
                )
            ),
            listOf(
                "basicInfo",
                "playTimeSection",
                "ratingsSection",
                "charactersSection"
            )
        )
    }
}