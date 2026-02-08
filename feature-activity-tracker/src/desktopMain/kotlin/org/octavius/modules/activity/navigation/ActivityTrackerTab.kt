package org.octavius.modules.activity.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Timeline
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import org.octavius.localization.Tr
import org.octavius.navigation.Screen
import org.octavius.navigation.Tab
import org.octavius.navigation.TabOptions

class ActivityTrackerTab : Tab {
    override val id = "activity-tracker"

    override val options: TabOptions
        @Composable
        get() = TabOptions(
            title = Tr.Tabs.activityTracker(),
            icon = rememberVectorPainter(Icons.Filled.Timeline)
        )

    override fun getInitialScreen(): Screen = ActivityTrackerHomeScreen.create()
}
