package org.octavius.report.filter.type

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.report.FilterMode
import org.octavius.report.Query
import org.octavius.report.filter.Filter
import org.octavius.report.filter.data.type.BooleanFilterData

class BooleanFilter(
    private val trueText: String,
    private val falseText: String
) : Filter<BooleanFilterData>() {
    override fun createDefaultData(): BooleanFilterData {
        return BooleanFilterData()
    }

    @Composable
    override fun RenderFilterUI(data: BooleanFilterData) {
        Text(
            text = "Filtruj według wartości",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = data.value.value == true,
                onClick = {
                    data.value.value = if (data.value.value == true) null else true
                },
                label = { Text(trueText) },
                leadingIcon = if (data.value.value == true) {
                    {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null
            )

            FilterChip(
                selected = data.value.value == false,
                onClick = {
                    data.value.value = if (data.value.value == false) null else false
                },
                label = { Text(falseText) },
                leadingIcon = if (data.value.value == false) {
                    {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = null,
                            modifier = Modifier.size(FilterChipDefaults.IconSize)
                        )
                    }
                } else null
            )
        }
    }

    override fun buildBaseQueryFragment(
        columnName: String,
        data: BooleanFilterData
    ): Query? {
        val booleanValue = data.value.value!!

        return when (data.mode.value) {
            FilterMode.Single -> {
                Query("$columnName = :$columnName", mapOf(columnName to booleanValue))
            }

            FilterMode.ListAny -> {
                Query("$columnName && :$columnName", mapOf(columnName to listOf(booleanValue)))
            }

            FilterMode.ListAll -> {
                Query("$columnName @> :$columnName", mapOf(columnName to listOf(booleanValue)))
            }
        }
    }
}