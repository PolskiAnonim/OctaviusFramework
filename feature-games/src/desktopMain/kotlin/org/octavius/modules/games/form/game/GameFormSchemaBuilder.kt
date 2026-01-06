package org.octavius.modules.games.form.game

import org.octavius.domain.game.GameStatus
import org.octavius.form.component.FormSchemaBuilder
import org.octavius.form.control.base.*
import org.octavius.form.control.type.button.ButtonControl
import org.octavius.form.control.type.button.ButtonType
import org.octavius.form.control.type.container.SectionControl
import org.octavius.form.control.type.number.DoubleControl
import org.octavius.form.control.type.number.IntegerControl
import org.octavius.form.control.type.primitive.BooleanControl
import org.octavius.form.control.type.primitive.StringControl
import org.octavius.form.control.type.repeatable.RepeatableControl
import org.octavius.form.control.type.selection.DatabaseControl
import org.octavius.form.control.type.selection.EnumControl
import org.octavius.localization.T

class GameFormSchemaBuilder : FormSchemaBuilder() {

    override fun defineControls(): Map<String, Control<*>> = mapOf(
        // Podstawowe dane
        "visibleCharactersSection" to BooleanControl(
            T.get("games.form.visibleCharacterSection"),
            required = true
        ),
        "playTimeExists" to BooleanControl(null),
        "ratingsExists" to BooleanControl(null),
        "charactersExists" to BooleanControl(null),
        // Sekcja podstawowych informacji
        "name" to StringControl(
            T.get("games.general.gameName"),
            required = true
        ),
        "series" to DatabaseControl(
            label = T.get("games.form.series"),
            relatedTable = "series",
            displayColumn = "name"
        ),
        "status" to EnumControl(
            T.get("games.form.status"),
            GameStatus::class,
            required = true
        ),
        "basicInfo" to SectionControl(
            controls = listOf("name", "series", "status", "visibleCharactersSection"),
            collapsible = false,
            initiallyExpanded = true,
            columns = 1,
            label = T.get("games.form.basicInfo")
        ),
        // Sekcja czasu gry
        "playTimeHours" to DoubleControl(
            T.get("games.form.playTimeHours"),
            validationOptions = DoubleValidation(min = 0.0, decimalPlaces = 2)
        ),
        "completionCount" to IntegerControl(
            T.get("games.form.playCount"),
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
            controls = listOf("playTimeHours", "completionCount"),
            collapsible = false,
            initiallyExpanded = true,
            columns = 2,
            label = T.get("games.form.playTime"),
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
            T.get("games.form.storyRating"),
            validationOptions = IntegerValidation(min = 0, max = 10)
        ),
        "gameplayRating" to IntegerControl(
            T.get("games.form.gameplayRating"),
            required = true,
            validationOptions = IntegerValidation(min = 0, max = 10)
        ),
        "atmosphereRating" to IntegerControl(
            T.get("games.form.atmosphereRating"),
            validationOptions = IntegerValidation(min = 0, max = 10)
        ),
        "ratingsSection" to SectionControl(
            controls = listOf("storyRating", "gameplayRating", "atmosphereRating"),
            collapsible = true,
            initiallyExpanded = true,
            columns = 3,
            label = T.get("games.form.ratings"),
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
            T.get("games.form.distinctiveCharacters"),
            required = true
        ),
        "hasDistinctiveProtagonist" to BooleanControl(
            T.get("games.form.distinctiveProtagonist"),
            required = true
        ),
        "hasDistinctiveAntagonist" to BooleanControl(
            T.get("games.form.distinctiveAntagonist"),
            required = true
        ),
        "charactersSection" to SectionControl(
            controls = listOf("hasDistinctiveCharacter", "hasDistinctiveProtagonist", "hasDistinctiveAntagonist"),
            collapsible = true,
            initiallyExpanded = true,
            columns = 1,
            label = T.get("games.form.characters"),
            dependencies = mapOf(
                "visible" to ControlDependency(
                    controlName = "visibleCharactersSection",
                    value = true,
                    dependencyType = DependencyType.Visible,
                    comparisonType = ComparisonType.Equals
                )
            )
        ),
        // Sekcja kategorii
        "categories" to RepeatableControl(
            rowControls = mapOf(
                "category" to DatabaseControl(
                    label = T.getPlural("games.form.category", 1),
                    relatedTable = "games.categories",
                    displayColumn = "name",
                    required = true
                )
            ),
            rowOrder = listOf("category"),
            label = T.getPlural("games.form.category", 2),
            validationOptions = RepeatableValidation(
                minItems = 0,
                maxItems = 10,
                uniqueFields = listOf("category")
            )
        ),
        // Przyciski
        "saveButton" to ButtonControl(
            text = T.get("action.save"),
            actions = listOf(
                ControlAction {
                    trigger.triggerAction("save", true)
                }
            ),
            buttonType = ButtonType.Filled
        ),
        "cancelButton" to ButtonControl(
            text = T.get("action.cancel"),
            actions = listOf(
                ControlAction {
                    trigger.triggerAction("cancel", false)
                }
            ),
            buttonType = ButtonType.Outlined
        )
    )

    override fun defineContentOrder(): List<String> = listOf(
        "basicInfo",
        "playTimeSection",
        "ratingsSection",
        "charactersSection",
        "categories"
    )

    override fun defineActionBarOrder(): List<String> = listOf(
        "cancelButton",
        "saveButton"
    )

}