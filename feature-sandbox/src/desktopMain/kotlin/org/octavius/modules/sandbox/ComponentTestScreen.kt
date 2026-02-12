package org.octavius.modules.sandbox

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import org.octavius.localization.Tr
import org.octavius.navigation.Screen
import org.octavius.ui.timeline.TimelineAxis
import org.octavius.ui.timeline.TimelineBlock
import org.octavius.ui.timeline.TimelineComponent
import org.octavius.ui.timeline.rememberTimelineState

class ComponentTestScreen : Screen {


    override val title: String = Tr.Sandbox.componentTestScreen()

    @Composable
    override fun Content() {
        val state = rememberTimelineState()
        val sampleBlocks = remember {
            listOf(
                TimelineBlock(2 * 3600f + 15 * 60f + 10f, 2 * 3600f + 16 * 60f + 2f, Color.Blue),
                TimelineBlock(5 * 3600f, 5 * 3600f + 30 * 60f, Color.Green),
                TimelineBlock(8 * 3600f + 45 * 60f, 9 * 3600f + 15 * 60f, Color.Red),
                TimelineBlock(14 * 3600f, 14 * 3600f + 50 * 60f, Color.Magenta),
            )
        }
        Column(Modifier.padding(16.dp).fillMaxSize()) {
            TimelineAxis(state, true, Modifier.height(30.dp))
            TimelineComponent(state, true, sampleBlocks, Modifier.weight(1f))
            Spacer(Modifier.padding(16.dp).weight(0.5f))
            TimelineComponent(state, true, sampleBlocks, Modifier.weight(1f))

        }

    }
}