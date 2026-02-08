package org.octavius.modules.sandbox

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.localization.Tr
import org.octavius.navigation.Screen
import org.octavius.ui.timeline.TimelineComponent

class ComponentTestScreen : Screen {


    override val title: String = Tr.Sandbox.componentTestScreen()

    @Composable
    override fun Content() {
        Box(Modifier.padding(16.dp)) {
            TimelineComponent()
        }

    }
}