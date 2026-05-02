package org.octavius.app.settings

import org.octavius.api.contract.ApiModule
import org.octavius.app.settings.navigation.SettingsTab
import org.octavius.contract.FeatureModule
import org.octavius.contract.ScreenFactory
import org.octavius.navigation.Tab

object SettingsFeature : FeatureModule {
    override val name: String = "settings"
    override fun getTab(): Tab = SettingsTab()
    override fun getApiModules(): List<ApiModule>? = null
    override fun getScreenFactories(): List<ScreenFactory>? = null
}