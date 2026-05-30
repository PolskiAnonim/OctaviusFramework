package org.octavius.modules.sandbox.form

import org.octavius.form.component.FormActionResult
import org.octavius.form.component.FormDataManager
import org.octavius.form.control.base.FormResultData
import org.octavius.localization.Tr
import org.octavius.modules.sandbox.domain.SandboxPriority
import org.octavius.ui.snackbar.SnackbarManager
import kotlin.system.measureTimeMillis

class SandboxFormDataManager : FormDataManager() {

    private fun testSqlGeneration() {
        try {
            val result: Map<String, Any?>
            val time = measureTimeMillis {
                result = loadData(1) {
                    from("games.games", "g")
                    map("id")
                    map("name")

                    mapRelatedList("level_1") {
                        from("games.play_time", "pt")
                        linkedBy("pt.game_id")
                        map("play_time_hours")

                        mapRelatedList("level_2") {
                            from("asian_media.publications", "p")
                            linkedBy("p.title_id", "pt.game_id")
                            map("publication_type")

                            mapRelatedList("level_3") {
                                from("finances.splits", "tr")
                                linkedBy("tr.account_id", "p.id")
                                map("amount")
                            }
                        }
                    }
                }
            }
            println("Test SQL Generation Result: $result")
        } catch (e: Exception) {
            println("Test SQL Generation Failed: $e")
        }
    }

    override fun initData(payload: Map<String, Any?>): Map<String, Any?> {
        testSqlGeneration()
        return mapOf(
            "basic_info" to mapOf(
                "name" to "",
                "quantity" to null,
                "active" to false,
                "priority" to SandboxPriority.Medium,
                "start_date" to null,
                "tags" to emptyList<String>()
            ),
            "elements" to emptyList<Map<String, Any?>>(),
            "level_1" to listOf(
                mapOf(
                    "level_1_name" to "Pierwszy L1",
                    "level_2" to listOf(
                        mapOf(
                            "level_2_name" to "Pierwszy L2",
                            "level_3" to listOf(
                                mapOf("level_3_name" to "L3-1", "level_3_value" to 10),
                                mapOf("level_3_name" to "L3-2", "level_3_value" to 20)
                            )
                        ),
                        mapOf(
                            "level_2_name" to "Drugi L2",
                            "level_3" to emptyList<Map<String, Any?>>()
                        )
                    )
                ),
                mapOf(
                    "level_1_name" to "Drugi L1",
                    "level_2" to listOf(
                        mapOf(
                            "level_2_name" to "Trzeci L2",
                            "level_3" to listOf(
                                mapOf("level_3_name" to "L3-3", "level_3_value" to 30)
                            )
                        )
                    )
                )
            )
        ) + payload
    }

    override fun definedFormActions(): Map<String, (formResultData: FormResultData) -> FormActionResult> {
        return mapOf(
            "save" to { _ ->
                SnackbarManager.showMessage(Tr.Sandbox.Form.savedMessage())
                FormActionResult.CloseScreen
            },
            "cancel" to { _ ->
                FormActionResult.CloseScreen
            }
        )
    }
}
