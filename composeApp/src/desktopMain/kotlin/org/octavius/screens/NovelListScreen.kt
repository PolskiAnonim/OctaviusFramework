package org.octavius.screens

import androidx.compose.runtime.Composable
import org.octavius.novels.component.VisibleList
import org.octavius.novels.domain.Novel
import org.octavius.novels.navigator.Screen

object NovelListScreen : Screen {
    private val visibleList = VisibleList(Novel::class)

    @Composable
    override fun Content() {
        visibleList.display()
    }

}