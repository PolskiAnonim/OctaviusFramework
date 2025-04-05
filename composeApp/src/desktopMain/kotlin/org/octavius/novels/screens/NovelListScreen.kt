package org.octavius.novels.screens

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.octavius.novels.component.VisibleList
import org.octavius.novels.domain.Novel
import org.octavius.novels.navigator.LocalNavigator
import org.octavius.novels.navigator.Screen

object NovelListScreen : Screen {
    private val visibleList = VisibleList(Novel::class)

    @Composable
    override fun Content(paddingValues: PaddingValues) {
        LocalNavigator.current
        visibleList.Service("novels","titles","array")

        Scaffold(
            modifier = Modifier.padding(paddingValues),
            topBar = { visibleList.TopBar() },
            bottomBar = { visibleList.BottomBar() }
        ) { innerPaddingValues ->
            visibleList.List(innerPaddingValues, "titles")
        }
    }
}