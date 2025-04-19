package org.octavius.novels.report

import androidx.compose.runtime.snapshots.SnapshotStateMap
import org.octavius.novels.util.Converters.camelToSnakeCase

sealed class FilterValue<T> {
    abstract val nullHandling: NullHandling

    // Filtr dla wartości liczbowych (Int, Double itp.)
    data class NumberFilter<T : Comparable<T>>(
        val filterType: NumberFilterType,
        val minValue: T? = null,
        val maxValue: T? = null,
        override val nullHandling: NullHandling = NullHandling.Ignore
    ) : FilterValue<T>()

    // Filtr dla wartości tekstowych
    data class TextFilter(
        val filterType: TextFilterType,
        val value: String,
        val caseSensitive: Boolean = false,
        override val nullHandling: NullHandling = NullHandling.Ignore
    ) : FilterValue<String>()

    // Filtr dla wartości enumów
    data class EnumFilter<E : Enum<E>>(
        val values: List<E>,
        val include: Boolean = true, // true = include these values, false = exclude
        override val nullHandling: NullHandling = NullHandling.Ignore
    ) : FilterValue<E>()

    // Filtr dla wartości boolean
    data class BooleanFilter(
        val value: Boolean?,
        override val nullHandling: NullHandling = NullHandling.Ignore
    ) : FilterValue<Boolean>()
}

enum class NumberFilterType {
    Equals,      // ==
    NotEquals,   // !=
    LessThan,    //
    LessEquals,  // <=
    GreaterThan, // >
    GreaterEquals, // >=
    Range        // between min and max
}

enum class TextFilterType {
    Exact,       // dokładne dopasowanie
    StartsWith,  // od początku
    EndsWith,    // od końca
    Contains,    // dowolny fragment
    NotContains  // nie zawiera
}

enum class NullHandling {
    Ignore,      // Ignoruj wartości null
    Include,     // Dołącz wartości null
    Exclude      // Wyklucz wartości null
}

object FilterQueryBuilder {
    fun buildWhereClause(filters: SnapshotStateMap<String, FilterValue<*>>): String {
        if (filters.isEmpty()) return ""

        val conditions = filters.mapNotNull { buildCondition(it.key, it.value) }
        if (conditions.isEmpty()) return ""

        return "WHERE " + conditions.joinToString(" AND ")
    }

    private fun buildCondition(columnName: String, filter: FilterValue<*>): String? {
        return when (filter) {
            is FilterValue.NumberFilter<*> -> buildNumberCondition(columnName, filter)
            is FilterValue.TextFilter -> buildTextCondition(columnName, filter)
            is FilterValue.EnumFilter<*> -> buildEnumCondition(columnName, filter)
            is FilterValue.BooleanFilter -> buildBooleanCondition(columnName, filter)
        }
    }

    private fun buildNumberCondition(column: String, filter: FilterValue.NumberFilter<*>): String? {
        val nullCheck = getNullCheck(column, filter.nullHandling)

        val condition = when (filter.filterType) {
            NumberFilterType.Equals ->
                filter.minValue?.let { "$column = $it" }
            NumberFilterType.NotEquals ->
                filter.minValue?.let { "$column <> $it" }
            NumberFilterType.LessThan ->
                filter.minValue?.let { "$column < $it" }
            NumberFilterType.LessEquals ->
                filter.minValue?.let { "$column <= $it" }
            NumberFilterType.GreaterThan ->
                filter.minValue?.let { "$column > $it" }
            NumberFilterType.GreaterEquals ->
                filter.minValue?.let { "$column >= $it" }
            NumberFilterType.Range -> {
                val minCond = filter.minValue?.let { "$column >= $it" }
                val maxCond = filter.maxValue?.let { "$column <= $it" }
                when {
                    minCond != null && maxCond != null -> "($minCond AND $maxCond)"
                    minCond != null -> minCond
                    maxCond != null -> maxCond
                    else -> null
                }
            }
        }

        return condition?.let { combineWithNullCheck(it, nullCheck) }
    }

    private fun buildTextCondition(column: String, filter: FilterValue.TextFilter): String {
        val value = filter.value.replace("'", "''") // Escape apostrophes
        val nullCheck = getNullCheck(column, filter.nullHandling)

        val valueExpr = if (filter.caseSensitive) "'$value'" else "LOWER('$value')"
        val columnExpr = if (filter.caseSensitive) column else "LOWER($column)"

        val condition = when (filter.filterType) {
            TextFilterType.Exact ->
                "$columnExpr = $valueExpr"
            TextFilterType.StartsWith ->
                "$columnExpr LIKE '$value%'"
            TextFilterType.EndsWith ->
                "$columnExpr LIKE '%$value'"
            TextFilterType.Contains ->
                "$columnExpr LIKE '%$value%'"
            TextFilterType.NotContains ->
                "$columnExpr NOT LIKE '%$value%'"
        }

        return combineWithNullCheck(condition, nullCheck)
    }

    private fun buildEnumCondition(column: String, filter: FilterValue.EnumFilter<*>): String? {
        if (filter.values.isEmpty()) return null

        val nullCheck = getNullCheck(column, filter.nullHandling)

        val values = filter.values.joinToString(", ") { "'${camelToSnakeCase(it.name).uppercase()}'" }
        val operator = if (filter.include) "=" else "<>"

        val condition = "$column $operator ANY(ARRAY[$values])"
        return combineWithNullCheck(condition, nullCheck)
    }

    private fun buildBooleanCondition(column: String, filter: FilterValue.BooleanFilter): String? {
        val nullCheck = getNullCheck(column, filter.nullHandling)

        val condition = filter.value?.let { "$column = ${if (it) "TRUE" else "FALSE"}" }
        return condition?.let { combineWithNullCheck(it, nullCheck) }
    }

    private fun getNullCheck(column: String, nullHandling: NullHandling): String? {
        return when (nullHandling) {
            NullHandling.Include -> "OR $column IS NULL"
            NullHandling.Exclude -> "AND $column IS NOT NULL"
            NullHandling.Ignore -> null
        }
    }

    private fun combineWithNullCheck(condition: String, nullCheck: String?): String {
        return nullCheck?.let { "($condition $it)" } ?: condition
    }
}