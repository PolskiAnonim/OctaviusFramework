package org.octavius.novels.navigator

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

            // Zawartość aktualnej zakładki z animacją
            AnimatedContent(
                targetState = currentIndex,
                transitionSpec = {
                    val direction = if (targetState > initialState) {
                        AnimatedContentTransitionScope.SlideDirection.Left
                    } else {
                        AnimatedContentTransitionScope.SlideDirection.Right
                    }

                    slideIntoContainer(
                        towards = direction,
                        animationSpec = tween(300)
                    ) togetherWith slideOutOfContainer(
                        towards = direction,
                        animationSpec = tween(300)
                    )
                },
                modifier = Modifier.fillMaxSize()
            ) { targetIndex ->
                Box(modifier = Modifier.fillMaxSize()) {
                    tabs.first { it.index == targetIndex }.Content()
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
                            .clickable { currentIndex = tab.index }
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
}