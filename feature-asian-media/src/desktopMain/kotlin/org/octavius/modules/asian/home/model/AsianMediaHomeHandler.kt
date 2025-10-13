package org.octavius.modules.asian.home.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.DataAccess
import org.octavius.data.DataResult
import org.octavius.data.builder.toSingleOf
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.data.exception.DatabaseException

class AsianMediaHomeHandler : KoinComponent {
    private val dataAccess: DataAccess by inject()
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _state = MutableStateFlow(AsianMediaHomeState())
    val state = _state.asStateFlow()

    init {
        loadData()
    }

    fun getSql(): String {
        val totalTitlesSubquery = dataAccess.select("COUNT(*)").from("asian_media.titles").toSql()

        val readingCountSubquery = dataAccess.select("COUNT(DISTINCT title_id)")
                .from("asian_media.publications")
                .where("status = 'READING'")
                .toSql()

        val notExistsForCompleted = dataAccess.select("1")
            .from("asian_media.publications p2")
            .where("p2.title_id = p1.title_id AND p2.status = 'READING'")
            .toSql()
        val completedCountSubquery =
            dataAccess.select("COUNT(DISTINCT p1.title_id)")
                .from("asian_media.publications p1")
                .where("p1.status = 'COMPLETED' AND NOT EXISTS ($notExistsForCompleted)")
                .toSql()

        val innerCurrentlyReading = dataAccess.select("t.id, t.titles")
            .from("asian_media.titles t")
            .where("EXISTS (SELECT 1 FROM asian_media.publications p WHERE p.title_id = t.id AND p.status = 'READING')")
            .orderBy("t.updated_at DESC")
            .limit(5)
            .toSql()
        val currentlyReadingSubquery = dataAccess.select("array_agg(ROW(id, titles[1])::asian_media.dashboard_item)")
                .fromSubquery(innerCurrentlyReading)
                .toSql()

        val innerRecentlyAdded = dataAccess.select("id, titles")
            .from("asian_media.titles")
            .orderBy("created_at DESC")
            .limit(5)
            .toSql()
        val recentlyAddedSubquery = dataAccess.select("array_agg(ROW(id, titles[1])::asian_media.dashboard_item)")
                .fromSubquery(innerRecentlyAdded)
                .toSql()

        // === Składamy finalną klauzulę SELECT z gotowych klocków ===

        return """
        ($totalTitlesSubquery) AS total_titles,
        ($readingCountSubquery) AS reading_count,
        ($completedCountSubquery) AS completed_count,
        ($currentlyReadingSubquery) AS currently_reading,
        ($recentlyAddedSubquery) AS recently_added
    """
    }

    fun loadData() {

        scope.launch {
            val finalSelectClause = getSql()

            when (val result = dataAccess.select(finalSelectClause).toSingleOf<DashboardData>()) {
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