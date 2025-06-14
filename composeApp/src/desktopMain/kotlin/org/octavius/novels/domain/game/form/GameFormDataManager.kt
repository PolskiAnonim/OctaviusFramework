package org.octavius.novels.domain.game.form

import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.domain.GameStatus
import org.octavius.novels.form.ControlResultData
import org.octavius.novels.form.ForeignKey
import org.octavius.novels.form.SaveOperation
import org.octavius.novels.form.TableRelation
import org.octavius.novels.form.component.FormDataManager

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
                "charactersExists" to false
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
            mapOf(
                "visibleCharactersSection" to dataExists["characters_exists"]!!,
                "charactersExists" to dataExists["characters_exists"]!!,
                "playTimeExists" to dataExists["play_time_exists"]!!,
                "ratingsExists" to dataExists["ratings_exists"]!!
            )
        }
    }


    override fun processFormData(formData: Map<String, ControlResultData>, loadedId: Int?): List<SaveOperation> {
        val result = mutableListOf<SaveOperation>()

        val statusesWithDetails = listOf(GameStatus.WithoutTheEnd, GameStatus.Playing, GameStatus.Played)
        // Obsługa głównej tabeli games
        val gameData = mutableMapOf<String, ControlResultData>()
        gameData["name"] = formData["name"]!!
        gameData["series"] = formData["series"]!!
        gameData["status"] = formData["status"]!!

        if (loadedId != null) {
            result.add(SaveOperation.Update("games", gameData, loadedId))
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