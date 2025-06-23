package org.octavius.domain.game.form

import org.octavius.domain.GameStatus
import org.octavius.form.ColumnInfo
import org.octavius.form.component.FormSchema
import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.*
import org.octavius.form.control.type.container.SectionControl
import org.octavius.form.control.type.primitive.*
import org.octavius.form.control.type.selection.DatabaseControl
import org.octavius.form.control.type.selection.EnumControl
import org.octavius.localization.Translations

class GameFormSchemaBuilder : FormSchemaBuilder() {
    override fun build(): FormSchema {
        return FormSchema(
            mapOf(
                // Podstawowe dane
                "visibleCharactersSection" to BooleanControl(
                    null,
                    Translations.get("games.form.visibleCharacterSection"),
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
                    Translations.get("games.general.gameName"),
                    required = true
                ),
                "series" to DatabaseControl(
                    ColumnInfo("games", "series"),
                    label = Translations.get("games.form.series"),
                    relatedTable = "series",
                    displayColumn = "name"
                ),
                "status" to EnumControl(
                    ColumnInfo("games", "status"),
                    Translations.get("games.form.status"),
                    GameStatus::class,
                    required = true
                ),
                "basicInfo" to SectionControl(
                    ctrls = listOf("name", "series", "status", "visibleCharactersSection"),
                    collapsible = false,
                    initiallyExpanded = true,
                    columns = 1,
                    label = Translations.get("games.form.basicInfo")
                ),
                // Sekcja czasu gry
                "playTimeHours" to DoubleControl(
                    ColumnInfo("play_time", "play_time_hours"),
                    Translations.get("games.form.playTimeHours"),
                    validationOptions = DoubleValidation(min = 0.0, decimalPlaces = 2)
                ),
                "completionCount" to IntegerControl(
                    ColumnInfo("play_time", "completion_count"),
                    Translations.get("games.form.playCount"),
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
                    label = Translations.get("games.form.playTime"),
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
                    Translations.get("games.form.storyRating"),
                    validationOptions = IntegerValidation(min = 0, max = 10)
                ),
                "gameplayRating" to IntegerControl(
                    ColumnInfo("ratings", "gameplay_rating"),
                    Translations.get("games.form.gameplayRating"),
                    required = true,
                    validationOptions = IntegerValidation(min = 0, max = 10)
                ),
                "atmosphereRating" to IntegerControl(
                    ColumnInfo("ratings", "atmosphere_rating"),
                    Translations.get("games.form.atmosphereRating"),
                    validationOptions = IntegerValidation(min = 0, max = 10)
                ),
                "ratingsSection" to SectionControl(
                    ctrls = listOf("storyRating", "gameplayRating", "atmosphereRating"),
                    collapsible = true,
                    initiallyExpanded = true,
                    columns = 3,
                    label = Translations.get("games.form.ratings"),
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
                    Translations.get("games.form.distinctiveCharacters"),
                    required = true
                ),
                "hasDistinctiveProtagonist" to BooleanControl(
                    ColumnInfo("characters", "has_distinctive_protagonist"),
                    Translations.get("games.form.distinctiveProtagonist"),
                    required = true
                ),
                "hasDistinctiveAntagonist" to BooleanControl(
                    ColumnInfo("characters", "has_distinctive_antagonist"),
                    Translations.get("games.form.distinctiveAntagonist"),
                    required = true
                ),
                "charactersSection" to SectionControl(
                    ctrls = listOf("hasDistinctiveCharacter", "hasDistinctiveProtagonist", "hasDistinctiveAntagonist"),
                    collapsible = true,
                    initiallyExpanded = true,
                    columns = 1,
                    label = Translations.get("games.form.characters"),
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