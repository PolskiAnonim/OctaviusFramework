package org.octavius.novels.component

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.database.LocalDatabase
import org.octavius.novels.state.LocalState
import org.octavius.novels.state.State
import kotlin.reflect.KClass


class VisibleList<T : Any>(private val clazz: KClass<T>) {
    private lateinit var databaseManager: DatabaseManager
    private lateinit var state: State
    private var elementList = mutableStateOf<List<T>>(emptyList())
    private lateinit var lazyListState: LazyListState;
    @Composable
    fun Service(
        tableName: String,
        columnName: String, // Nazwa kolumny do przeszukiwania
        searchType: String // Typ wyszukiwania: "array" (dla unnest) lub "text" (dla zwykłych kolumn)
    ) {
        databaseManager = LocalDatabase.current
        state = LocalState.current
        lazyListState = rememberLazyListState()
        LaunchedEffect(state.searchQuery.value) {
            // Resetujemy stronę do 1 przy każdej zmianie wyszukiwania
            state.currentPage.value = 1
        }

        LaunchedEffect(state.currentPage.value, state.searchQuery.value) {
            lazyListState.scrollToItem(0,0)
            try {
                // Bezpieczne użycie zapytania - unikamy wstrzyknięcia SQL
                val escapedQuery = state.searchQuery.value.replace("'", "''")

                // Wybór odpowiedniego typu warunku w zależności od parametrów
                val whereClause = when (searchType) {
                    "array" -> """
                    WHERE EXISTS (
                        SELECT 1
                        FROM unnest($columnName) AS item
                        WHERE item ILIKE '%$escapedQuery%'
                    )
                """
                    "text" -> """
                    WHERE $columnName ILIKE '%$escapedQuery%'
                """
                    else -> "" // Domyślnie pusty warunek
                }

                val (elements, total) = databaseManager.getDataForPage(
                    tableName, // Używamy przekazanej nazwy tabeli
                    state.currentPage.value,
                    state.pageSize,
                    whereClause,
                    clazz
                )
                elementList.value = elements
                state.totalPages.value = (total + state.pageSize - 1) / state.pageSize
            } catch (e: Exception) {
                // Obsługa błędów
                println(e.toString())
            }
        }
    }

    @Composable
    fun List(paddingValues: PaddingValues, fieldName: String) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = lazyListState
        ) {
            items(elementList.value) { element ->
                ListItem(element, fieldName, { println("aaaaa") })
                Spacer(modifier = Modifier.height(8.dp)) // opcjonalnie, dla odstępu między elementami
            }
        }
    }

    @Composable
    fun BottomBar() {
        BottomAppBar(
            backgroundColor = MaterialTheme.colors.primary
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        if (state.currentPage.value > 1) state.currentPage.value--
                    },
                    enabled = state.currentPage.value > 1
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Poprzednia strona",
                        tint = MaterialTheme.colors.onPrimary
                    )
                }

                Text(
                    text = "Strona ${state.currentPage.value} z ${state.totalPages.value}",
                    color = MaterialTheme.colors.onPrimary
                )

                IconButton(
                    onClick = {
                        if (state.currentPage.value < state.totalPages.value) state.currentPage.value++
                    },
                    enabled = state.currentPage.value < state.totalPages.value
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = "Następna strona",
                        tint = MaterialTheme.colors.onPrimary
                    )
                }
            }
        }
    }

    @Composable
    fun TopBar() {
        TopAppBar(
            modifier = Modifier.fillMaxWidth(),
            backgroundColor = MaterialTheme.colors.primary
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wyszukiwarka
                TextField(
                    value = state.searchQuery.value,
                    onValueChange = { state.searchQuery.value = it },
                    modifier = Modifier
                        .weight(1f),
                    placeholder = { Text("Szukaj...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Szukaj"
                        )
                    },
                    trailingIcon = {
                        if (state.searchQuery.value.isNotEmpty()) {
                            IconButton(
                                onClick = { state.searchQuery.value = "" }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Wyczyść"
                                )
                            }
                        }
                    },
                    singleLine = true,
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = MaterialTheme.colors.surface
                    )
                )

                // Przycisk dodawania
                IconButton(
                    onClick = { /* TODO: Akcja dodawania */ },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Dodaj",
                        tint = MaterialTheme.colors.onPrimary
                    )
                }
            }
        }
    }

    @Composable
    fun ListItem(data: T, fieldName: String, onNavigate: () -> Unit) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp)
                .border(1.dp, Color.Gray, RoundedCornerShape(4.dp))
        ) {
            val property = clazz.members.find { it.name == fieldName }
            property?.let { member ->
                when (val value = member.call(data)) {
                    is List<*> -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tytuł:",
                                modifier = Modifier.padding(4.dp),
                                style = MaterialTheme.typography.subtitle1
                            )
                            // Przycisk nawigacyjny
                            IconButton(onClick = onNavigate) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Przejdź dalej"
                                )
                            }
                        }
                        value.forEach { item ->
                            SelectionContainer {
                                Text(
                                    text = item.toString(),
                                    modifier = Modifier.padding(start = 12.dp, top = 2.dp, bottom = 2.dp),
                                    style = MaterialTheme.typography.body2
                                )
                            }
                        }
                    }
                    else -> {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SelectionContainer {
                                Text(
                                    text = "$fieldName: $value",
                                    modifier = Modifier.padding(4.dp),
                                    style = MaterialTheme.typography.body1
                                )
                            }
                            // Przycisk nawigacyjny
                            IconButton(onClick = onNavigate) {
                                Icon(
                                    imageVector = Icons.Default.ArrowForward,
                                    contentDescription = "Przejdź dalej"
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}