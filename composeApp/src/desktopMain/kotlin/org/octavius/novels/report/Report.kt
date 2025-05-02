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
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.report.column.ReportColumn

class Report(
    private val sql: String,
    private val columns: List<ReportColumn>,
    private val params: Array<Any?> = emptyArray(),
    private val pageSize: Int = 10,
    private val onRowClick: ((Map<String, Any?>) -> Unit)? = null
) {
    private val currentPage = mutableStateOf(1)
    private val totalPages = mutableStateOf(1L)
    private val dataList = mutableStateOf<List<Map<String, Any?>>>(emptyList())
    private val searchQuery = mutableStateOf("")

    @Composable
    fun Display(modifier: Modifier = Modifier) {
        val lazyListState = rememberLazyListState()
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(currentPage.value, searchQuery.value) {
            fetchData()
        }

        Column(modifier = modifier) {
            // Pasek wyszukiwania
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = searchQuery.value,
                    onValueChange = {
                        searchQuery.value = it
                        currentPage.value = 1
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
                        if (searchQuery.value.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery.value = ""
                                    currentPage.value = 1
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
            }

            // Nagłówki kolumn
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.primaryContainer)
                    .padding(vertical = 8.dp)
            ) {
                columns.forEach { column ->
                    Box(
                        modifier = Modifier
                            .weight(column.width)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = column.header,
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Dane
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.weight(1f)
            ) {
                items(dataList.value) { rowData ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .run {
                                if (onRowClick != null) {
                                    this.clickable { onRowClick.invoke(rowData) }
                                } else {
                                    this
                                }
                            }
                    ) {
                        columns.forEach { column ->
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
                            if (currentPage.value > 1) {
                                currentPage.value--
                                coroutineScope.launch {
                                    lazyListState.scrollToItem(0)
                                }
                            }
                        },
                        enabled = currentPage.value > 1
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Poprzednia strona",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Text(
                        text = "Strona ${currentPage.value} z ${totalPages.value}",
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    IconButton(
                        onClick = {
                            if (currentPage.value < totalPages.value) {
                                currentPage.value++
                                coroutineScope.launch {
                                    lazyListState.scrollToItem(0)
                                }
                            }
                        },
                        enabled = currentPage.value < totalPages.value
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

    private fun fetchData() {
        val finalSql = if (searchQuery.value.isNotEmpty()) {
            // Dodaj warunek wyszukiwania - to jest uproszczone i może wymagać dostosowania
            val escapedQuery = searchQuery.value.replace("'", "''")

            if (sql.lowercase().contains("where")) {
                "$sql AND (CAST(id AS TEXT) ILIKE '%$escapedQuery%')"
            } else {
                "$sql WHERE (CAST(id AS TEXT) ILIKE '%$escapedQuery%')"
            }
        } else {
            sql
        }

        try {
            val (results, totalCount) = DatabaseManager.executeQuery(
                sql = finalSql,
                params = params.toList(),
                page = currentPage.value,
                pageSize = pageSize
            )

            dataList.value = results
            totalPages.value = (totalCount + pageSize - 1) / pageSize
        } catch (e: Exception) {
            println("Błąd podczas pobierania danych: ${e.message}")
            e.printStackTrace()
        }
    }
}