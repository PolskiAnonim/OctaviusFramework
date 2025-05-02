package org.octavius.novels.navigator

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

class TabNavigator(
    private val tabs: List<Tab>,
    initialIndex: UShort = 0u,
) {
    private var directionOfTabAnimation = 0
    private val currentIndexState: MutableState<UShort> = mutableStateOf(initialIndex)

    var currentIndex: UShort
        get() = currentIndexState.value
        set(value) {
            currentIndexState.value = value
        }

    val current: Tab
        @Composable
        get() = tabs.first { it.index == currentIndex }

    @Composable
    fun Display() {
        Column(modifier = Modifier.fillMaxSize()) {
            // Pasek zakładek
            TabBar()

            // Zawartość aktualnej zakładki
            Box(modifier = Modifier.fillMaxSize()) {
                // Wyświetlamy tylko aktualną zakładkę bez animacji
                tabs.firstOrNull { it.index == currentIndex }?.let { tab ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        tab.Content()
                    }
                }
            }
        }
    }

    @Composable
    private fun TabBar() {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            color = MaterialTheme.colorScheme.primary
        ) {
            Row(
                modifier = Modifier.fillMaxSize(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tabs.forEach { tab ->
                    val isSelected = tab.index == currentIndex
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clickable { changeTab(tab.index) }
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
                                    tint = if (isSelected)
                                        MaterialTheme.colorScheme.onPrimaryContainer
                                    else
                                        MaterialTheme.colorScheme.onPrimary
                                )
                            }

                            Text(
                                text = tab.options.title,
                                style = MaterialTheme.typography.bodyMedium,
                                textAlign = TextAlign.Center,
                                color = if (isSelected)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            }
        }
    }

    private fun changeTab(newIndex: UShort) {
        directionOfTabAnimation = if (currentIndex > newIndex) -1 else 1
        currentIndex = newIndex
    }
}