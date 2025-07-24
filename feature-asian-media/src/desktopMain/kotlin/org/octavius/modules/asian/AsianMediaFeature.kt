package org.octavius.modules.asian

import org.octavius.contract.ApiModule
import org.octavius.contract.FeatureModule
import org.octavius.contract.ScreenFactory
import org.octavius.contract.Tab
import org.octavius.modules.asian.ui.AsianMediaTab

object AsianMediaFeature : FeatureModule {
    override val name: String = "settings"
    override fun getTab(): Tab? = AsianMediaTab()
    override fun getApiModules(): List<ApiModule>? = null
    override fun getScreenFactories(): List<ScreenFactory>? = null
}