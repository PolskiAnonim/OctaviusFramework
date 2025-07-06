package org.octavius.domain

import kotlinx.serialization.json.JsonObject
import org.octavius.localization.Translations

enum class SortDirection {
    Ascending, // Rosnąca
    Descending // Malejąca
}

data class SortConfiguration(
    val columnName: String,
    val sortDirection: SortDirection
)

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
            Equals -> Translations.get("filter.string.equals")
            NotEquals -> Translations.get("filter.string.notEquals")
            LessThan -> Translations.get("filter.string.lessThan")
            LessEquals -> Translations.get("filter.string.lessEqual")
            GreaterThan -> Translations.get("filter.string.greaterThan")
            GreaterEquals -> Translations.get("filter.string.greaterEqual")
            Range -> Translations.get("filter.string.range")
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

enum class FilterDataType {
    Boolean,
    String,
    Number,
    Enum
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

data class FilterConfig(
    val columnName: String,
    val config: JsonObject
)