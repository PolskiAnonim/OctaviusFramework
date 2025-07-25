package org.octavius.modules.settings

import org.octavius.contract.FeatureModule
import org.octavius.contract.Tab
import org.octavius.modules.settings.ui.SettingsTab

object SettingsFeature : FeatureModule {
    override val name: String = "settings"
    override fun getTab(): Tab? = SettingsTab()
}