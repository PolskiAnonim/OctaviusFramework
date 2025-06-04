package org.octavius.novels.domain.game.form

import org.octavius.novels.domain.GameStatus
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.FormControls
import org.octavius.novels.form.component.FormSchema
import org.octavius.novels.form.component.FormSchemaBuilder
import org.octavius.novels.form.control.ComparisonType
import org.octavius.novels.form.control.ControlDependency
import org.octavius.novels.form.control.DependencyType
import org.octavius.novels.form.control.type.BooleanControl
import org.octavius.novels.form.control.type.DatabaseControl
import org.octavius.novels.form.control.type.DoubleControl
import org.octavius.novels.form.control.type.EnumControl
import org.octavius.novels.form.control.type.HiddenControl
import org.octavius.novels.form.control.type.IntegerControl
import org.octavius.novels.form.control.type.SectionControl
import org.octavius.novels.form.control.type.TextControl

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
                    )
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
                    "Ocena fabuły (1-10)"
                ),
                "gameplayRating" to IntegerControl(
                    ColumnInfo("ratings", "gameplay_rating"),
                    "Ocena rozgrywki (1-10)",
                    required = true
                ),
                "atmosphereRating" to IntegerControl(
                    ColumnInfo("ratings", "atmosphere_rating"),
                    "Ocena atmosfery (1-10)"
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