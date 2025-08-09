package org.octavius.modules.settings

import org.octavius.api.contract.ApiModule
import org.octavius.contract.FeatureModule
import org.octavius.contract.ScreenFactory
import org.octavius.contract.Tab
import org.octavius.modules.settings.navigation.SettingsTab

object SettingsFeature : FeatureModule {
    override val name: String = "settings"
    override fun getTab(): Tab? = SettingsTab()
    override fun getApiModules(): List<ApiModule>? = null
    override fun getScreenFactories(): List<ScreenFactory>? = null
}