package org.octavius.novels.report.column.type

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.FilterValue
import org.octavius.novels.report.FilterValue.EnumFilter
import org.octavius.novels.report.NullHandling
import org.octavius.novels.report.column.ReportColumn
import kotlin.reflect.KClass

class EnumColumn(
    name: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    private val enumClass: KClass<out Enum<*>>? = null,
    private val formatter: (Enum<*>?) -> String = {
        it?.let {
            try {
                val method = it::class.members.find { member -> member.name == "toDisplayString" }
                if (method != null) {
                    method.call(it) as String
                } else {
                    it.toString()
                }
            } catch (e: Exception) {
                it.toString()
            }
        } ?: ""
    }
) : ReportColumn(name, header, width, sortable, filterable) {

    @Composable
    override fun RenderCell(item: Map<String, Any?>, modifier: Modifier) {
        val value = item[name] as? Enum<*>

        Box(
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp, horizontal = 8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = formatter(value),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }

    @Composable
    override fun RenderFilter(
        currentFilter: FilterValue<*>?,
        onFilterChanged: (FilterValue<*>?) -> Unit
    ) {
        if (!filtrable || enumClass == null) return

        val enumValues = remember { enumClass.java.enumConstants.toList() }
        val selectedValues = remember { mutableStateListOf<Enum<*>>() }

        @Suppress("UNCHECKED_CAST")
        val enumFilter = currentFilter as? FilterValue.EnumFilter<*>

        // Inicjalizacja wartości z aktualnego filtra
        LaunchedEffect(currentFilter) {
            selectedValues.clear()
            enumFilter?.values?.forEach { value ->
                selectedValues.add(value)
            }
        }

        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Wybierz wartości",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            enumValues.forEach { enumValue ->
                val isSelected = selectedValues.contains(enumValue)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = { checked ->
                            if (checked) {
                                selectedValues.add(enumValue)
                            } else {
                                selectedValues.remove(enumValue)
                            }

                            if (selectedValues.isEmpty()) {
                                onFilterChanged(null)
                            } else {
                                // Poprawione tworzenie filtra
                                val filter = EnumFilter(
                                    values = selectedValues.toList(),
                                    nullHandling = NullHandling.Exclude
                                )
                                onFilterChanged(filter)
                            }
                        }
                    )

                    Text(
                        text = formatter(enumValue),
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }
    }
}