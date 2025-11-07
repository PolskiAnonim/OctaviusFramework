package org.octavius.database.type

import kotlinx.serialization.json.JsonObject
import org.octavius.data.annotation.PgType

@PgType(name = "dynamic_dto")
internal data class DynamicDto(
    val typeName: String,
    val dataPayload: JsonObject
)