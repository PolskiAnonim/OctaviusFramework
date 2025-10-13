package org.octavius.report

import androidx.compose.ui.unit.Dp
import org.octavius.data.exception.DatabaseException
import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.T


data class Query(
    val sql: String,
    val params: Map<String, Any> = emptyMap()
)

data class ReportPaginationState(
    val currentPage: Long = 0,
    val totalPages: Long = 1,
    val totalItems: Long = 0,
    val pageSize: Long = 10
) {
    fun resetPage(): ReportPaginationState = this.copy(currentPage = 0)
}

enum class NumberFilterDataType: EnumWithFormatter<NumberFilterDataType> {
    Equals,      // ==
    NotEquals,   // !=
    LessThan,    //
    LessEquals,  // <=
    GreaterThan, // >
    GreaterEquals, // >=
    Range;        // between min and max

    override fun toDisplayString(): String {
        return when(this) {
            Equals -> T.get("filter.number.equals")
            NotEquals -> T.get("filter.number.notEquals")
            LessThan -> T.get("filter.number.lessThan")
            LessEquals -> T.get("filter.number.lessEqual")
            GreaterThan -> T.get("filter.number.greaterThan")
            GreaterEquals -> T.get("filter.number.greaterEqual")
            Range -> T.get("filter.number.range")
        }
    }
}

enum class StringFilterDataType: EnumWithFormatter<StringFilterDataType> {
    Exact,       // dokładne dopasowanie
    NotExact,    // brak dokładnego dopasowania
    StartsWith,  // od początku
    EndsWith,    // od końca
    Contains,    // dowolny fragment
    NotContains;  // nie zawiera

    override fun toDisplayString(): String {
        return when(this) {
            Exact -> T.get("filter.string.exact")
            StartsWith -> T.get("filter.string.startsWith")
            EndsWith -> T.get("filter.string.endsWith")
            Contains -> T.get("filter.string.contains")
            NotContains -> T.get("filter.string.notContains")
            NotExact -> T.get("filter.string.notExact")
        }
    }
}

enum class DateTimeFilterDataType : EnumWithFormatter<DateTimeFilterDataType> {
    Equals,      // ==
    NotEquals,   // !=
    Before,      // <
    BeforeEquals, // <=
    After,       // >
    AfterEquals, // >=
    Range;        // between min and max

    override fun toDisplayString(): String {
        return when (this) {
            Equals -> T.get("filter.datetime.equals")
            NotEquals -> T.get("filter.datetime.notEquals")
            Before -> T.get("filter.datetime.before")
            BeforeEquals -> T.get("filter.datetime.beforeEqual")
            After -> T.get("filter.datetime.after")
            AfterEquals -> T.get("filter.datetime.afterEqual")
            Range -> T.get("filter.datetime.range")
        }
    }
}

enum class IntervalFilterDataType : EnumWithFormatter<IntervalFilterDataType> {
    Equals,      // ==
    NotEquals,   // !=
    LessThan,    // <
    LessEquals,  // <=
    GreaterThan, // >
    GreaterEquals, // >=
    Range;        // between min and max

    override fun toDisplayString(): String {
        return when (this) {
            Equals -> T.get("filter.interval.equals")
            NotEquals -> T.get("filter.interval.notEquals")
            LessThan -> T.get("filter.interval.lessThan")
            LessEquals -> T.get("filter.interval.lessEqual")
            GreaterThan -> T.get("filter.interval.greaterThan")
            GreaterEquals -> T.get("filter.interval.greaterEqual")
            Range -> T.get("filter.interval.range")
        }
    }
}

enum class NullHandling {
    Ignore,      // Ignoruj wartości null
    Include,     // Dołącz wartości null - dla pustej wartości - tylko nulle
    Exclude,     // Wyklucz wartości null
}

enum class FilterMode: EnumWithFormatter<FilterMode> {
    Single,
    ListAny,
    ListAll;

    override fun toDisplayString(): String {
        return when (this) {
            Single -> "" // Ta wartość jest niemożliwa do zmiany i jest niewidoczna
            ListAny -> T.get("filter.list.any")
            ListAll -> T.get("filter.list.all")
        }
    }
}

/**
 * Reprezentuje szerokość kolumny w raporcie.
 * Może być stała (w Dp) lub elastyczna (jako waga w RowScope).
 */
sealed class ColumnWidth {
    /**
     * Stała szerokość kolumny, zdefiniowana w Dp.
     * Używana dla kolumn z UI, jak przyciski akcji.
     */
    data class Fixed(val width: Dp) : ColumnWidth()

    /**
     * Elastyczna szerokość kolumny, zdefiniowana jako waga.
     * Kolumna zajmie przestrzeń proporcjonalną do swojej wagi.
     * Używana dla kolumn z danymi.
     */
    data class Flexible(val weight: Float) : ColumnWidth()
}

sealed class ReportDataResult {
    /** Sukces - zawiera dane i stan paginacji */
    data class Success(
        val data: List<Map<String, Any?>>,
        val paginationState: ReportPaginationState
    ) : ReportDataResult()

    /** Porażka - zawiera błąd */
    data class Failure(val error: DatabaseException) : ReportDataResult()
}