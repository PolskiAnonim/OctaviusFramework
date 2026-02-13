package org.octavius.modules.sandbox

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import org.octavius.localization.Tr
import org.octavius.navigation.Screen
import org.octavius.ui.timeline.TimelineBlock
import org.octavius.ui.timeline.TimelineComponent
import org.octavius.ui.timeline.TimelineLane
import org.octavius.ui.timeline.rememberTimelineState

class ComponentTestScreen : Screen {

    override val title: String = Tr.Sandbox.componentTestScreen()

    @Composable
    override fun Content() {
        val state = rememberTimelineState()
        val lanes = remember {
            listOf(
                TimelineLane("Kategorie", listOf(
                    TimelineBlock(2 * 3600f + 15 * 60f, 2 * 3600f + 45 * 60f, Color.Blue),
                    TimelineBlock(5 * 3600f, 5 * 3600f + 30 * 60f, Color.Cyan),
                    TimelineBlock(14 * 3600f, 14 * 3600f + 50 * 60f, Color.Blue),
                )),
                TimelineLane("Aplikacje", listOf(
                    TimelineBlock(3 * 3600f, 3 * 3600f + 20 * 60f, Color.Green),
                    TimelineBlock(8 * 3600f + 45 * 60f, 9 * 3600f + 15 * 60f, Color.Yellow),
                    TimelineBlock(12 * 3600f, 12 * 3600f + 40 * 60f, Color.Green),
                )),
                TimelineLane("Dokumenty", listOf(
                    TimelineBlock(1 * 3600f, 1 * 3600f + 10 * 60f, Color.Red),
                    TimelineBlock(10 * 3600f, 10 * 3600f + 25 * 60f, Color.Magenta),
                    TimelineBlock(16 * 3600f + 30 * 60f, 17 * 3600f, Color.Red),
                )),
            )
        }
        TimelineComponent(state, true, lanes, Modifier.padding(16.dp).fillMaxSize())
    }
}
