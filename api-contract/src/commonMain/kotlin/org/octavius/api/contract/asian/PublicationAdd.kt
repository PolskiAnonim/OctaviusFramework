package org.octavius.api.contract.asian

import kotlinx.serialization.Serializable
import org.octavius.domain.asian.PublicationLanguage
import org.octavius.domain.asian.PublicationType

@Serializable
data class PublicationAddRequest(
    val titles: List<String>,
    val type: PublicationType,
    val language: PublicationLanguage,
    val sourceUrl: String? = null
)

@Serializable
data class PublicationAddResponse(
    val success: Boolean,
    val newTitleId: Int? = null,
    val message: String
)