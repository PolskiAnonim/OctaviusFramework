package org.octavius.novels.domain.game

import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.domain.GameStatus
import org.octavius.novels.form.*
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

class GameForm(id: Int? = null) : Form() {

    init {
        if (id != null) {
            // Ładowanie istniejącej gry
            loadData(id)
        } else {
            // Inicjalizacja formularza dla nowej gry
            clearForm()
        }
    }

    override fun initData(): Map<String, Any?> {
        // Domyślne wartości dla nowej gry
        return if (loadedId == null) {
            mapOf(
                "visibleCharactersSection" to false,
                "playTimeExists" to false,
                "ratingsExists" to false,
                "charactersExists" to false
            )
        } else {
            val dataExists = DatabaseManager.executeQuery(
                """
            SELECT CASE WHEN pt.game_id IS NULL THEN FALSE ELSE TRUE END AS play_time_exists,
                CASE WHEN c.game_id IS NULL THEN FALSE ELSE TRUE END AS characters_exists,
                CASE WHEN r.game_id IS NULL THEN FALSE ELSE TRUE END AS ratings_exists
            FROM games g LEFT JOIN characters c ON c.game_id = g.id
                LEFT JOIN play_time pt ON pt.game_id = g.id
                LEFT JOIN ratings r ON r.game_id = g.id
            WHERE g.id = ?
                """.trimIndent(),
                listOf(loadedId)
            ).first()
            mapOf(
                "visibleCharactersSection" to dataExists[ColumnInfo("", "characters_exists")]!!,
                "charactersExists" to dataExists[ColumnInfo("", "characters_exists")]!!,
                "playTimeExists" to dataExists[ColumnInfo("", "play_time_exists")]!!,
                "ratingsExists" to dataExists[ColumnInfo("", "ratings_exists")]!!
            )
        }
    }

    override fun defineTableRelations(): List<TableRelation> {
        return listOf(
            TableRelation("games"), // Główna tabela
            TableRelation("characters", "games.id = characters.game_id"), // Powiązana tabela
            TableRelation("play_time", "games.id = play_time.game_id"), // Powiązana tabela
            TableRelation("ratings", "games.id = ratings.game_id"), // Powiązana tabela
        )
    }

    override fun createSchema(): FormControls {
        return FormControls(
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

    override fun processFormData(formData: Map<String, ControlResultData>): List<SaveOperation> {
        val result = mutableListOf<SaveOperation>()

        val statusesWithDetails = listOf(GameStatus.WithoutTheEnd, GameStatus.Playing, GameStatus.Played)
        // Obsługa głównej tabeli games
        val gameData = mutableMapOf<String, ControlResultData>()
        gameData["name"] = formData["name"]!!
        gameData["series"] = formData["series"]!!
        gameData["status"] = formData["status"]!!

        if (loadedId != null) {
            result.add(SaveOperation.Update("games", gameData, loadedId!!))
        } else {
            result.add(SaveOperation.Insert("games", gameData))
        }
        // Play time
        val status = formData["status"]!!.value as GameStatus

        if (formData["playTimeExists"]!!.value as Boolean) {
            if (status in statusesWithDetails) {
                val playTimeData = mutableMapOf<String, ControlResultData>()
                playTimeData["play_time_hours"] = formData["playTimeHours"]!!
                playTimeData["completion_count"] = formData["completionCount"]!!
                result.add(
                    SaveOperation.Update(
                        "play_time",
                        playTimeData,
                        foreignKeys = listOf(ForeignKey("game_id", "games", loadedId))
                    )
                )
            } else {
                result.add(
                    SaveOperation.Delete(
                        "play_time",
                        foreignKeys = listOf(ForeignKey("game_id", "games", loadedId))
                    )
                )
            }
        } else if (status in statusesWithDetails) {
            val playTimeData = mutableMapOf<String, ControlResultData>()
            playTimeData["play_time_hours"] = formData["playTimeHours"]!!
            playTimeData["completion_count"] = formData["completionCount"]!!
            result.add(
                SaveOperation.Insert(
                    "play_time",
                    playTimeData,
                    foreignKeys = listOf(ForeignKey("game_id", "games", loadedId)),
                    returningId = false
                )
            )
        }

        if (formData["ratingsExists"]!!.value as Boolean) {
            if (status in statusesWithDetails) {
                val ratingsData = mutableMapOf<String, ControlResultData>()
                ratingsData["story_rating"] = formData["storyRating"]!!
                ratingsData["gameplay_rating"] = formData["gameplayRating"]!!
                ratingsData["atmosphere_rating"] = formData["atmosphereRating"]!!
                result.add(
                    SaveOperation.Update(
                        "ratings",
                        ratingsData,
                        foreignKeys = listOf(ForeignKey("game_id", "games", loadedId))
                    )
                )
            }
        } else if (status in statusesWithDetails) {
            val ratingsData = mutableMapOf<String, ControlResultData>()
            ratingsData["story_rating"] = formData["storyRating"]!!
            ratingsData["gameplay_rating"] = formData["gameplayRating"]!!
            ratingsData["atmosphere_rating"] = formData["atmosphereRating"]!!
            result.add(
                SaveOperation.Insert(
                    "ratings",
                    ratingsData,
                    foreignKeys = listOf(ForeignKey("game_id", "games", loadedId)),
                    returningId = false
                )
            )
        }

        if (formData["charactersExists"]!!.value as Boolean) {
            if (formData["visibleCharactersSection"]!!.value as Boolean) {
                val charactersData = mutableMapOf<String, ControlResultData>()
                charactersData["has_distinctive_character"] = formData["hasDistinctiveCharacter"]!!
                charactersData["has_distinctive_protagonist"] = formData["hasDistinctiveProtagonist"]!!
                charactersData["has_distinctive_antagonist"] = formData["hasDistinctiveAntagonist"]!!
                result.add(
                    SaveOperation.Update(
                        "characters",
                        charactersData,
                        foreignKeys = listOf(ForeignKey("game_id", "games", loadedId))
                    )
                )
            } else {
                result.add(
                    SaveOperation.Delete(
                        "characters",
                        foreignKeys = listOf(ForeignKey("game_id", "games", loadedId))
                    )
                )
            }
        } else if (formData["visibleCharactersSection"]!!.value as Boolean) {
            val charactersData = mutableMapOf<String, ControlResultData>()
            charactersData["has_distinctive_character"] = formData["hasDistinctiveCharacter"]!!
            charactersData["has_distinctive_protagonist"] = formData["hasDistinctiveProtagonist"]!!
            charactersData["has_distinctive_antagonist"] = formData["hasDistinctiveAntagonist"]!!
            result.add(
                SaveOperation.Insert(
                    "characters",
                    charactersData,
                    foreignKeys = listOf(ForeignKey("game_id", "games", loadedId)),
                    returningId = false
                )
            )
        }

        return result
    }
}