package org.octavius.modules.games

import org.octavius.contract.FeatureModule
import org.octavius.contract.Tab
import org.octavius.modules.games.ui.GameTab

object GamesFeature : FeatureModule {
    override val name: String = "games"
    override fun getTab(): Tab? = GameTab()
}