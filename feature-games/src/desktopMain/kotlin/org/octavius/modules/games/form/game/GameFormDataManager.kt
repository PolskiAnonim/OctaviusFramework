package org.octavius.modules.games.form.game

import org.octavius.data.DataResult
import org.octavius.data.builder.execute
import org.octavius.data.builder.toField
import org.octavius.data.transaction.TransactionPlan
import org.octavius.data.transaction.TransactionValue
import org.octavius.data.transaction.toTransactionValue
import org.octavius.data.type.PgStandardType
import org.octavius.data.type.withPgType
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.domain.game.GameStatus
import org.octavius.form.component.FormActionResult
import org.octavius.form.component.FormDataManager
import org.octavius.form.control.base.FormResultData
import org.octavius.form.control.base.getCurrent
import org.octavius.form.control.base.getCurrentAs
import org.octavius.form.control.base.getInitialAs
import org.octavius.form.control.type.repeatable.RepeatableResultValue

class GameFormDataManager : FormDataManager() {

    private fun loadGameData(loadedId: Int?) = loadData(loadedId) {
        from("games", "g")

        // Proste mapowania z tabeli 'games'
        map("name")
        map("series")
        map("status")

        // Relacja 1-do-1 z 'play_time'
        mapOneToOne {
            from("play_time", "pt")
            on("g.id = pt.game_id")
            existenceFlag("playTimeExists", "pt.game_id")
            map("playTimeHours")
            map("completionCount")
        }

        // Relacja 1-do-1 z 'ratings'
        mapOneToOne {
            from("ratings", "r")
            on("g.id = r.game_id")
            existenceFlag("ratingsExists", "r.game_id")
            map("storyRating")
            map("gameplayRating")
            map("atmosphereRating")
        }

        // Relacja 1-do-1 z 'characters'
        mapOneToOne {
            from("characters", "c")
            on("g.id = c.game_id")
            existenceFlag("charactersExists", "c.game_id")
            map("hasDistinctiveCharacter")
            map("hasDistinctiveProtagonist")
            map("hasDistinctiveAntagonist")
        }

        // Relacja 1-do-N z 'categories'
        mapRelatedList("categories") {
            from("categories_to_games", "ctg")
            linkedBy("ctg.game_id")
            map("category", "category_id")
        }
    }

    override fun initData(loadedId: Int?, payload: Map<String, Any?>?): Map<String, Any?> {
        val loadedData = loadGameData(loadedId)

        val defaultData = if (loadedId == null) {
            mapOf(
                "visibleCharactersSection" to false,
                "playTimeExists" to false,
                "ratingsExists" to false,
                "charactersExists" to false,
                "categories" to emptyList<Map<String, Any?>>()
            )
        } else {
            mapOf("visibleCharactersSection" to (loadedData["charactersExists"] as Boolean))
        }

        // Kolejność łączenia: Domyślne -> Załadowane z DB -> Payload (nadpisuje wszystko)
        return defaultData + loadedData + (payload ?: emptyMap())
    }

    override fun definedFormActions(): Map<String, (FormResultData, Int?) -> FormActionResult> {
        return mapOf(
            "save" to { formData, loadedId -> processSave(formData, loadedId) },
            "cancel" to { _, _ -> FormActionResult.CloseScreen }
        )
    }

    fun processSave(formResultData: FormResultData, loadedId: Int?): FormActionResult {
        val plan = TransactionPlan()
        val statusesWithDetails = listOf(GameStatus.WithoutTheEnd, GameStatus.Playing, GameStatus.Played)

        // =================================================================================
        // KROK 1: Główna encja 'games'
        // =================================================================================
        val gameIdRef: TransactionValue

        val gameData = mapOf(
            "name" to formResultData.getCurrent("name"),
            "series" to formResultData.getCurrent("series"),
            "status" to formResultData.getCurrent("status")
        )

        // W zależności od tego, czy tworzymy nową grę, czy edytujemy istniejącą,
        // nasza referencja do ID będzie albo stałą wartością, albo odwołaniem do wyniku przyszłej operacji.
        if (loadedId != null) {
            // === TRYB EDYCJI ===
            // ID gry jest znane, więc używamy stałej wartości.
            gameIdRef = loadedId.toTransactionValue()
            // Zaktualizuj grę, przekazując wszystkie parametry
            plan.add(
                dataAccess.update("games.games")
                    .setValues(gameData)
                    .where("id = :id")
                    .asStep()
                    .execute(gameData + mapOf("id" to gameIdRef))
            )
        } else {
            // Wstaw nową grę i pobierz jej ID jako referencję
            gameIdRef = plan.add(
                dataAccess.insertInto("games.games")
                    .values(gameData)
                    .returning("id")
                    .asStep()
                    .toField<Int>(gameData) // Używamy toField, bo chcemy pojedynczą wartość
            ).field()
        }

        val status = formResultData.getCurrentAs<GameStatus>("status")

        // =================================================================================
        // KROK 2: Obsługa tabel zależnych 1-do-1
        // =================================================================================

        // --- Obsługa Play Time ---
        handleDependentTable(
            plan = plan,
            exists = formResultData.getCurrentAs("playTimeExists"),
            conditionMet = status in statusesWithDetails,
            tableName = "games.play_time",
            data = mapOf(
                "play_time_hours" to formResultData.getCurrent("playTimeHours"),
                "completion_count" to formResultData.getCurrent("completionCount")
            ),
            gameIdRef = gameIdRef
        )

        // --- Obsługa Ratings ---
        handleDependentTable(
            plan = plan,
            exists = formResultData.getCurrentAs("ratingsExists"),
            conditionMet = status in statusesWithDetails,
            tableName = "games.ratings",
            data = mapOf(
                "story_rating" to formResultData.getCurrent("storyRating"),
                "gameplay_rating" to formResultData.getCurrent("gameplayRating"),
                "atmosphere_rating" to formResultData.getCurrent("atmosphereRating")
            ),
            gameIdRef = gameIdRef
        )

        // --- Obsługa Characters ---
        handleDependentTable(
            plan = plan,
            exists = formResultData.getCurrentAs("charactersExists"),
            conditionMet = formResultData.getCurrentAs("visibleCharactersSection"),
            tableName = "games.characters",
            data = mapOf(
                "has_distinctive_character" to formResultData.getCurrent("hasDistinctiveCharacter"),
                "has_distinctive_protagonist" to formResultData.getCurrent("hasDistinctiveProtagonist"),
                "has_distinctive_antagonist" to formResultData.getCurrent("hasDistinctiveAntagonist")
            ),
            gameIdRef = gameIdRef
        )

        // =================================================================================
        // KROK 3: Obsługa tabeli łączącej many-to-many
        // =================================================================================

        val categoriesResult = formResultData.getCurrentAs<RepeatableResultValue>("categories")
        // Usunięte kategorie
        val deletedCategories = categoriesResult.deletedRows.map { rowData ->
            rowData.getInitialAs<Int>("category")
        } + categoriesResult.modifiedRows.map { rowData ->
            rowData.getInitialAs<Int>("category")
        }

        plan.add(
            dataAccess.deleteFrom("games.categories_to_games ctg")
                .using("UNNEST(:ids_to_delete) AS t(id)")
                .where("ctg.category_id = t.id AND ctg.game_id = :game_id")
                .asStep().execute("ids_to_delete" to deletedCategories.withPgType(PgStandardType.INT4_ARRAY), "game_id" to gameIdRef)
        )

        val insertedCategories = categoriesResult.addedRows.map { rowData ->
            rowData.getCurrentAs<Int>("category")
        } + categoriesResult.modifiedRows.map { rowData ->
            rowData.getCurrentAs<Int>("category")
        }

        plan.add(
            dataAccess.insertInto("games.categories_to_games", listOf("game_id", "category_id"))
                .fromSelect(dataAccess.select(":game_id","category_id").from("UNNEST(:ids_to_insert) AS category_id").toSql())
                .asStep().execute("ids_to_insert" to insertedCategories.withPgType(PgStandardType.INT4_ARRAY), "game_id" to gameIdRef)
        )

        // Wykonanie całego planu
        return when (val result = dataAccess.executeTransactionPlan(plan)) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(result.error))
                FormActionResult.Failure
            }

            is DataResult.Success -> FormActionResult.CloseScreen
        }
    }

    /**
     * Metoda pomocnicza do obsługi logiki INSERT/UPDATE/DELETE dla tabel zależnych 1-do-1.
     * Hermetyzuje powtarzalny wzorzec.
     */
    private fun handleDependentTable(
        plan: TransactionPlan,
        exists: Boolean,
        conditionMet: Boolean,
        tableName: String,
        data: Map<String, Any?>,
        gameIdRef: TransactionValue
    ) {
        if (exists) {
            if (conditionMet) { // UPDATE
                val allParams = data + mapOf("game_id" to gameIdRef)
                plan.add(
                    dataAccess.update(tableName)
                        .setValues(data)
                        .where("game_id = :game_id")
                        .asStep()
                        .execute(allParams)
                )
            } else { // DELETE
                plan.add(
                    dataAccess.deleteFrom(tableName)
                        .where("game_id = :game_id")
                        .asStep()
                        .execute("game_id" to gameIdRef)
                )
            }
        } else if (conditionMet) { // INSERT
            val allParams = data + mapOf("game_id" to gameIdRef)
            plan.add(
                dataAccess.insertInto(tableName)
                    .values(allParams)
                    .asStep()
                    .execute(allParams)
            )
        }
    }
}