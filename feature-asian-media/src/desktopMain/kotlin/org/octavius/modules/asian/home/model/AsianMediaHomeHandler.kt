package org.octavius.modules.asian.home.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.contract.DataFetcher
import org.octavius.data.contract.DataResult
import org.octavius.data.contract.toSingleOf
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.exception.DatabaseException

class AsianMediaHomeHandler : KoinComponent {
    private val dataFetcher: DataFetcher by inject()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow(AsianMediaHomeState())
    val state = _state.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {

        scope.launch {
            val query = """
            SELECT
            -- Sekcja statystyk
            (SELECT COUNT(*) FROM asian_media.titles) AS total_titles,
            (SELECT COUNT(DISTINCT title_id) FROM asian_media.publications WHERE status = 'READING') AS reading_count,
            (SELECT COUNT(DISTINCT p1.title_id) FROM asian_media.publications p1
                WHERE p1.status = 'COMPLETED' AND NOT EXISTS
                    (SELECT 1 FROM asian_media.publications p2 WHERE p2.title_id = p1.title_id AND p2.status = 'READING')) AS completed_count,
            
            -- Sekcja "currently reading" zagregowana do tablicy typu dashboard_item
            (SELECT array_agg(ROW(t.id, t.titles[1])::asian_media.dashboard_item)
             FROM (
                 SELECT t.id, t.titles
                 FROM asian_media.titles t
                 WHERE EXISTS (SELECT 1 FROM asian_media.publications p WHERE p.title_id = t.id AND p.status = 'READING')
                 ORDER BY t.updated_at DESC
                 LIMIT 5
             ) t) AS currently_reading,
             
            -- Sekcja "recently added" zagregowana do tablicy typu dashboard_item
            (SELECT array_agg(ROW(id, titles[1])::asian_media.dashboard_item)
             FROM (
                 SELECT id, titles
                 FROM asian_media.titles
                 ORDER BY created_at DESC
                 LIMIT 5
             ) t) AS recently_added
            """.trimIndent()

            when (val result = dataFetcher.query().from(query).toSingleOf<DashboardData>()) {
                is DataResult.Success -> {
                    val data = result.value
                    if (data != null) {
                        _state.update {
                            it.copy(
                                totalTitles = data.totalTitles,
                                readingCount = data.readingCount,
                                completedCount = data.completedCount,
                                currentlyReading = data.currentlyReading.orEmpty(),
                                recentlyAdded = data.recentlyAdded.orEmpty(),
                                isLoading = false
                            )
                        }
                    } else {
                        _state.update { it.copy(isLoading = false) } // No data
                    }
                }
                is DataResult.Failure -> {
                    showError(result.error)
                    _state.update { it.copy(isLoading = false) }
                }
            }
        }
    }

    private fun showError(error: DatabaseException) {
        GlobalDialogManager.show(ErrorDialogConfig(error))
    }
}