package org.octavius.modules.asian

import kotlinx.serialization.json.Json
import org.octavius.api.contract.ExtensionModule
import org.octavius.api.contract.ParsedData
import org.octavius.navigation.Screen

object AsianMediaExtensionModule: ExtensionModule {
    override val id: String = "asian-media"

    override fun deserializeData(jsonString: String): ParsedData? {
        return try {
            // Moduł próbuje zdeserializować dane do JEDYNEGO typu, który go interesuje.
            Json.decodeFromString<AsianPublicationData>(jsonString)
        } catch (e: Exception) {
            // Jeśli się nie uda, to znaczy, że to nie są dane dla niego.
            null
        }
    }

    override fun createScreenFromData(data: ParsedData): Screen {
        val publicationData = data as AsianPublicationData
        return when (publicationData.source) {
            "MangaUpdates" -> MangaUpdatesAddScreen(publicationData)
            else -> TODO("Not implemented yet")
        }
    }
}