package org.octavius.domain

enum class SortDirection {
    Ascending, // Rosnąca
    Descending // Malejąca
}

data class SortConfiguration(
    val columnName: String,
    val sortDirection: SortDirection
)

enum class NumberFilterDataType {
    Equals,      // ==
    NotEquals,   // !=
    LessThan,    //
    LessEquals,  // <=
    GreaterThan, // >
    GreaterEquals, // >=
    Range        // between min and max
}

enum class StringFilterDataType {
    Exact,       // dokładne dopasowanie
    StartsWith,  // od początku
    EndsWith,    // od końca
    Contains,    // dowolny fragment
    NotContains  // nie zawiera
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