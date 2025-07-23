package org.octavius.report

import androidx.compose.ui.unit.Dp
import org.octavius.domain.EnumWithFormatter
import org.octavius.localization.Translations


data class Query(
    val sql: String,
    val params: Map<String, Any> = emptyMap()
)

data class ReportPaginationState(
    val currentPage: Long = 0,
    val totalPages: Long = 1,
    val totalItems: Long = 0,
    val pageSize: Int = 10
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
            Equals -> Translations.get("filter.number.equals")
            NotEquals -> Translations.get("filter.number.notEquals")
            LessThan -> Translations.get("filter.number.lessThan")
            LessEquals -> Translations.get("filter.number.lessEqual")
            GreaterThan -> Translations.get("filter.number.greaterThan")
            GreaterEquals -> Translations.get("filter.number.greaterEqual")
            Range -> Translations.get("filter.number.range")
        }
    }
}

enum class StringFilterDataType: EnumWithFormatter<StringFilterDataType> {
    Exact,       // dokładne dopasowanie
    StartsWith,  // od początku
    EndsWith,    // od końca
    Contains,    // dowolny fragment
    NotContains;  // nie zawiera

    override fun toDisplayString(): String {
        return when(this) {
            Exact -> Translations.get("filter.string.exact")
            StartsWith -> Translations.get("filter.string.startsWith")
            EndsWith -> Translations.get("filter.string.endsWith")
            Contains -> Translations.get("filter.string.contains")
            NotContains -> Translations.get("filter.string.notContains")
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
            ListAny -> Translations.get("filter.list.any")
            ListAll -> Translations.get("filter.list.all")
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