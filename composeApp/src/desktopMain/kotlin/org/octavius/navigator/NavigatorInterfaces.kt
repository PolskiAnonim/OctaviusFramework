package org.octavius.navigator

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.painter.Painter

val LocalNavigator = compositionLocalOf<Navigator> { error("No Navigator found!") }

//Screen for Navigator
interface Screen {
    @Composable
    fun Content(paddingValues: PaddingValues)
}

//Tabs for TabNavigator
data class TabOptions(
    val title: String,
    val icon: Painter? = null
)

interface Tab {
    val options: TabOptions
        @Composable get
    val index: UShort

    @Composable
    fun Content()
}