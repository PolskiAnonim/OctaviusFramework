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
import kotlinx.serialization.json.JsonObject
import org.octavius.localization.T
import org.octavius.report.FilterMode
import org.octavius.report.NullHandling
import org.octavius.report.Query
import org.octavius.report.ReportEvent
import org.octavius.report.filter.data.FilterData

abstract class Filter<T : FilterData> {

    /**
     * Tworzy domyślny, pusty obiekt danych dla tego filtra.
     */
    abstract fun createDefaultData(): T

    abstract fun deserializeData(data: JsonObject): T

    /**
     * Renderuje unikalny interfejs użytkownika dla tego konkretnego filtra.
     * Ta metoda będzie implementowana przez każdą podklasę (BooleanFilter, StringFilter, etc.).
     */
    @Composable
    abstract fun RenderFilterUI(onEvent: (ReportEvent) -> Unit, columnKey: String, data: T)

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
        return when (data.nullHandling) {
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
    fun Render(onEvent: (ReportEvent) -> Unit, columnKey: String, data: T) {
        Column(modifier = Modifier.padding(8.dp)) {
            // Renderujemy unikalną część dla danego typu filtra
            RenderFilterUI(onEvent, columnKey, data)

            FilterSpacer()
            FilterModePanel(onEvent, columnKey, data)
            FilterSpacer()
            NullHandlingPanel(onEvent, columnKey, data)
        }
    }


    // --- WSPÓLNE KOMPONENTY UI ---

    @Composable
    fun NullHandlingPanel(onEvent: (ReportEvent) -> Unit, columnKey: String, filterData: FilterData) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = T.get("filter.null.values"),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp)
            )

            RadioButton(
                selected = filterData.nullHandling == NullHandling.Ignore,
                onClick = {
                    onEvent.invoke(
                        ReportEvent.FilterChanged(
                            columnKey,
                            filterData.withNullHandling(NullHandling.Ignore)
                        )
                    )
                }
            )
            Text(T.get("filter.null.ignore"), modifier = Modifier.padding(end = 12.dp))

            RadioButton(
                selected = filterData.nullHandling == NullHandling.Include,
                onClick = {
                    ReportEvent.FilterChanged(
                        columnKey,
                        filterData.withNullHandling(NullHandling.Include)
                    )
                }
            )
            Text(T.get("filter.null.include"), modifier = Modifier.padding(end = 12.dp))

            RadioButton(
                selected = filterData.nullHandling == NullHandling.Exclude,
                onClick = {
                    onEvent.invoke(
                        ReportEvent.FilterChanged(
                            columnKey,
                            filterData.withNullHandling(NullHandling.Exclude)
                        )
                    )

                }
            )
            Text(T.get("filter.null.exclude"))
        }
    }

    @Composable
    fun FilterModePanel(onEvent: (ReportEvent) -> Unit, columnKey: String, filterData: FilterData) {
        if (filterData.mode == FilterMode.Single) return
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = T.get("filter.mode"),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(end = 8.dp)
            )

            RadioButton(
                selected = filterData.mode == FilterMode.ListAny,
                onClick = {
                    onEvent.invoke(ReportEvent.FilterChanged(columnKey, filterData.withMode(FilterMode.ListAny)))
                }

            )
            Text(FilterMode.ListAny.toDisplayString(), modifier = Modifier.padding(end = 12.dp))

            RadioButton(
                selected = filterData.mode == FilterMode.ListAll,
                onClick = {
                    onEvent.invoke(ReportEvent.FilterChanged(columnKey, filterData.withMode(FilterMode.ListAll)))
                }
            )
            Text(FilterMode.ListAll.toDisplayString())
        }
    }
}