package org.octavius.feature.books

import org.octavius.api.contract.ApiModule
import org.octavius.contract.FeatureModule
import org.octavius.contract.ScreenFactory
import org.octavius.navigation.Tab

object BooksFeature : FeatureModule {

    override val name: String = "books"
    override fun getTab(): Tab? = null
    override fun getApiModules(): List<ApiModule>? = null
    override fun getScreenFactories(): List<ScreenFactory>? = null
}
