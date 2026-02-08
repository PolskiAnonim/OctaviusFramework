package org.octavius.modules.sandbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import org.octavius.localization.Tr
import org.octavius.navigation.Screen
import org.octavius.ui.timeline.TimelineComponent
import org.octavius.ui.timeline.rememberTimelineState

class ComponentTestScreen : Screen {


    override val title: String = Tr.Sandbox.componentTestScreen()

    @Composable
    override fun Content() {
        val state = rememberTimelineState()
        Column(Modifier.padding(16.dp).fillMaxSize()) {
            TimelineComponent(state, Modifier.weight(1f))
            Spacer(Modifier.padding(16.dp).weight(0.5f))
            TimelineComponent(state, Modifier.weight(1f))
        }

    }
}