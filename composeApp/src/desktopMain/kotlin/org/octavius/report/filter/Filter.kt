package org.octavius.report.filter

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.localization.Translations
import org.octavius.report.FilterMode
import org.octavius.report.NullHandling
import org.octavius.report.Query
import org.octavius.report.filter.data.FilterData

abstract class Filter<T : FilterData> {

    /**
     * Tworzy domyślny, pusty obiekt danych dla tego filtra.
     */
    abstract fun createDefaultData(): T

    /**
     * Renderuje unikalny interfejs użytkownika dla tego konkretnego filtra.
     * Ta metoda będzie implementowana przez każdą podklasę (BooleanFilter, StringFilter, etc.).
     */
    @Composable
    abstract fun RenderFilterUI(data: T)

    /**
     * Tworzy fragment zapytania SQL na podstawie danych z filtra.
     */
    protected abstract fun buildBaseQueryFragment(columnName: String, data: T): Query?

    fun createQueryFragment(columnName: String, data: T): Query? {
        if (!data.isActive()) return null

        val baseQuery = buildBaseQueryFragment(columnName, data)
        return applyNullHandling(columnName, baseQuery, data)
    }

    private fun applyNullHandling(columnName: String, baseQuery: Query?, data: T): Query? {
        return when (data.nullHandling.value) {
            NullHandling.Ignore -> baseQuery
            NullHandling.Include -> baseQuery?.let {
                Query("(${it.sql} OR $columnName IS NULL)", it.params)
            } ?: Query("$columnName IS NULL")
            NullHandling.Exclude -> baseQuery?.let {
                Query("(${it.sql} AND $columnName IS NOT NULL)", it.params)
            } ?: Query("$columnName IS NOT NULL")
        }
    }

    /**
     * Główny punkt wejścia do renderowania filtra.
     * Buduje cały UI, włączając w to wspólne panele.
     */
    @Composable
    fun Render(data: T) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Renderujemy unikalną część dla danego typu filtra
            RenderFilterUI(data)

            // Renderujemy wspólne elementy, które były w FilterRenderer.txt
            FilterSpacer()
            FilterModePanel(data)
            FilterSpacer()
            NullHandlingPanel(data)
        }
    }


    // --- WSPÓLNE KOMPONENTY UI ---

    @Composable
    fun NullHandlingPanel(filterData: FilterData) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Translations.get("filter.null.values"),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp)
            )

            RadioButton(
                selected = filterData.nullHandling.value == NullHandling.Ignore,
                onClick = {
                    filterData.nullHandling.value = NullHandling.Ignore
                }
            )
            Text(Translations.get("filter.null.ignore"), modifier = Modifier.padding(end = 12.dp))

            RadioButton(
                selected = filterData.nullHandling.value == NullHandling.Include,
                onClick = {
                    filterData.nullHandling.value = NullHandling.Include
                }
            )
            Text(Translations.get("filter.null.include"), modifier = Modifier.padding(end = 12.dp))

            RadioButton(
                selected = filterData.nullHandling.value == NullHandling.Exclude,
                onClick = {
                    filterData.nullHandling.value = NullHandling.Exclude
                }
            )
            Text(Translations.get("filter.null.exclude"))
        }
    }

    @Composable
    fun FilterModePanel(filterData: FilterData) {
        if (filterData.mode.value == FilterMode.Single) return
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = Translations.get("filter.mode"),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp)
            )

            RadioButton(
                selected = filterData.mode.value == FilterMode.ListAny,
                onClick = {
                    filterData.mode.value = FilterMode.ListAny
                }
            )
            Text(FilterMode.ListAny.toDisplayString(), modifier = Modifier.padding(end = 12.dp))

            RadioButton(
                selected = filterData.mode.value == FilterMode.ListAll,
                onClick = {
                    filterData.mode.value = FilterMode.ListAll
                }
            )
            Text(FilterMode.ListAll.toDisplayString())
        }
    }
}