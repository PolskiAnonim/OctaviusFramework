package org.octavius.novels.report.filter.type

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.report.FilterData
import org.octavius.novels.report.NullHandling
import org.octavius.novels.report.filter.Filter
import org.octavius.novels.util.Converters.camelToSnakeCase
import kotlin.reflect.KClass

class EnumFilter<E : Enum<*>>(columnName: String, val enumClass: KClass<E>) : Filter(columnName) {

    override fun constructWhereClause(filter: FilterData<*>): String {
        val filterData = filter as FilterData.EnumData<*>

        // Jeśli lista wartości jest pusta, nie ma sensu budować klauzuli
        if (filterData.values.value.isEmpty() && filterData.nullHandling.value == NullHandling.Ignore) {
            return ""
        }

        // Przygotowanie listy wartości enum jako stringi do zapytania SQL
        val enumValues = filterData.values.value.joinToString(", ") {
            "'${camelToSnakeCase(it.name).uppercase()}'"
        }

        return when {
            // Gdy lista jest niepusta i chcemy tylko te wartości (lub ich nie chcemy)
            filterData.values.value.isNotEmpty() -> {
                val operator = if (filterData.include.value) "IN" else "NOT IN"
                val valuesClause = "$columnName $operator ($enumValues)"

                when (filterData.nullHandling.value) {
                    NullHandling.Ignore -> valuesClause
                    NullHandling.Include -> "($valuesClause OR $columnName IS NULL)"
                    NullHandling.Exclude -> "($valuesClause AND $columnName IS NOT NULL)"
                }
            }

            // Gdy lista jest pusta, ale chcemy obsłużyć nulle
            else -> {
                when (filterData.nullHandling.value) {
                    NullHandling.Include -> "$columnName IS NULL"
                    NullHandling.Exclude -> "$columnName IS NOT NULL"
                    // Ten przypadek nie powinien wystąpić (pusta lista i ignorowanie null)
                    else -> ""
                }
            }
        }
    }

    @Composable
    override fun RenderFilter(
        currentFilter: FilterData<*>
    ) {

        val enumValues = remember { enumClass.java.enumConstants.toList() }

        @Suppress("UNCHECKED_CAST")
        val filterData = currentFilter as FilterData.EnumData<E>
        val selectedValues = filterData.values
        val include = filterData.include

        Column(modifier = Modifier.padding(8.dp)) {
            Text(
                text = "Wybierz wartości",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // Przełącznik dla trybu włączania/wyłączania
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tryb wyboru:")
                RadioButton(
                    selected = include.value,
                    onClick = {
                        include.value = true
                        filterData.markDirty()
                    }
                )
                Text("Uwzględnij zaznaczone", modifier = Modifier.padding(end = 8.dp))

                RadioButton(
                    selected = !include.value,
                    onClick = {
                        include.value = false
                        filterData.markDirty()
                    }
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
                        text = enumValue.let {
                            val method = it::class.members.find { member -> member.name == "toDisplayString" }
                            if (method != null) {
                                method.call(it) as String
                            } else {
                                it.toString()
                            }
                        },
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            NullHandlingPanel(filterData)
        }
    }
}