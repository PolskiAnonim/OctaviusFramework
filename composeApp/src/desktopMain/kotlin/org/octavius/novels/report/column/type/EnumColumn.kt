package org.octavius.novels.report.column.type

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.ColumnState
import org.octavius.novels.report.FilterValue
import org.octavius.novels.report.FilterValue.EnumFilter
import org.octavius.novels.report.NullHandling
import org.octavius.novels.report.SortDirection
import org.octavius.novels.report.column.ReportColumn
import org.octavius.novels.util.Converters.camelToSnakeCase
import kotlin.reflect.KClass

class EnumColumn<E : Enum<*>>(
    name: String,
    header: String,
    width: Float = 1f,
    sortable: Boolean = false,
    filterable: Boolean = true,
    private val enumClass: KClass<out E>,
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
) : ReportColumn(name, header, width, filterable, sortable) {

    override fun initializeState(): ColumnState {
        return ColumnState(
            mutableStateOf(SortDirection.UNSPECIFIED),
            filtering = if (filterable) mutableStateOf(EnumFilter<E>()) else mutableStateOf(null)
        )
    }

    override fun constructWhereClause(filter: FilterValue<*>): String {
        val enumFilter = filter as EnumFilter<*>

        // Jeśli lista wartości jest pusta, nie ma sensu budować klauzuli
        if (enumFilter.values.value.isEmpty() && enumFilter.nullHandling.value == NullHandling.Ignore) {
            return ""
        }

        // Przygotowanie listy wartości enum jako stringi do zapytania SQL
        val enumValues = enumFilter.values.value.joinToString(", ") {
            "'${camelToSnakeCase(it.name).uppercase()}'"
        }

        return when {
            // Gdy lista jest niepusta i chcemy tylko te wartości (lub ich nie chcemy)
            enumFilter.values.value.isNotEmpty() -> {
                val operator = if (enumFilter.include.value) "IN" else "NOT IN"
                val valuesClause = "$name $operator ($enumValues)"

                when (enumFilter.nullHandling.value) {
                    NullHandling.Ignore -> valuesClause
                    NullHandling.Include -> "($valuesClause OR $name IS NULL)"
                    NullHandling.Exclude -> "($valuesClause AND $name IS NOT NULL)"
                }
            }

            // Gdy lista jest pusta, ale chcemy obsłużyć nulle
            else -> {
                when (enumFilter.nullHandling.value) {
                    NullHandling.Include -> "$name IS NULL"
                    NullHandling.Exclude -> "$name IS NOT NULL"
                    // Ten przypadek nie powinien wystąpić (pusta lista i ignorowanie null)
                    else -> ""
                }
            }
        }
    }

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
        currentFilter: FilterValue<*>
    ) {
        if (!filterable) return

        val enumValues = remember { enumClass.java.enumConstants.toList() }

        val enumFilter = currentFilter as EnumFilter<E>
        val selectedValues = enumFilter.values
        val include = enumFilter.include
        val nullHandling = enumFilter.nullHandling

        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Wybierz wartości",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Obsługa wartości null
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Wartości puste:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(end = 8.dp)
                )

                RadioButton(
                    selected = nullHandling.value == NullHandling.Ignore,
                    onClick = { nullHandling.value = NullHandling.Ignore }
                )
                Text("Ignoruj", modifier = Modifier.padding(end = 12.dp))

                RadioButton(
                    selected = nullHandling.value == NullHandling.Include,
                    onClick = { nullHandling.value = NullHandling.Include }
                )
                Text("Dołącz", modifier = Modifier.padding(end = 12.dp))

                RadioButton(
                    selected = nullHandling.value == NullHandling.Exclude,
                    onClick = { nullHandling.value = NullHandling.Exclude }
                )
                Text("Wyklucz")
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Przełącznik dla trybu włączania/wyłączania
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tryb wyboru:")
                RadioButton(
                    selected = include.value,
                    onClick = { include.value = true }
                )
                Text("Uwzględnij zaznaczone", modifier = Modifier.padding(end = 8.dp))

                RadioButton(
                    selected = !include.value,
                    onClick = { include.value = false }
                )
                Text("Wyklucz zaznaczone")
            }

            Spacer(modifier = Modifier.height(8.dp))

            enumValues.forEach { enumValue ->
                @Suppress("UNCHECKED_CAST")
                val isSelected = selectedValues.value.contains(enumValue)

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
                                currentFilter.addValue(enumValue)
                            } else {
                                currentFilter.removeValue(enumValue)
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