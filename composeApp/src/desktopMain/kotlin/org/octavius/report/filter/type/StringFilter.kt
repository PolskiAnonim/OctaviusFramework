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
import org.octavius.localization.Translations
import org.octavius.report.FilterMode
import org.octavius.report.Query
import org.octavius.report.StringFilterDataType
import org.octavius.report.filter.EnumDropdownMenu
import org.octavius.report.filter.Filter
import org.octavius.report.filter.FilterSpacer
import org.octavius.report.filter.data.type.StringFilterData

class StringFilter: Filter<StringFilterData>() {

    override fun createDefaultData(): StringFilterData {
        return StringFilterData()
    }

    @Composable
    override fun RenderFilterUI(data: StringFilterData) {
        OutlinedTextField(
            value = data.value.value,
            onValueChange = { data.value.value = it },
            label = { Text(Translations.get("filter.string.value")) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            trailingIcon = {
                if (data.value.value.isNotEmpty()) {
                    IconButton(onClick = { data.value.value = "" }) {
                        Icon(Icons.Default.Clear, Translations.get("filter.general.clear"))
                    }
                }
            }
        )

        FilterSpacer()

        EnumDropdownMenu(
            currentValue = data.filterType.value,
            options = StringFilterDataType.entries,
            onValueChange = { data.filterType.value = it }
        )


        FilterSpacer()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Checkbox(
                checked = data.caseSensitive.value,
                onCheckedChange = { data.caseSensitive.value = it }
            )
            Text(
                text = Translations.get("filter.string.caseSensitive"),
                modifier = Modifier.padding(start = 8.dp)
            )
        }
    }

    override fun buildBaseQueryFragment(
        columnName: String,
        data: StringFilterData
    ): Query? {
        val value = data.value.value.trim()
        val filterType = data.filterType.value
        val caseSensitive = data.caseSensitive.value
        return when (data.mode.value) {
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
                val operator = if (isAllMode) "@>" else "&&"
                Query("$columnName $operator :$columnName", mapOf(columnName to listOf(valueParam)))
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