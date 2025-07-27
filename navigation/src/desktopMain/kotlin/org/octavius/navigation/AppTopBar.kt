package org.octavius.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import org.octavius.contract.Tab

/**
 * Komponent UI wyświetlający główny pasek zakładek aplikacji.
 *
 * Renderuje listę zakładek, podświetlając aktualnie wybraną. Umożliwia nawigację
 * między głównymi sekcjami aplikacji.
 *
 * @param tabs Lista obiektów [Tab], które mają być wyświetlone.
 * @param currentState Aktualny stan nawigacji, używany do określenia, która zakładka jest aktywna.
 * @param onTabSelected Funkcja zwrotna wywoływana po kliknięciu zakładki, przekazująca jej indeks.
 */
@Composable
fun AppTabBar(
    tabs: List<Tab>,
    currentState: AppNavigationState,
    onTabSelected: (UShort) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth().height(56.dp), color = MaterialTheme.colorScheme.primary
    ) {
        Row(
            modifier = Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically
        ) {
            tabs.forEach { tab ->
                val isSelected = tab.index == currentState.activeTab.index
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clickable { onTabSelected.invoke(tab.index) }
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.primaryContainer
                            else MaterialTheme.colorScheme.primary
                        )
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        tab.options.icon?.let {
                            Icon(
                                painter = it,
                                contentDescription = tab.options.title,
                                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                else MaterialTheme.colorScheme.onPrimary
                            )
                        }

                        Text(
                            text = tab.options.title,
                            style = MaterialTheme.typography.bodyMedium,
                            textAlign = TextAlign.Center,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }
        }
    }
}