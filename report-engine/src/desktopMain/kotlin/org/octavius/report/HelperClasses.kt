package org.octavius.report

import androidx.compose.ui.unit.Dp
import org.octavius.data.exception.DatabaseException
import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.Tr


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
            Equals -> Tr.Filter.Number.equals()
            NotEquals -> Tr.Filter.Number.notEquals()
            LessThan -> Tr.Filter.Number.lessThan()
            LessEquals -> Tr.Filter.Number.lessEqual()
            GreaterThan -> Tr.Filter.Number.greaterThan()
            GreaterEquals -> Tr.Filter.Number.greaterEqual()
            Range -> Tr.Filter.Number.range()
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
            Exact -> Tr.Filter.String.exact()
            StartsWith -> Tr.Filter.String.startsWith()
            EndsWith -> Tr.Filter.String.endsWith()
            Contains -> Tr.Filter.String.contains()
            NotContains -> Tr.Filter.String.notContains()
            NotExact -> Tr.Filter.String.notExact()
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
            Equals -> Tr.Filter.Datetime.equals()
            NotEquals -> Tr.Filter.Datetime.notEquals()
            Before -> Tr.Filter.Datetime.before()
            BeforeEquals -> Tr.Filter.Datetime.beforeEqual()
            After -> Tr.Filter.Datetime.after()
            AfterEquals -> Tr.Filter.Datetime.afterEqual()
            Range -> Tr.Filter.Datetime.range()
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
            Equals -> Tr.Filter.Interval.equals()
            NotEquals -> Tr.Filter.Interval.notEquals()
            LessThan -> Tr.Filter.Interval.lessThan()
            LessEquals -> Tr.Filter.Interval.lessEqual()
            GreaterThan -> Tr.Filter.Interval.greaterThan()
            GreaterEquals -> Tr.Filter.Interval.greaterEqual()
            Range -> Tr.Filter.Interval.range()
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
            ListAny -> Tr.Filter.List.any()
            ListAll -> Tr.Filter.List.all()
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