package org.octavius.api.contract.asian

import kotlinx.serialization.Serializable

@Serializable
data class PublicationCheckResponse(
    val found: Boolean,
    val titleId: Int? = null,
    val matchedTitle: String? = null
)