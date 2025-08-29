package org.octavius.modules.asian

import org.octavius.api.contract.ApiModule
import org.octavius.contract.FeatureModule
import org.octavius.contract.ScreenFactory
import org.octavius.modules.asian.api.AsianMediaApi
import org.octavius.modules.asian.navigation.AsianMediaTab
import org.octavius.navigation.Tab


object AsianMediaFeature : FeatureModule {
    override val name: String = "settings"
    override fun getTab(): Tab? = AsianMediaTab()
    override fun getApiModules(): List<ApiModule>? = listOf(AsianMediaApi())
    override fun getScreenFactories(): List<ScreenFactory>? = null
}
