package org.octavius.report.filter.data

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import kotlinx.serialization.json.JsonObject
import org.octavius.report.FilterMode
import org.octavius.report.NullHandling

abstract class FilterData {
    val nullHandling: MutableState<NullHandling> = mutableStateOf(NullHandling.Ignore)
    val mode: MutableState<FilterMode> = mutableStateOf(FilterMode.Single)

    // Zwraca wszystkie State'y które powinny być śledzone dla reaktywności
    abstract fun getTrackableStates(): List<Any?>

    fun resetFilterData() {
        nullHandling.value = NullHandling.Ignore
        mode.value = if (mode.value == FilterMode.ListAll) FilterMode.ListAny else mode.value
        resetValue()
    }

    protected abstract fun resetValue()

    abstract fun isActive(): Boolean

    // Serializacja i deserializacja danych do bazy
    abstract fun serialize(): JsonObject
    abstract fun deserialize(data: JsonObject)
}