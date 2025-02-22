package org.octavius.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.database.LocalDatabase
import org.octavius.novels.domain.Novel
import org.octavius.novels.navigator.Screen
import org.octavius.novels.state.LocalState
import org.octavius.novels.state.State

object NovelListScreen : Screen {
    private lateinit var state: State
    private lateinit var databaseManager: DatabaseManager

    @Composable
    override fun Content() {
        state = LocalState.current
        databaseManager = LocalDatabase.current



        Scaffold(
            topBar = { topBar() },
            bottomBar = { bottomBar() }
        ) { paddingValues ->
            // Tutaj możesz dodać zawartość głównego ekranu
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Text("Zawartość głównego ekranu")
            }
        }
    }

    @Composable
    fun topBar() {
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
    fun bottomBar() {
        LaunchedEffect(state.currentPage.value, state.searchQuery.value) {
            try {
                val (elements, total)= databaseManager.getDataForPage(
                    "novels",
                    state.currentPage.value,
                    state.pageSize,
                    """
                        WHERE EXISTS (
                            SELECT 1
                            FROM unnest(titles) AS title
                            WHERE title ILIKE '%${state.searchQuery.value}%'
                        )
                    """,
                    Novel::class
                )
                state.elementList = elements
                state.totalPages.value = (total + state.pageSize - 1) / state.pageSize
                println(state.elementList.toString())
            } catch (e: Exception) {
                // Obsługa błędów
                println(e.toString())
            }
        }

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

}