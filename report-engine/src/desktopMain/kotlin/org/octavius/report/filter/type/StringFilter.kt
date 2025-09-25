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
import org.octavius.data.withPgType
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
        val filterType = data.filterType
        val caseSensitive = data.caseSensitive
        return when (data.mode) {
            FilterMode.Single -> buildSingleStringQuery(columnName, value, filterType, caseSensitive)
            FilterMode.ListAny -> buildListStringQuery(columnName, value, filterType, caseSensitive, false)
            FilterMode.ListAll -> buildListStringQuery(columnName, value, filterType, caseSensitive, true)
        }
    }

    private fun buildSingleStringQuery(
        columnName: String,
        searchValue: String,
        filterType: StringFilterDataType,
        caseSensitive: Boolean
    ): Query {
        val columnRef = if (caseSensitive) columnName else "LOWER($columnName)"
        val valueRef = if (caseSensitive) searchValue else searchValue.lowercase()

        return when (filterType) {
            StringFilterDataType.Exact -> {
                Query("$columnRef = :$columnName", mapOf(columnName to valueRef))
            }
            StringFilterDataType.StartsWith -> {
                Query("$columnRef LIKE :$columnName", mapOf(columnName to "$valueRef%"))
            }
            StringFilterDataType.EndsWith -> {
                Query("$columnRef LIKE :$columnName", mapOf(columnName to "%$valueRef"))
            }
            StringFilterDataType.Contains -> {
                Query("$columnRef LIKE :$columnName", mapOf(columnName to "%$valueRef%"))
            }
            StringFilterDataType.NotContains -> {
                Query("$columnRef NOT LIKE :$columnName", mapOf(columnName to "%$valueRef%"))
            }
        }
    }

    private fun buildListStringQuery(
        columnName: String,
        searchValue: String,
        filterType: StringFilterDataType,
        caseSensitive: Boolean,
        isAllMode: Boolean
    ): Query {
        val valueParam = if (caseSensitive) searchValue else searchValue.lowercase()

        return when (filterType) {
            StringFilterDataType.Exact -> {
                if (caseSensitive) {
                    val operator = if (isAllMode) "@>" else "&&"
                    Query(
                        "$columnName $operator :$columnName",
                        mapOf(columnName to listOf(valueParam).withPgType("text[]"))
                    )
                } else {
                    // Jeśli wielkość liter NIE ma znaczenia, musimy użyć UNNEST
                    val paramMap = mapOf(columnName to valueParam)
                    if (isAllMode) {
                        Query("NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE LOWER(elem) <> :$columnName)", paramMap)
                    } else {
                        Query("EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE LOWER(elem) = :$columnName)", paramMap)
                    }
                }
            }
            StringFilterDataType.StartsWith -> {
                val condition = if (caseSensitive) "elem" else "LOWER(elem)"
                val existsType = if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $condition NOT LIKE :$columnName)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $condition LIKE :$columnName)"
                Query(existsType, mapOf(columnName to "$valueParam%"))
            }
            StringFilterDataType.EndsWith -> {
                val condition = if (caseSensitive) "elem" else "LOWER(elem)"
                val existsType = if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $condition NOT LIKE :$columnName)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $condition LIKE :$columnName)"
                Query(existsType, mapOf(columnName to "%$valueParam"))
            }
            StringFilterDataType.Contains -> {
                val condition = if (caseSensitive) "elem" else "LOWER(elem)"
                val existsType = if (isAllMode) "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $condition NOT LIKE :$columnName)" else "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $condition LIKE :$columnName)"
                Query(existsType, mapOf(columnName to "%$valueParam%"))
            }
            StringFilterDataType.NotContains -> {
                val condition = if (caseSensitive) "elem" else "LOWER(elem)"
                val existsType = if (isAllMode) "EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $condition LIKE :$columnName)" else "NOT EXISTS (SELECT 1 FROM unnest($columnName) AS elem WHERE $condition LIKE :$columnName)"
                Query(existsType, mapOf(columnName to "%$valueParam%"))
            }
        }
    }


}