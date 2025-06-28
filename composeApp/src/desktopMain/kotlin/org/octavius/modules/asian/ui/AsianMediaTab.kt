package org.octavius.modules.asian.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import novelskotlin.composeapp.generated.resources.Res
import novelskotlin.composeapp.generated.resources.novel_icon
import org.jetbrains.compose.resources.painterResource
import org.octavius.localization.Translations
import org.octavius.navigator.Navigator
import org.octavius.navigator.Tab
import org.octavius.navigator.TabOptions

class AsianMediaTab : Tab {
    private val navigator = Navigator()

    init {
        navigator.addScreen(
            AsianMediaReportScreen(
                navigator
            )
        )
    }

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            title = Translations.get("tabs.asianMedia"),
            icon = painterResource(Res.drawable.novel_icon)
        )

    override val index: UShort = 0u

    @Composable
    override fun Content() {
        Box(Modifier.fillMaxSize()) {
            navigator.Display()
        }
    }
}