package org.octavius.modules.games.form.game

import org.octavius.data.DataResult
import org.octavius.data.transaction.TransactionPlan
import org.octavius.data.transaction.TransactionValue
import org.octavius.data.transaction.toTransactionValue
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.domain.game.GameStatus
import org.octavius.form.component.FormActionResult
import org.octavius.form.component.FormDataManager
import org.octavius.form.control.base.FormResultData
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
            mapOf("visibleCharactersSection" to false, "categories" to emptyList<Map<String, Any?>>())
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
        val plan = TransactionPlan(dataAccess)
        val statusesWithDetails = listOf(GameStatus.WithoutTheEnd, GameStatus.Playing, GameStatus.Played)

        // =================================================================================
        val gameIdRef: TransactionValue

        // Dane dla głównej tabeli 'games'.
        val gameData = mapOf(
            "name" to formResultData["name"]!!.currentValue,
            "series" to formResultData["series"]!!.currentValue,
            "status" to formResultData["status"]!!.currentValue
        )

        // W zależności od tego, czy tworzymy nową grę, czy edytujemy istniejącą,
        // nasza referencja do ID będzie albo stałą wartością, albo odwołaniem do wyniku przyszłej operacji.
        if (loadedId != null) {
            // === TRYB EDYCJI ===
            // ID gry jest znane, więc używamy stałej wartości.
            gameIdRef = loadedId.toTransactionValue()

            // Operacja 0: Aktualizuj grę. Nie potrzebujemy niczego zwracać.
            plan.update(
                tableName = "games",
                data = gameData,
                filter = mapOf("id" to gameIdRef),
            )
        } else {
            // === TRYB TWORZENIA ===
            // Operacja 0: Wstaw nową grę i zwróć jej wygenerowane 'id'.
            gameIdRef = plan.insert(
                tableName = "games",
                data = gameData,
                returning = listOf("id")
            ).field("id")
        }

        // Od tego momentu wszystkie operacje na tabelach zależnych używają `gameIdRef`,

        val status = formResultData["status"]!!.currentValue as GameStatus

        // =================================================================================
        // KROK 2: Obsługa tabel zależnych (1-do-1)
        // =================================================================================

        // --- Obsługa Play Time ---
        handleDependentTable(
            plan = plan,
            exists = formResultData["playTimeExists"]!!.currentValue as Boolean,
            conditionMet = status in statusesWithDetails,
            tableName = "play_time",
            data = mapOf(
                "play_time_hours" to formResultData["playTimeHours"]!!.currentValue,
                "completion_count" to formResultData["completionCount"]!!.currentValue
            ),
            gameIdRef = gameIdRef
        )

        // --- Obsługa Ratings ---
        handleDependentTable(
            plan = plan,
            exists = formResultData["ratingsExists"]!!.currentValue as Boolean,
            conditionMet = status in statusesWithDetails,
            tableName = "ratings",
            data = mapOf(
                "story_rating" to formResultData["storyRating"]!!.currentValue,
                "gameplay_rating" to formResultData["gameplayRating"]!!.currentValue,
                "atmosphere_rating" to formResultData["atmosphereRating"]!!.currentValue
            ),
            gameIdRef = gameIdRef
        )

        // --- Obsługa Characters ---
        handleDependentTable(
            plan = plan,
            exists = formResultData["charactersExists"]!!.currentValue as Boolean,
            conditionMet = formResultData["visibleCharactersSection"]!!.currentValue as Boolean,
            tableName = "characters",
            data = mapOf(
                "has_distinctive_character" to formResultData["hasDistinctiveCharacter"]!!.currentValue,
                "has_distinctive_protagonist" to formResultData["hasDistinctiveProtagonist"]!!.currentValue,
                "has_distinctive_antagonist" to formResultData["hasDistinctiveAntagonist"]!!.currentValue
            ),
            gameIdRef = gameIdRef
        )

        // =================================================================================
        // KROK 3: Obsługa tabeli łączącej (many-to-many)
        // =================================================================================

        val categoriesResult = formResultData["categories"]!!.currentValue as RepeatableResultValue

        // Usunięte kategorie
        categoriesResult.deletedRows.forEach { rowData ->
            val categoryId = rowData["category"]!!.initialValue as Int
            plan.delete(
                tableName = "categories_to_games",
                filter = mapOf(
                    "game_id" to gameIdRef,
                    "category_id" to categoryId
                )
            )
        }

        // Zmodyfikowane kategorie (DELETE + INSERT)
        categoriesResult.modifiedRows.forEach { rowData ->
            val oldCategoryId = rowData["category"]!!.initialValue as Int
            val newCategoryId = rowData["category"]!!.currentValue
            // Usuń stare powiązanie
            plan.delete(
                tableName = "categories_to_games",
                filter = mapOf(
                    "game_id" to gameIdRef,
                    "category_id" to oldCategoryId
                )
            )
            // Dodaj nowe powiązanie
            plan.insert(
                tableName = "categories_to_games",
                data = mapOf(
                    "game_id" to gameIdRef,
                    "category_id" to newCategoryId
                )
            )
        }

        // Dodane kategorie
        categoriesResult.addedRows.forEach { rowData ->
            val categoryId = rowData["category"]!!.currentValue
            plan.insert(
                tableName = "categories_to_games",
                data = mapOf(
                    "game_id" to gameIdRef,
                    "category_id" to categoryId
                )
            )
        }

        val result = dataAccess.executeTransactionPlan(plan.build())
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
        plan: TransactionPlan,
        exists: Boolean,
        conditionMet: Boolean,
        tableName: String,
        data: Map<String, Any?>,
        gameIdRef: TransactionValue
    ) {
        if (exists) {
            if (conditionMet) {
                // Rekord istnieje i warunek jest spełniony -> UPDATE
                plan.update(
                    tableName = tableName,
                    data = data,
                    filter = mapOf("game_id" to gameIdRef)
                )
            } else {
                // Rekord istnieje, ale warunek nie jest spełniony -> DELETE
                plan.delete(
                    tableName = tableName,
                    filter = mapOf("game_id" to gameIdRef)
                )
            }
        } else if (conditionMet) {
            // Rekord nie istnieje, ale warunek jest spełniony -> INSERT
            // Klucz obcy `game_id` jest częścią danych do wstawienia.
            val dataWithFk = data + mapOf("game_id" to gameIdRef)
            plan.insert(
                tableName = tableName,
                data = dataWithFk
            )
        }
    }
}