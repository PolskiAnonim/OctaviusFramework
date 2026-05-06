package org.octavius.modules.finances

import org.octavius.api.contract.ApiModule
import org.octavius.contract.FeatureModule
import org.octavius.modules.finances.navigation.FinancesTab
import org.octavius.navigation.Tab

object FinancesFeature : FeatureModule {

    override val name: String = "finances"
    override fun getTab(): Tab = FinancesTab()
    override fun getApiModules(): List<ApiModule> = emptyList()
    override fun getScreenFactories() = null
}
