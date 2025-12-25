package org.octavius.modules.asian.model

import kotlinx.serialization.Serializable

@Serializable
data class PublicationCheckResponse(
    val found: Boolean,
    val titleId: Int? = null,
    val matchedTitle: String? = null
)

@Serializable
data class PublicationCheckRequest(
    val titles: List<String>
)