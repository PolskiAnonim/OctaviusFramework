package org.octavius.novels.report

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.navigator.Screen
import org.octavius.novels.report.column.ReportColumn
import org.octavius.novels.report.column.type.BooleanColumn
import org.octavius.novels.report.column.type.EnumColumn
import org.octavius.novels.report.column.type.IntegerColumn
import org.octavius.novels.report.column.type.StringColumn
import org.octavius.novels.report.column.type.StringListColumn

abstract class Report : Screen {

    private val query: Query
    private val columns: Map<String, ReportColumn>
    private val columnStates = mutableMapOf<String, ColumnState>()

    init {
        columns = this.createColumns()
        initializeColumnStates(columns)
        query = this.createQuery()
    }

    private val reportState = ReportState()

    private fun initializeColumnStates(columns: Map<String, ReportColumn>) {
        for ((key, column) in columns) {
            columnStates[key] = column.initializeState()
        }
    }

    open var onRowClick: ((Map<String, Any?>) -> Unit)? = null

    abstract fun createQuery(): Query

    abstract fun createColumns(): Map<String, ReportColumn>

    private fun fetchData(
        page: Int,
        onResult: (List<Map<String, Any?>>, Long) -> Unit
    ) {
        // Budowanie klauzuli WHERE dla wyszukiwania
        val whereClauseBuilder = StringBuilder()

        // Dodaj warunek wyszukiwania dla searchQuery jeśli nie jest pusty
        if (reportState.searchQuery.value.isNotEmpty()) {
            val escapedQuery = reportState.searchQuery.value.replace("'", "''")
            val searchConditions = mutableListOf<String>()

            // Dla każdej widocznej kolumny dodaj warunek wyszukiwania
            columns.forEach { (key, column) ->
                // Pomijamy kolumny, które są ukryte lub nie są typu tekstowego
                if (columnStates[key]?.visible?.value == true) {
                    when (column) {
                        is StringColumn, is StringListColumn -> {
                            // Dla kolumn tekstowych szukamy ILIKE
                            searchConditions.add("CAST(${column.name} AS TEXT) ILIKE '%$escapedQuery%'")
                        }
                        is EnumColumn<*> -> {
                            // Dla enumów szukamy po wartościach wyświetlanych
                            searchConditions.add("CAST(${column.name} AS TEXT) ILIKE '%$escapedQuery%'")
                        }
                        is IntegerColumn, is BooleanColumn -> {
                            // Dla pozostałych typów próbujemy konwersję do tekstu
                            if (escapedQuery.toIntOrNull() != null || escapedQuery.lowercase() in listOf("true", "false", "tak", "nie")) {
                                searchConditions.add("CAST(${column.name} AS TEXT) ILIKE '%$escapedQuery%'")
                            }
                        }
                    }
                }
            }

            // Łączymy wszystkie warunki operatorem OR
            if (searchConditions.isNotEmpty()) {
                whereClauseBuilder.append("(${searchConditions.joinToString(" OR ")})")
            }
        }

        // Dodaj warunki filtrowania z columnStates
        var filtersAdded = false
        for ((key, state) in columnStates) {
            val filter = state.filtering.value ?: continue
            if (!filter.isActive()) continue

            val column = columns[key] ?: continue
            val whereClause = column.constructWhereClause(filter)

            if (whereClause.isNotEmpty()) {
                if (whereClauseBuilder.isNotEmpty()) {
                    whereClauseBuilder.append(" AND ")
                }
                whereClauseBuilder.append(whereClause)
                filtersAdded = true
            }
        }

        // Dodaj klauzulę WHERE do zapytania SQL
        val finalSql = if (whereClauseBuilder.isNotEmpty()) {
            val whereClauseIndex = query.sql.lowercase().indexOf("where")
            val orderClauseIndex = query.sql.lowercase().indexOf("order")

            when {
                whereClauseIndex != -1 -> {
                    "${query.sql.substring(0, whereClauseIndex+5)} $whereClauseBuilder AND ${query.sql.substring(whereClauseIndex+5)}"
                }
                orderClauseIndex != -1 -> {
                    "${query.sql.substring(0, orderClauseIndex)} WHERE $whereClauseBuilder ${query.sql.substring(orderClauseIndex)}"
                }
                else -> {
                    "${query.sql} WHERE $whereClauseBuilder"
                }
            }
        } else {
            query.sql
        }

        try {
            val (results, totalCount) = DatabaseManager.executeQuery(
                sql = finalSql,
                params = query.params.toList(),
                page = page,
                pageSize = reportState.pageSize.value
            )

            onResult(results, totalCount)
        } catch (e: Exception) {
            println("Błąd podczas pobierania danych: ${e.message}")
            e.printStackTrace()
            onResult(emptyList(), 0)
        }
    }

    // Sprawdza, czy jakikolwiek filtr jest aktywny
    private fun areAnyFiltersActive(): Boolean {
        return columnStates.values.any { state ->
            state.filtering.value?.isActive() == true
        }
    }

    @Composable
    override fun Content(paddingValues: PaddingValues) {
        val lazyListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()
        var showFilters by remember { mutableStateOf(false) }
        var dataList by remember { mutableStateOf<List<Map<String, Any?>>>(emptyList()) }
        var totalPages by remember { mutableStateOf(1L) }

        // Dla śledzenia zmian filtrów
        val filteringState = derivedStateOf {
            columnStates.map { (key, state) ->
                key to (state.filtering.value?.dirtyState?.value)
            }.toMap()
        }

        // Efekt pobierający dane po zmianie parametrów
        LaunchedEffect(
            reportState.currentPage.value,
            reportState.searchQuery.value,
            filteringState.value  // Śledzenie zmian w filtrach
        ) {
            fetchData(reportState.currentPage.value) { result, pages ->
                dataList = result
                totalPages = pages
                reportState.totalPages.value = pages.toInt()
            }
        }

        Column(modifier = Modifier.padding(paddingValues)) {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f)
            ) {
                item {
                    // Pasek wyszukiwania i filtrowania
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = reportState.searchQuery.value,
                            onValueChange = {
                                reportState.searchQuery.value = it
                                reportState.currentPage.value = 1
                            },
                            modifier = Modifier.weight(1f),
                            placeholder = { Text("Szukaj...") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = "Szukaj"
                                )
                            },
                            trailingIcon = {
                                if (reportState.searchQuery.value.isNotEmpty()) {
                                    IconButton(
                                        onClick = {
                                            reportState.searchQuery.value = ""
                                            reportState.currentPage.value = 1
                                        }
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Wyczyść"
                                        )
                                    }
                                }
                            },
                            singleLine = true
                        )

                        // Przycisk filtrów z aktywnym wskaźnikiem
                        IconButton(
                            onClick = { showFilters = !showFilters },
                            modifier = Modifier.padding(start = 8.dp)
                        ) {
                            if (areAnyFiltersActive()) {
                                Badge(
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.FilterListOff,
                                        contentDescription = "Filtry aktywne",
                                        tint = MaterialTheme.colorScheme.onPrimary
                                    )
                                }
                            } else {
                                Icon(
                                    imageVector = Icons.Default.FilterList,
                                    contentDescription = "Filtry"
                                )
                            }
                        }
                    }

                    // Panel filtrów
                    if (showFilters) {
                        FilterPanel(
                            columns = columns.filter { it.value.filterable },
                            columnStates = columnStates.filter { it.value.filtering.value != null },
                            onPageReset = {
                                reportState.currentPage.value = 1
                            }
                        )
                    }
                }

                item {
                    // Nagłówki kolumn
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.primaryContainer)
                            .padding(vertical = 8.dp)
                    ) {
                        columns.forEach { (key, column) ->
                            Box(
                                modifier = Modifier
                                    .weight(column.width)
                                    .padding(horizontal = 4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = column.header,
                                        style = MaterialTheme.typography.titleMedium,
                                        textAlign = TextAlign.Center
                                    )

                                    // Ikona sortowania, jeśli kolumna obsługuje sortowanie
                                    /*if (column.sortable) {
                                        val sortValue = reportState.sorting[key]

                                        IconButton(
                                            onClick = {
                                                when (sortValue?.direction?.value) {
                                                    null -> {
                                                        reportState.sorting[key] = SortValue(
                                                            mutableStateOf(key),
                                                            mutableStateOf(SortDirection.ASC)
                                                        )
                                                    }

                                                    SortDirection.ASC -> {
                                                        reportState.sorting[key] = SortValue(
                                                            mutableStateOf(key),
                                                            mutableStateOf(SortDirection.DESC)
                                                        )
                                                    }

                                                    else -> {
                                                        reportState.sorting.remove(key)
                                                    }
                                                }
                                            },
                                            modifier = Modifier.size(24.dp)
                                        ) {
                                            when (sortValue?.direction?.value) {
                                                null -> Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                                    contentDescription = "Sortuj",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                                )

                                                SortDirection.ASC -> Icon(
                                                    imageVector = Icons.Default.ArrowUpward,
                                                    contentDescription = "Sortuj rosnąco",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )

                                                SortDirection.DESC -> Icon(
                                                    imageVector = Icons.Default.ArrowDownward,
                                                    contentDescription = "Sortuj malejąco",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                                )

                                                else -> Icon(
                                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                                    contentDescription = "Sortuj",
                                                    tint = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.5f)
                                                )
                                            }
                                        }
                                    } */

                                    // Wskaźnik aktywnego filtra
                                    val columnState = columnStates[key]
                                    if (column.filterable && columnState!!.filtering.value?.isActive() == true) {
                                        Icon(
                                            imageVector = Icons.Default.FilterAlt,
                                            contentDescription = "Filtr aktywny",
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                items(dataList) { rowData ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .run {
                                if (onRowClick != null) {
                                    this.clickable { onRowClick!!.invoke(rowData) }
                                } else {
                                    this
                                }
                            }
                    ) {
                        columns.forEach { (_, column) ->
                            Box(
                                modifier = Modifier
                                    .weight(column.width)
                                    .padding(horizontal = 4.dp)
                            ) {
                                column.RenderCell(rowData, Modifier)
                            }
                        }
                    }

                    // Separator
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(1.dp)
                            .background(MaterialTheme.colorScheme.outlineVariant)
                    )
                }
            }

            // Paginacja
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = {
                            if (reportState.currentPage.value > 1) {
                                reportState.currentPage.value--
                                coroutineScope.launch {
                                    lazyListState.scrollToItem(0)
                                }
                            }
                        },
                        enabled = reportState.currentPage.value > 1
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Poprzednia strona",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = "Strona ${reportState.currentPage.value} z ${reportState.totalPages.value}",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    IconButton(
                        onClick = {
                            if (reportState.currentPage.value < reportState.totalPages.value) {
                                reportState.currentPage.value++
                                coroutineScope.launch {
                                    lazyListState.scrollToItem(0)
                                }
                            }
                        },
                        enabled = reportState.currentPage.value < reportState.totalPages.value
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Następna strona",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }
        }
    }
}