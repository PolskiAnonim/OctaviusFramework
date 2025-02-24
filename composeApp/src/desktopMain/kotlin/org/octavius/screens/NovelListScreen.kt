package org.octavius.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.novels.component.VisibleList
import org.octavius.novels.domain.Novel
import org.octavius.novels.navigator.Screen

object NovelListScreen : Screen {
    private val visibleList = VisibleList(Novel::class)

    @Composable
    override fun Content() {
        visibleList.Service("novels","titles","array")

        Scaffold(
            topBar = { visibleList.TopBar() },
            bottomBar = { visibleList.BottomBar() }
        ) { paddingValues ->
            visibleList.List(paddingValues, "titles")
        }
    }

}