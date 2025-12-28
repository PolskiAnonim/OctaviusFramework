package org.octavius.feature.books.home.model

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.DataAccess
import org.octavius.data.DataResult
import org.octavius.data.builder.toSingleOf
import org.octavius.data.exception.DatabaseException
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager

class BooksHomeHandler(
    private val scope: CoroutineScope
) : KoinComponent {
    private val dataAccess: DataAccess by inject()

    private val _state: MutableStateFlow<BooksHomeState> = MutableStateFlow(BooksHomeState.Loading)
    val state = _state.asStateFlow()

    private fun getSql(): String {
        // Liczba wszystkich książek
        val totalBooksSubquery = dataAccess.select("COUNT(*)").from("books.books").toSql()

        // Liczba wszystkich autorów
        val totalAuthorsSubquery = dataAccess.select("COUNT(*)").from("books.authors").toSql()

        // Liczba książek "W trakcie czytania"
        val readingCountSubquery = dataAccess.select("COUNT(*)")
            .from("books.books")
            .where("status = 'READING'")
            .toSql()

        // Liczba przeczytanych
        val completedCountSubquery = dataAccess.select("COUNT(*)")
            .from("books.books")
            .where("status = 'COMPLETED'")
            .toSql()

        // Lista: Aktualnie czytane (ostatnio aktualizowane)
        val innerCurrentlyReading = dataAccess.select("id, title_pl")
            .from("books.books")
            .where("status = 'READING'")
            .orderBy("updated_at DESC")
            .limit(5)
            .toSql()

        val currentlyReadingSubquery = dataAccess.select("""
            COALESCE(
                array_agg(
                    dynamic_dto('books_dashboard_item', jsonb_build_object(
                        'id', id,
                        'title', title_pl)
                    )
                ), 
                '{}'::dynamic_dto[]
            )
        """).fromSubquery(innerCurrentlyReading).toSql()

        // Lista: Ostatnio dodane
        val innerRecentlyAdded = dataAccess.select("id, title_pl")
            .from("books.books")
            .orderBy("created_at DESC")
            .limit(5)
            .toSql()

        val recentlyAddedSubquery = dataAccess.select("""
            COALESCE(
                array_agg(
                    dynamic_dto('books_dashboard_item', jsonb_build_object(
                        'id', id,
                        'title', title_pl)
                    )
                ), 
                '{}'::dynamic_dto[]
            )
        """).fromSubquery(innerRecentlyAdded).toSql()

        // Złożenie wszystkiego w jeden SELECT
        return """
            ($totalBooksSubquery) AS total_books,
            ($totalAuthorsSubquery) AS total_authors,
            ($readingCountSubquery) AS reading_count,
            ($completedCountSubquery) AS completed_count,
            ($currentlyReadingSubquery) AS currently_reading,
            ($recentlyAddedSubquery) AS recently_added
        """
    }

    fun loadData() {
        val finalSelectClause = getSql()

        dataAccess.select(finalSelectClause)
            .async(scope)
            .toSingleOf<BooksDashboardData> { result ->
                when (result) {
                    is DataResult.Success -> {
                        val data = result.value!! // Biorąc pod uwagę konstrukcję zapytania to nigdy nie będzie null
                        _state.update {
                            BooksHomeState.Success(data)
                        }
                    }
                    is DataResult.Failure -> {
                        showError(result.error)
                        _state.update { BooksHomeState.Error }
                    }
                }
            }
    }

    private fun showError(error: DatabaseException) {
        GlobalDialogManager.show(ErrorDialogConfig(error))
    }
}