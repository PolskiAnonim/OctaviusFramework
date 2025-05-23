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
import org.octavius.novels.report.FilterValue
import org.octavius.novels.report.NullHandling
import org.octavius.novels.report.filter.Filter
import org.octavius.novels.util.Converters.camelToSnakeCase
import kotlin.reflect.KClass

class EnumFilter<E : Enum<*>>(columnName: String, val enumClass: KClass<E>) : Filter(columnName) {

    override fun constructWhereClause(filter: FilterValue<*>): String {
        val enumFilter = filter as FilterValue.EnumFilter<*>

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
                val valuesClause = "$columnName $operator ($enumValues)"

                when (enumFilter.nullHandling.value) {
                    NullHandling.Ignore -> valuesClause
                    NullHandling.Include -> "($valuesClause OR $columnName IS NULL)"
                    NullHandling.Exclude -> "($valuesClause AND $columnName IS NOT NULL)"
                }
            }

            // Gdy lista jest pusta, ale chcemy obsłużyć nulle
            else -> {
                when (enumFilter.nullHandling.value) {
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
        currentFilter: FilterValue<*>
    ) {

        val enumValues = remember { enumClass.java.enumConstants.toList() }

        @Suppress("UNCHECKED_CAST")
        val enumFilter = currentFilter as FilterValue.EnumFilter<E>
        val selectedValues = enumFilter.values
        val include = enumFilter.include

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
                        enumFilter.markDirty()
                    }
                )
                Text("Uwzględnij zaznaczone", modifier = Modifier.padding(end = 8.dp))

                RadioButton(
                    selected = !include.value,
                    onClick = {
                        include.value = false
                        enumFilter.markDirty()
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

            NullHandlingPanel(enumFilter)
        }
    }
}