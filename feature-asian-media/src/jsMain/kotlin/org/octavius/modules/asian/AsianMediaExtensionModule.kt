package org.octavius.modules.asian

import kotlinx.serialization.json.Json
import org.octavius.api.contract.ExtensionModule
import org.octavius.api.contract.ParsedData
import org.octavius.modules.asian.model.AsianPublicationData
import org.octavius.modules.asian.screen.MangaUpdatesAddScreen
import org.octavius.navigation.Screen

object AsianMediaExtensionModule : ExtensionModule {
    override val id: String = "asian-media"

    override fun createScreenFromJson(jsonData: String): Screen? {
        return try {
            val data = Json.decodeFromString<AsianPublicationData>(jsonData)
            chooseScreen(data)
        } catch (e: Exception) {
            println("Błąd deserializacji danych dla modułu $id: ${e.message}")
            null
        }
    }

    fun chooseScreen(publicationData: AsianPublicationData): Screen {
        return when (publicationData.source) {
            "MangaUpdates" -> MangaUpdatesAddScreen(publicationData)
            else -> TODO("Not implemented yet")
        }
    }

}