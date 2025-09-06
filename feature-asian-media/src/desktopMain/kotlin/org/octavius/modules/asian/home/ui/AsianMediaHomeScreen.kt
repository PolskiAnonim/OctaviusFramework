package org.octavius.modules.asian.home.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.octavius.localization.T
import org.octavius.modules.asian.form.ui.AsianMediaFormScreen
import org.octavius.modules.asian.home.model.AsianMediaHomeState
import org.octavius.modules.asian.home.model.AsianMediaHomeHandler
import org.octavius.modules.asian.home.model.DashboardItem
import org.octavius.modules.asian.report.ui.AsianMediaReportScreen
import org.octavius.navigation.AppRouter
import org.octavius.navigation.Screen

class AsianMediaHomeScreen(override val title: String) : Screen {
    @Composable
    override fun Content() {
        val handler = remember { AsianMediaHomeHandler() }
        val state by handler.state.collectAsState()

        Scaffold(
            floatingActionButton = {
                ExtendedFloatingActionButton(
                    text = { Text(T.get("asianMedia.report.newTitle")) },
                    icon = { Icon(Icons.Default.Add, contentDescription = null) },
                    onClick = {
                        AppRouter.navigateTo(AsianMediaFormScreen.create())
                    }
                )
            },
            floatingActionButtonPosition = FabPosition.End
        ) { paddingValues ->
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize().padding(paddingValues),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    item { StatsHeader(state) }

                    item {
                        OutlinedButton(
                            onClick = {
                                AppRouter.navigateTo(AsianMediaReportScreen.create())
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                Icons.AutoMirrored.Filled.Article,
                                contentDescription = null,
                                modifier = Modifier.size(ButtonDefaults.IconSize)
                            )
                            Spacer(Modifier.size(ButtonDefaults.IconSpacing))
                            Text(T.get("asianMedia.home.viewFullList"))
                        }
                    }

                    item {
                        QuickAccessList(
                            title = T.get("asianMedia.home.currentlyReading"),
                            items = state.currentlyReading,
                            onItemClick = { item ->
                                AppRouter.navigateTo(AsianMediaFormScreen.create(entityId = item.id))
                            }
                        )
                    }

                    item {
                        QuickAccessList(
                            title = T.get("asianMedia.home.recentlyAdded"),
                            items = state.recentlyAdded,
                            onItemClick = { item ->
                                AppRouter.navigateTo(AsianMediaFormScreen.create(entityId = item.id))
                            }
                        )
                    }
                }
            }
        }
    }

    companion object {
        fun create(): Screen {
            return AsianMediaHomeScreen(T.get("asianMedia.home.title"))
        }
    }
}


@Composable
private fun StatsHeader(state: AsianMediaHomeState) {
    // ... (bez zmian)
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(
            T.get("asianMedia.home.totalTitles"),
            state.totalTitles.toString(),
            Modifier.weight(1f)
        )
        StatCard(
            T.get("asianMedia.home.reading"),
            state.readingCount.toString(),
            Modifier.weight(1f)
        )
        StatCard(
            T.get("asianMedia.home.completed"),
            state.completedCount.toString(),
            Modifier.weight(1f)
        )
    }
}

@Composable
private fun StatCard(title: String, value: String, modifier: Modifier = Modifier) {
    // ... (bez zmian)
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = value, style = MaterialTheme.typography.headlineMedium)
            Text(text = title, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun QuickAccessList(
    title: String,
    items: List<DashboardItem>,
    onItemClick: (DashboardItem) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(text = title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)

        if (items.isEmpty()) {
            Text(
                text = T.get("asianMedia.home.noItems"),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(start = 8.dp, top = 8.dp)
            )
        } else {
            Card {
                Column {
                    items.forEachIndexed { index, item ->
                        ListItem(
                            headlineContent = { Text(item.mainTitle) },
                            trailingContent = { Icon(Icons.Default.ChevronRight, null) },
                            modifier = Modifier.clickable { onItemClick(item) }
                        )
                        if (index < items.lastIndex) {
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = DividerDefaults.Thickness,
                                color = DividerDefaults.color
                            )
                        }
                    }
                }
            }
        }
    }
}