package org.octavius.modules.games.form.game

import org.octavius.data.contract.DataResult
import org.octavius.data.contract.DatabaseStep
import org.octavius.data.contract.DatabaseValue
import org.octavius.data.contract.toDatabaseValue
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.domain.game.GameStatus
import org.octavius.form.ControlResultData
import org.octavius.form.FormActionResult
import org.octavius.form.TableRelation
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

    override fun initData(loadedId: Int?, payload: Map<String, Any?>?): Map<String, Any?> {
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
            val dataExists = dataFetcher.select(
                """CASE WHEN pt.game_id IS NULL THEN FALSE ELSE TRUE END AS play_time_exists,
                CASE WHEN c.game_id IS NULL THEN FALSE ELSE TRUE END AS characters_exists,
                CASE WHEN r.game_id IS NULL THEN FALSE ELSE TRUE END AS ratings_exists
                """,
                """games g 
                    LEFT JOIN characters c ON c.game_id = g.id
                    LEFT JOIN play_time pt ON pt.game_id = g.id 
                    LEFT JOIN ratings r ON r.game_id = g.id""",
            ).where("g.id = :id").toSingle(mapOf("id" to loadedId))

            val game: Map<String, Any?>
            when (dataExists) {
                is DataResult.Failure -> throw IllegalArgumentException("Game not found")
                is DataResult.Success<Map<String, Any?>?> -> {
                    game = dataExists.value ?: throw IllegalArgumentException("Game not found")
                }
            }

            // Załaduj kategorie dla tej gry
            val catResult = dataFetcher.select(
                "ctg.category_id as category",
                "categories_to_games ctg JOIN categories c ON ctg.category_id = c.id"
            ).where("ctg.game_id = :gameId").toList(mapOf("gameId" to loadedId))

            val categories: List<Map<String, Any?>>

            when (catResult) {
                is DataResult.Failure -> throw IllegalArgumentException("Game not found")
                is DataResult.Success<List<Map<String, Any?>>> -> {
                    categories =
                        catResult.value.map { it.mapKeys { (key, _) -> if (key == "original_category") "originalCategory" else key } }
                }
            }

            mapOf(
                "visibleCharactersSection" to game["characters_exists"]!!,
                "charactersExists" to game["characters_exists"]!!,
                "playTimeExists" to game["play_time_exists"]!!,
                "ratingsExists" to game["ratings_exists"]!!,
                "categories" to categories,
            )
        }
    }

    override fun definedFormActions(): Map<String, (Map<String, ControlResultData>, Int?) -> FormActionResult> {
        return mapOf(
            "save" to { formData, loadedId -> processSave(formData, loadedId) },
            "cancel" to { _, _ -> FormActionResult.CloseScreen }
        )
    }

    fun processSave(formData: Map<String, ControlResultData>, loadedId: Int?): FormActionResult {
        val databaseSteps = mutableListOf<DatabaseStep>()
        val statusesWithDetails = listOf(GameStatus.WithoutTheEnd, GameStatus.Playing, GameStatus.Played)

        // =================================================================================
        val gameIdRef: DatabaseValue

        // Dane dla głównej tabeli 'games'.
        val gameData = mapOf(
            "name" to formData["name"]!!.currentValue.toDatabaseValue(),
            "series" to formData["series"]!!.currentValue.toDatabaseValue(),
            "status" to formData["status"]!!.currentValue.toDatabaseValue()
        )

        // W zależności od tego, czy tworzymy nową grę, czy edytujemy istniejącą,
        // nasza referencja do ID będzie albo stałą wartością, albo odwołaniem do wyniku przyszłej operacji.
        if (loadedId != null) {
            // === TRYB EDYCJI ===
            // ID gry jest znane, więc używamy stałej wartości.
            gameIdRef = loadedId.toDatabaseValue()

            // Operacja 0: Aktualizuj grę. Nie potrzebujemy niczego zwracać.
            databaseSteps.add(
                DatabaseStep.Update(
                    tableName = "games",
                    data = gameData,
                    filter = mapOf("id" to gameIdRef),
                    returning = emptyList()
                )
            )
        } else {
            // === TRYB TWORZENIA ===
            // ID gry zostanie wygenerowane, więc tworzymy referencję do wyniku operacji o indeksie 0.
            gameIdRef = DatabaseValue.FromStep(0, "id")

            // Operacja 0: Wstaw nową grę i zwróć jej wygenerowane 'id'.
            databaseSteps.add(
                DatabaseStep.Insert(
                    tableName = "games",
                    data = gameData,
                    returning = listOf("id")
                )
            )
        }

        // Od tego momentu wszystkie operacje na tabelach zależnych używają `gameIdRef`,

        val status = formData["status"]!!.currentValue as GameStatus

        // =================================================================================
        // KROK 2: Obsługa tabel zależnych (1-do-1)
        // =================================================================================

        // --- Obsługa Play Time ---
        handleDependentTable(
            databaseSteps = databaseSteps,
            exists = formData["playTimeExists"]!!.currentValue as Boolean,
            conditionMet = status in statusesWithDetails,
            tableName = "play_time",
            data = mapOf(
                "play_time_hours" to formData["playTimeHours"]!!.currentValue.toDatabaseValue(),
                "completion_count" to formData["completionCount"]!!.currentValue.toDatabaseValue()
            ),
            gameIdRef = gameIdRef
        )

        // --- Obsługa Ratings ---
        handleDependentTable(
            databaseSteps = databaseSteps,
            exists = formData["ratingsExists"]!!.currentValue as Boolean,
            conditionMet = status in statusesWithDetails,
            tableName = "ratings",
            data = mapOf(
                "story_rating" to formData["storyRating"]!!.currentValue.toDatabaseValue(),
                "gameplay_rating" to formData["gameplayRating"]!!.currentValue.toDatabaseValue(),
                "atmosphere_rating" to formData["atmosphereRating"]!!.currentValue.toDatabaseValue()
            ),
            gameIdRef = gameIdRef
        )

        // --- Obsługa Characters ---
        handleDependentTable(
            databaseSteps = databaseSteps,
            exists = formData["charactersExists"]!!.currentValue as Boolean,
            conditionMet = formData["visibleCharactersSection"]!!.currentValue as Boolean,
            tableName = "characters",
            data = mapOf(
                "has_distinctive_character" to formData["hasDistinctiveCharacter"]!!.currentValue.toDatabaseValue(),
                "has_distinctive_protagonist" to formData["hasDistinctiveProtagonist"]!!.currentValue.toDatabaseValue(),
                "has_distinctive_antagonist" to formData["hasDistinctiveAntagonist"]!!.currentValue.toDatabaseValue()
            ),
            gameIdRef = gameIdRef
        )

        // =================================================================================
        // KROK 3: Obsługa tabeli łączącej (many-to-many)
        // =================================================================================

        val categoriesResult = formData["categories"]!!.currentValue as RepeatableResultValue

        // Usunięte kategorie
        categoriesResult.deletedRows.forEach { rowData ->
            val categoryId = rowData["category"]!!.initialValue as Int
            databaseSteps.add(
                DatabaseStep.Delete(
                    tableName = "categories_to_games",
                    filter = mapOf(
                        "game_id" to gameIdRef,
                        "category_id" to categoryId.toDatabaseValue()
                    )
                )
            )
        }

        // Zmodyfikowane kategorie (DELETE + INSERT)
        categoriesResult.modifiedRows.forEach { rowData ->
            val oldCategoryId = rowData["category"]!!.initialValue as Int
            val newCategoryId = rowData["category"]!!.currentValue
            // Usuń stare powiązanie
            databaseSteps.add(
                DatabaseStep.Delete(
                    tableName = "categories_to_games",
                    filter = mapOf(
                        "game_id" to gameIdRef,
                        "category_id" to oldCategoryId.toDatabaseValue()
                    )
                )
            )
            // Dodaj nowe powiązanie
            databaseSteps.add(
                DatabaseStep.Insert(
                    tableName = "categories_to_games",
                    data = mapOf(
                        "game_id" to gameIdRef,
                        "category_id" to newCategoryId.toDatabaseValue()
                    )
                )
            )
        }

        // Dodane kategorie
        categoriesResult.addedRows.forEach { rowData ->
            val categoryId = rowData["category"]!!.currentValue
            databaseSteps.add(
                DatabaseStep.Insert(
                    tableName = "categories_to_games",
                    data = mapOf(
                        "game_id" to gameIdRef,
                        "category_id" to categoryId.toDatabaseValue()
                    )
                )
            )
        }

        val result = batchExecutor.execute(databaseSteps)
        when (result) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                return FormActionResult.Failure
            }
            is DataResult.Success<*> -> return FormActionResult.CloseScreen
        }
    }

    /**
     * Metoda pomocnicza do obsługi logiki INSERT/UPDATE/DELETE dla tabel zależnych 1-do-1.
     * Hermetyzuje powtarzalny wzorzec.
     */
    private fun handleDependentTable(
        databaseSteps: MutableList<DatabaseStep>,
        exists: Boolean,
        conditionMet: Boolean,
        tableName: String,
        data: Map<String, DatabaseValue>,
        gameIdRef: DatabaseValue
    ) {
        if (exists) {
            if (conditionMet) {
                // Rekord istnieje i warunek jest spełniony -> UPDATE
                databaseSteps.add(
                    DatabaseStep.Update(
                        tableName = tableName,
                        data = data,
                        filter = mapOf("game_id" to gameIdRef)
                    )
                )
            } else {
                // Rekord istnieje, ale warunek nie jest spełniony -> DELETE
                databaseSteps.add(
                    DatabaseStep.Delete(
                        tableName = tableName,
                        filter = mapOf("game_id" to gameIdRef)
                    )
                )
            }
        } else if (conditionMet) {
            // Rekord nie istnieje, ale warunek jest spełniony -> INSERT
            // Klucz obcy `game_id` jest częścią danych do wstawienia.
            val dataWithFk = data + mapOf("game_id" to gameIdRef)
            databaseSteps.add(
                DatabaseStep.Insert(
                    tableName = tableName,
                    data = dataWithFk
                )
            )
        }
    }
}