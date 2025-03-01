package org.octavius.novels

import androidx.compose.runtime.*
import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.database.LocalDatabase
import org.octavius.novels.state.LocalState
import org.octavius.novels.state.State
import org.octavius.novels.theme.NovelsTheme
import org.octavius.novels.screens.MainScreen


@Composable
fun App() {
    val databaseManager = DatabaseManager("jdbc:postgresql://localhost:5430/novels_games", "postgres", "1234")


    NovelsTheme {
        CompositionLocalProvider(LocalState provides State()) {
            CompositionLocalProvider(LocalDatabase provides databaseManager) {
                MainScreen.Content()
            }
        }
    }
}