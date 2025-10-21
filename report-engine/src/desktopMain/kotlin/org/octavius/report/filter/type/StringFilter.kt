package org.octavius.report.filter.type

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.serialization.json.JsonObject
import org.octavius.localization.T
import org.octavius.report.FilterMode
import org.octavius.report.Query
import org.octavius.report.ReportEvent
import org.octavius.report.StringFilterDataType
import org.octavius.report.filter.EnumDropdownMenu
import org.octavius.report.filter.Filter
import org.octavius.report.filter.FilterSpacer
import org.octavius.report.filter.data.type.StringFilterData

class StringFilter: Filter<StringFilterData>() {

    override fun createDefaultData(): StringFilterData = StringFilterData()

    override fun deserializeData(data: JsonObject): StringFilterData = StringFilterData.deserialize(data)

    @Composable
    override fun RenderFilterUI(onEvent: (ReportEvent) -> Unit, columnKey: String, data: StringFilterData) {
        OutlinedTextField(
            value = data.value,
            onValueChange = { onEvent.invoke(ReportEvent.FilterChanged(columnKey, data.copy(value = it))) },
            label = { Text(T.get("filter.string.value")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (data.value.isNotEmpty()) {
                    IconButton(onClick =  { onEvent.invoke(ReportEvent.FilterChanged(columnKey, data.copy(value = ""))) }) {
                        Icon(Icons.Default.Clear, T.get("filter.general.clear"))
                    }
                }
            }
        )

        FilterSpacer()

        EnumDropdownMenu(
            currentValue = data.filterType,
            options = StringFilterDataType.entries,
            onValueChange =  { onEvent.invoke(ReportEvent.FilterChanged(columnKey, data.copy(filterType = it))) },
        )


        FilterSpacer()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = data.caseSensitive,
                onCheckedChange =  { onEvent.invoke(ReportEvent.FilterChanged(columnKey, data.copy(caseSensitive = it))) },
            )
            Text(
                text = T.get("filter.string.caseSensitive"),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }



    override fun buildBaseQueryFragment(
        columnName: String,
        data: StringFilterData
    ): Query? {
        val value = data.value.trim()
        if (value.isEmpty()) return null
        val filterType = data.filterType
        val caseSensitive = data.caseSensitive
        return when (data.mode) {
            FilterMode.Single -> buildSingleStringQuery(columnName, value, filterType, caseSensitive)
            FilterMode.ListAny -> buildListStringQuery(columnName, value, filterType, caseSensitive, false)
            FilterMode.ListAll -> buildListStringQuery(columnName, value, filterType, caseSensitive, true)
        }
    }

    // Funkcja pomocnicza do "ucieczki" znaków specjalnych dla LIKE
    fun escapeSqlLike(value: String): String {
        return value.replace("%", "\\%").replace("_", "\\_")
    }

    private fun buildSingleStringQuery(
        columnName: String,
        searchValue: String,
        filterType: StringFilterDataType,
        caseSensitive: Boolean
    ): Query {
        // Obsługa przypadków równościowych (=, <>)
        if (filterType == StringFilterDataType.Exact || filterType == StringFilterDataType.NotExact) {
            val operator = if (filterType == StringFilterDataType.Exact) "=" else "<>"

            return if (caseSensitive) {
                // Czułe na wielkość liter: WHERE columnName = 'Wartość'
                val querySql = "$columnName $operator :$columnName"
                Query(querySql, mapOf(columnName to searchValue))
            } else {
                // Niezależne od wielkości liter: WHERE LOWER(columnName) = 'wartość'
                val querySql = "LOWER($columnName) $operator :$columnName"
                // Przekazujemy już zmienioną na małe litery wartość
                Query(querySql, mapOf(columnName to searchValue.lowercase()))
            }
        }

        // Dla wszystkich wariantów LIKE
        val operator = if (caseSensitive) "LIKE" else "ILIKE"
        val notOperator = if (caseSensitive) "NOT LIKE" else "NOT ILIKE"

        val escapedSearchValue = escapeSqlLike(searchValue)

        val pattern = when (filterType) {
            StringFilterDataType.StartsWith -> "$escapedSearchValue%"
            StringFilterDataType.EndsWith -> "%$escapedSearchValue"
            StringFilterDataType.Contains, StringFilterDataType.NotContains -> "%$escapedSearchValue%"
            else -> throw IllegalStateException("Unexpected filterType: $filterType") // Zabezpieczenie
        }

        val finalOperator = if (filterType == StringFilterDataType.NotContains) notOperator else operator

        return Query("$columnName $finalOperator :$columnName", mapOf(columnName to pattern))
    }

    private fun buildListStringQuery(
        columnName: String,
        searchValue: String,
        filterType: StringFilterDataType,
        caseSensitive: Boolean,
        isAllMode: Boolean // true = ALL, false = ANY
    ): Query {
        // 1. Zdefiniujmy operator LIKE/ILIKE na podstawie czułości
        val likeOperator = if (caseSensitive) "LIKE" else "ILIKE"
        val notLikeOperator = if (caseSensitive) "NOT LIKE" else "NOT ILIKE"

        val escapedSearchValue = escapeSqlLike(searchValue)

        // 3. Obsługa przypadków równościowych (Exact, NotExact)
        if (filterType == StringFilterDataType.Exact || filterType == StringFilterDataType.NotExact) {
            val operator = if (filterType == StringFilterDataType.Exact) "=" else "<>"

            // Zawsze używamy UNNEST dla spójności logiki `isAllMode`
            val innerCondition = if (caseSensitive) "elem $operator :$columnName" else "LOWER(elem) $operator :$columnName"
            val valueParam = if (caseSensitive) searchValue else searchValue.lowercase()

            val querySql = if (isAllMode) {
                // ALL: Sprawdź, czy NIE ISTNIEJE element, który NIE spełnia warunku.
                // Dla Exact: "czy nie istnieje element, który jest różny od szukanego" -> "wszystkie są równe"
                // Dla NotExact: "czy nie istnieje element, który jest równy szukanemu" -> "wszystkie są różne"
                "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE NOT ($innerCondition))"
            } else {
                // ANY: Sprawdź, czy ISTNIEJE element, który spełnia warunek.
                "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $innerCondition)"
            }
            return Query(querySql, mapOf(columnName to valueParam))
        }

        // 4. Obsługa przypadków z wzorcami (Contains, StartsWith, EndsWith, etc.)
        val (pattern, currentLikeOperator) = when (filterType) {
            StringFilterDataType.Contains    -> Pair("%$escapedSearchValue%", likeOperator)
            StringFilterDataType.StartsWith  -> Pair("$escapedSearchValue%", likeOperator)
            StringFilterDataType.EndsWith    -> Pair("%$escapedSearchValue", likeOperator)
            StringFilterDataType.NotContains -> Pair("%$escapedSearchValue%", notLikeOperator)
            else -> throw IllegalStateException("Nieobsługiwany typ filtra: $filterType")
        }

        val innerCondition = "elem $currentLikeOperator :$columnName"

        val querySql = if (isAllMode) {
            // ALL: Czy NIE ISTNIEJE element, który NIE spełnia warunku?
            // np. dla Contains: "wszystkie elementy zawierają wartość"
            // np. dla NotContains: "wszystkie elementy nie zawierają wartości" (czyli żaden nie zawiera)
            "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE NOT ($innerCondition))"
        } else {
            // ANY: Czy ISTNIEJE element, który spełnia warunek?
            // np. dla Contains: "co najmniej jeden element zawiera wartość"
            // np. dla NotContains: "co najmniej jeden element nie zawiera wartości"
            "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $innerCondition)"
        }

        return Query(querySql, mapOf(columnName to pattern))
    }


}