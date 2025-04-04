package org.octavius.novels.component

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.navigator.LocalNavigator
import org.octavius.novels.screens.NovelEditScreen
import org.octavius.novels.state.LocalState
import org.octavius.novels.state.State
import kotlin.reflect.KClass

class VisibleList<T : Any>(private val clazz: KClass<T>) {
    private lateinit var state: State
    private var elementList = mutableStateOf<List<T>>(emptyList())
    private lateinit var lazyListState: LazyListState

    @Composable
    fun Service(
        tableName: String,
        columnName: String,
        searchType: String
    ) {
        state = LocalState.current
        lazyListState = rememberLazyListState()

        LaunchedEffect(state.searchQuery.value) {
            state.currentPage.value = 1
        }

        LaunchedEffect(state.currentPage.value, state.searchQuery.value) {
            lazyListState.scrollToItem(0,0)
            try {
                val escapedQuery = state.searchQuery.value.replace("'", "''")

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
                    else -> ""
                }

                val (elements, total) = DatabaseManager.getDataForPage(
                    tableName,
                    state.currentPage.value,
                    state.pageSize,
                    whereClause,
                    clazz
                )
                elementList.value = elements
                state.totalPages.value = (total + state.pageSize - 1) / state.pageSize
            } catch (e: Exception) {
                println(e.toString())
            }
        }
    }

    @Composable
    fun List(paddingValues: PaddingValues, fieldName: String) {
        val navigator = LocalNavigator.current
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            state = lazyListState
        ) {
            items(elementList.value) { element ->
                ListItem(element, fieldName, { navigator.AddScreen(
                    NovelEditScreen(clazz.members.find { it.name == "id" }?.call(element) as Int?)) })
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }

    @Composable
    fun BottomBar() {
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
                        if (state.currentPage.value > 1) state.currentPage.value--
                    },
                    enabled = state.currentPage.value > 1
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Poprzednia strona",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }

                Text(
                    text = "Strona ${state.currentPage.value} z ${state.totalPages.value}",
                    color = MaterialTheme.colorScheme.onPrimaryContainer
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
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }

    @Composable
    fun TopBar() {
        val navigator = LocalNavigator.current
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = MaterialTheme.colorScheme.primary
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Wyszukiwarka
                OutlinedTextField(
                    value = state.searchQuery.value,
                    onValueChange = { state.searchQuery.value = it },
                    modifier = Modifier.weight(1f),
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
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedTextColor = MaterialTheme.colorScheme.onSurface
                    )
                )

                // Przycisk dodawania
                IconButton(
                    onClick = { navigator.AddScreen(NovelEditScreen()) },
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Dodaj",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    }

    @Composable
    fun ListItem(data: T, fieldName: String, onNavigate: () -> Unit) {
        ElevatedCard(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(8.dp)
        ) {
            val property = clazz.members.find { it.name == fieldName }
            property?.let { member ->
                when (val value = member.call(data)) {
                    is List<*> -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Tytuł:",
                                style = MaterialTheme.typography.titleMedium
                            )

                            // Przycisk nawigacyjny
                            FilledTonalButton(onClick = onNavigate) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                    contentDescription = "Przejdź dalej"
                                )
                            }
                        }

                        Column(modifier = Modifier.padding(start = 16.dp, bottom = 16.dp)) {
                            value.forEach { item ->
                                SelectionContainer {
                                    Text(
                                        text = item.toString(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                    else -> {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            SelectionContainer {
                                Text(
                                    text = "$fieldName: $value",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }

                            // Przycisk nawigacyjny
                            FilledTonalButton(onClick = onNavigate) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
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