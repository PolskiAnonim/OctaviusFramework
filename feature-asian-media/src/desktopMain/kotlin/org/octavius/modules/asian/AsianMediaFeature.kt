package org.octavius.modules.asian

import org.octavius.contract.FeatureModule
import org.octavius.contract.Tab
import org.octavius.modules.asian.ui.AsianMediaTab

object AsianMediaFeature : FeatureModule {
    override val name: String = "settings"
    override fun getTab(): Tab? = AsianMediaTab()
}