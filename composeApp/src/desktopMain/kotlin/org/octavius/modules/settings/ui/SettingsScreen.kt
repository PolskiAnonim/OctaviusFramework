package org.octavius.modules.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Api
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import org.octavius.localization.Translations
import org.octavius.navigator.LocalNavigator
import org.octavius.navigator.Screen
import org.octavius.ui.theme.FormSpacing

class SettingsScreen() : Screen {
    override val title = Translations.get("settings.title")

    data class SettingOption(
        val title: String,
        val description: String,
        val icon: ImageVector,
        val onClick: () -> Unit
    )

    @Composable
    override fun Content() {
        val navigator = LocalNavigator.current

        val settingOptions = listOf(
            SettingOption(
                title = Translations.get("settings.language.title"),
                description = Translations.get("settings.language.description"),
                icon = Icons.Default.Language,
                onClick = { /* TODO: Navigate to language settings */ }
            ),
            SettingOption(
                title = Translations.get("settings.database.title"),
                description = Translations.get("settings.database.description"),
                icon = Icons.Default.Storage,
                onClick = { /* TODO: Navigate to database settings */ }
            ),
            SettingOption(
                title = Translations.get("settings.api.title"),
                description = Translations.get("settings.api.description"),
                icon = Icons.Default.Api,
                onClick = {
                    navigator.addScreen(ApiIntegrationsReportScreen(navigator))
                }
            )
        )

        LazyColumn(
            modifier = Modifier.Companion.fillMaxSize().padding(FormSpacing.cardPadding),
            verticalArrangement = Arrangement.spacedBy(FormSpacing.controlSpacing)
        ) {
            items(settingOptions) { option ->
                SettingOptionCard(option)
            }
        }
    }

    @Composable
    private fun SettingOptionCard(option: SettingOption) {
        Card(
            modifier = Modifier.Companion.fillMaxWidth(),
            onClick = option.onClick
        ) {
            Row(
                modifier = Modifier.Companion
                    .fillMaxWidth()
                    .padding(FormSpacing.cardPadding),
                verticalAlignment = Alignment.Companion.CenterVertically
            ) {
                Icon(
                    imageVector = option.icon,
                    contentDescription = null,
                    modifier = Modifier.Companion.size(24.dp)
                )

                Spacer(modifier = Modifier.Companion.width(FormSpacing.cardPadding))

                Column(modifier = Modifier.Companion.weight(1f)) {
                    Text(
                        text = option.title,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        text = option.description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}