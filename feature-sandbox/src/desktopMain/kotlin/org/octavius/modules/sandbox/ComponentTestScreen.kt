package org.octavius.modules.sandbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.localization.Tr
import org.octavius.navigation.Screen
import org.octavius.ui.timeline.TimelineAxis
import org.octavius.ui.timeline.TimelineComponent
import org.octavius.ui.timeline.rememberTimelineState

class ComponentTestScreen : Screen {


    override val title: String = Tr.Sandbox.componentTestScreen()

    @Composable
    override fun Content() {
        val state = rememberTimelineState()
        Column(Modifier.padding(16.dp).fillMaxSize()) {
            TimelineAxis(state, Modifier.height(30.dp))
            TimelineComponent(state)
        }

    }
}