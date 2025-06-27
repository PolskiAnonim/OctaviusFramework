package org.octavius.navigator

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.graphics.painter.Painter
import org.octavius.localization.Translations

val LocalNavigator = compositionLocalOf<Navigator> { error(Translations.get("navigation.noNavigator")) }

//Screen for Navigator
interface Screen {
    @Composable
    fun Content()
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