package org.octavius.novels.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import novelskotlin.composeapp.generated.resources.Res
import novelskotlin.composeapp.generated.resources.novel_icon
import org.octavius.novels.domain.novel.NovelReport
import org.octavius.novels.navigator.Navigator
import org.octavius.novels.navigator.Tab
import org.octavius.novels.navigator.TabOptions

class NovelTab : Tab {
    private val navigator = Navigator()

    init {
        navigator.addScreen(
            NovelReport(
                navigator
            )
        )
    }

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            title = "Nowelki",
            icon = org.jetbrains.compose.resources.painterResource(Res.drawable.novel_icon)
        )

    override val index: UShort = 0u

    @Composable
    override fun Content() {
        Box(Modifier.fillMaxSize()) {
            navigator.Display()
        }
    }
}