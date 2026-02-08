package org.octavius.modules.sandbox

import org.octavius.contract.FeatureModule
import org.octavius.contract.ScreenFactory
import org.octavius.modules.sandbox.navigation.SandboxTab
import org.octavius.navigation.Tab

object SandboxFeature : FeatureModule {
    override val name: String = "sandbox"
    override fun getTab(): Tab = SandboxTab()
    override fun getApiModules() = null
    override fun getScreenFactories(): List<ScreenFactory>? = null
}
