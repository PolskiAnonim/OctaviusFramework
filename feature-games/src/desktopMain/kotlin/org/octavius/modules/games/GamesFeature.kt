package org.octavius.modules.games

import org.octavius.contract.FeatureModule
import org.octavius.contract.ScreenFactory
import org.octavius.modules.games.navigation.GameTab
import org.octavius.navigation.Tab

object GamesFeature : FeatureModule {
    override val name: String = "games"
    override fun getTab(): Tab = GameTab()
    override fun getApiModules() = null
    override fun getScreenFactories(): List<ScreenFactory>? = null
}