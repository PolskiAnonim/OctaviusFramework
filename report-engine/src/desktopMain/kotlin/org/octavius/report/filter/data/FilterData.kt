package org.octavius.report.filter.data

import kotlinx.serialization.json.JsonObject
import org.octavius.report.FilterMode
import org.octavius.report.NullHandling

interface FilterData {
     val nullHandling: NullHandling
     val mode: FilterMode

     fun isActive(): Boolean

    // Serializacja i deserializacja danych do bazy
     fun serialize(): JsonObject

    // Kopia ze zmienionym trybem działania
    fun withMode(newMode: FilterMode): FilterData
    // Kopia ze zmienioną obsługą Nulli
    fun withNullHandling(newNullHandling: NullHandling): FilterData
}