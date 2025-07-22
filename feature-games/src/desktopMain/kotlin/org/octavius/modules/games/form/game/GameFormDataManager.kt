package org.octavius.modules.games.form.game

import org.octavius.database.DatabaseManager
import org.octavius.domain.game.GameStatus
import org.octavius.form.ControlResultData
import org.octavius.database.ForeignKey
import org.octavius.database.SaveOperation
import org.octavius.database.TableRelation
import org.octavius.form.component.FormDataManager
import org.octavius.form.control.type.repeatable.RepeatableResultValue

class GameFormDataManager : FormDataManager() {
    override fun defineTableRelations(): List<TableRelation> {
        return listOf(
            TableRelation("games"), // Główna tabela
            TableRelation("characters", "games.id = characters.game_id"), // Powiązana tabela
            TableRelation("play_time", "games.id = play_time.game_id"), // Powiązana tabela
            TableRelation("ratings", "games.id = ratings.game_id"), // Powiązana tabela
        )
    }

    override fun initData(loadedId: Int?): Map<String, Any?> {
        // Domyślne wartości dla nowej gry
        return if (loadedId == null) {
            mapOf(
                "visibleCharactersSection" to false,
                "playTimeExists" to false,
                "ratingsExists" to false,
                "charactersExists" to false,
                "categories" to emptyList<Map<String, Any?>>()
            )
        } else {
            val dataExists = DatabaseManager.getFetcher().fetchRow(
                """games g 
                    LEFT JOIN characters c ON c.game_id = g.id
                    LEFT JOIN play_time pt ON pt.game_id = g.id 
                    LEFT JOIN ratings r ON r.game_id = g.id""",
                """CASE WHEN pt.game_id IS NULL THEN FALSE ELSE TRUE END AS play_time_exists,
                CASE WHEN c.game_id IS NULL THEN FALSE ELSE TRUE END AS characters_exists,
                CASE WHEN r.game_id IS NULL THEN FALSE ELSE TRUE END AS ratings_exists
                """,
                "g.id = :id", mapOf("id" to loadedId)
            )

            // Załaduj kategorie dla tej gry
            val categories = DatabaseManager.getFetcher().fetchList(
                "categories_to_games ctg JOIN categories c ON ctg.category_id = c.id",
                "ctg.category_id as category",
                "ctg.game_id = :gameId",
                params = mapOf("gameId" to loadedId)
            )

            mapOf(
                "visibleCharactersSection" to dataExists["characters_exists"]!!,
                "charactersExists" to dataExists["characters_exists"]!!,
                "playTimeExists" to dataExists["play_time_exists"]!!,
                "ratingsExists" to dataExists["ratings_exists"]!!,
                "categories" to categories.map { it.mapKeys { (key, _) -> if (key == "original_category") "originalCategory" else key } },
            )
        }
    }


    override fun processFormData(formData: Map<String, ControlResultData>, loadedId: Int?): List<SaveOperation> {
        val result = mutableListOf<SaveOperation>()

        val statusesWithDetails = listOf(GameStatus.WithoutTheEnd, GameStatus.Playing, GameStatus.Played)
        // Obsługa głównej tabeli games
        val gameData = mutableMapOf<String, Any?>()
        gameData["name"] = formData["name"]!!.currentValue
        gameData["series"] = formData["series"]!!.currentValue
        gameData["status"] = formData["status"]!!.currentValue

        if (loadedId != null) {
            result.add(SaveOperation.Update("games", gameData, loadedId))
        } else {
            result.add(SaveOperation.Insert("games", gameData))
        }
        // Play time
        val status = formData["status"]!!.currentValue as GameStatus

        if (formData["playTimeExists"]!!.currentValue as Boolean) {
            if (status in statusesWithDetails) {
                val playTimeData = mutableMapOf<String, Any?>()
                playTimeData["play_time_hours"] = formData["playTimeHours"]!!.currentValue
                playTimeData["completion_count"] = formData["completionCount"]!!.currentValue
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
            val playTimeData = mutableMapOf<String, Any?>()
            playTimeData["play_time_hours"] = formData["playTimeHours"]!!.currentValue
            playTimeData["completion_count"] = formData["completionCount"]!!.currentValue
            result.add(
                SaveOperation.Insert(
                    "play_time",
                    playTimeData,
                    foreignKeys = listOf(ForeignKey("game_id", "games", loadedId)),
                    returningId = false
                )
            )
        }

        if (formData["ratingsExists"]!!.currentValue as Boolean) {
            if (status in statusesWithDetails) {
                val ratingsData = mutableMapOf<String, Any?>()
                ratingsData["story_rating"] = formData["storyRating"]!!.currentValue
                ratingsData["gameplay_rating"] = formData["gameplayRating"]!!.currentValue
                ratingsData["atmosphere_rating"] = formData["atmosphereRating"]!!.currentValue
                result.add(
                    SaveOperation.Update(
                        "ratings",
                        ratingsData,
                        foreignKeys = listOf(ForeignKey("game_id", "games", loadedId))
                    )
                )
            }
        } else if (status in statusesWithDetails) {
            val ratingsData = mutableMapOf<String, Any?>()
            ratingsData["story_rating"] = formData["storyRating"]!!.currentValue
            ratingsData["gameplay_rating"] = formData["gameplayRating"]!!.currentValue
            ratingsData["atmosphere_rating"] = formData["atmosphereRating"]!!.currentValue
            result.add(
                SaveOperation.Insert(
                    "ratings",
                    ratingsData,
                    foreignKeys = listOf(ForeignKey("game_id", "games", loadedId)),
                    returningId = false
                )
            )
        }

        if (formData["charactersExists"]!!.currentValue as Boolean) {
            if (formData["visibleCharactersSection"]!!.currentValue as Boolean) {
                val charactersData = mutableMapOf<String, Any?>()
                charactersData["has_distinctive_character"] = formData["hasDistinctiveCharacter"]!!.currentValue
                charactersData["has_distinctive_protagonist"] = formData["hasDistinctiveProtagonist"]!!.currentValue
                charactersData["has_distinctive_antagonist"] = formData["hasDistinctiveAntagonist"]!!.currentValue
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
        } else if (formData["visibleCharactersSection"]!!.currentValue as Boolean) {
            val charactersData = mutableMapOf<String, Any?>()
            charactersData["has_distinctive_character"] = formData["hasDistinctiveCharacter"]!!.currentValue
            charactersData["has_distinctive_protagonist"] = formData["hasDistinctiveProtagonist"]!!.currentValue
            charactersData["has_distinctive_antagonist"] = formData["hasDistinctiveAntagonist"]!!.currentValue
            result.add(
                SaveOperation.Insert(
                    "characters",
                    charactersData,
                    foreignKeys = listOf(ForeignKey("game_id", "games", loadedId)),
                    returningId = false
                )
            )
        }

        // Obsługa kategorii
        val categoriesResult = formData["categories"]!!.currentValue as RepeatableResultValue

        // Usunięte kategorie
        categoriesResult.deletedRows.forEach { rowData ->
            val categoryId = rowData["category"]!!.initialValue as Int
            result.add(
                SaveOperation.Delete(
                    "categories_to_games",
                    foreignKeys = listOf(
                        ForeignKey("game_id", "games", loadedId),
                        ForeignKey("category_id", "categories", categoryId)
                    )
                )
            )
        }

        // Zmodyfikowane kategorie - dla many-to-many DELETE starej + INSERT nowej
        categoriesResult.modifiedRows.forEach { rowData ->
            val categoryData = mutableMapOf<String, Any?>()
            val category = rowData["category"]!!
            categoryData["category_id"] = category.currentValue

            result.add(
                SaveOperation.Delete(
                    "categories_to_games",
                    foreignKeys = listOf(
                        ForeignKey("game_id", "games", loadedId),
                        ForeignKey("category_id", "categories", category.initialValue as Int)
                    ),
                )
            )

            result.add(
                SaveOperation.Insert(
                    "categories_to_games",
                    categoryData,
                    foreignKeys = listOf(ForeignKey("game_id", "games", loadedId)),
                    returningId = false
                )
            )
        }

        // Dodane kategorie
        categoriesResult.addedRows.forEach { rowData ->
            val categoryData = mutableMapOf<String, Any?>()
            categoryData["category_id"] = rowData["category"]!!.currentValue

            result.add(
                SaveOperation.Insert(
                    "categories_to_games",
                    categoryData,
                    foreignKeys = listOf(ForeignKey("game_id", "games", loadedId)),
                    returningId = false
                )
            )
        }

        return result
    }
}