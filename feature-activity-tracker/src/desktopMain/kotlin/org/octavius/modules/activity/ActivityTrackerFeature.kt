package org.octavius.modules.activity

import org.octavius.contract.FeatureModule
import org.octavius.contract.ScreenFactory
import org.octavius.modules.activity.navigation.ActivityTrackerTab
import org.octavius.navigation.Tab

object ActivityTrackerFeature : FeatureModule {
    override val name: String = "activity-tracker"
    override fun getTab(): Tab = ActivityTrackerTab()
    override fun getApiModules() = null
    override fun getScreenFactories(): List<ScreenFactory>? = null
}
