package org.octavius.modules.asian

import kotlinx.serialization.Serializable
import org.octavius.api.contract.ParsedData
import org.octavius.domain.asian.PublicationLanguage
import org.octavius.domain.asian.PublicationType

@Serializable
data class AsianPublicationData(
    override val source: String, // "MangaUpdates", "NovelUpdates" - KLUCZOWE!
    val titles: List<String>,
    val type: PublicationType,
    val language: PublicationLanguage,
    override val moduleId: String = "asian-media"
) : ParsedData