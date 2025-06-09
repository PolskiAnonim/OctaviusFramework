package org.octavius.novels.report

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

sealed class FilterData<T> {
    abstract val nullHandling: MutableState<NullHandling>

    abstract fun reset()
    abstract fun isActive(): Boolean
    
    val dirtyState: MutableState<Boolean> = mutableStateOf(false)

    // Funkcja pomocnicza do oznaczenia filtru jako zmieniony
    fun markDirty() {
        dirtyState.value = !dirtyState.value
    }

    data class BooleanData(
        val value: MutableState<Boolean?> = mutableStateOf(null),
        override val nullHandling: MutableState<NullHandling> = mutableStateOf(NullHandling.Ignore)
    ) : FilterData<Boolean>() {
        override fun reset() {
            value.value = null
            nullHandling.value = NullHandling.Ignore
        }

        override fun isActive(): Boolean {
            return value.value != null || nullHandling.value != NullHandling.Ignore
        }
    }

    data class NumberData<T : Number>(
        val filterType: MutableState<NumberFilterDataType> = mutableStateOf(NumberFilterDataType.Equals),
        val minValue: MutableState<T?> = mutableStateOf(null),
        val maxValue: MutableState<T?> = mutableStateOf(null),
        override val nullHandling: MutableState<NullHandling> = mutableStateOf(NullHandling.Ignore)
    ) : FilterData<T>() {
        override fun reset() {
            minValue.value = null
            maxValue.value = null
            filterType.value = NumberFilterDataType.Equals
            nullHandling.value = NullHandling.Ignore
            markDirty()
        }

        override fun isActive(): Boolean {
            return minValue.value != null || maxValue.value != null || nullHandling.value != NullHandling.Ignore
        }
    }

    data class StringData(
        val filterType: MutableState<StringFilterDataType> = mutableStateOf(StringFilterDataType.Contains),
        val value: MutableState<String> = mutableStateOf(""),
        val caseSensitive: MutableState<Boolean> = mutableStateOf(false),
        override val nullHandling: MutableState<NullHandling> = mutableStateOf(NullHandling.Ignore)
    ) : FilterData<String>() {
        override fun reset() {
            value.value = ""
            filterType.value = StringFilterDataType.Contains
            caseSensitive.value = false
            nullHandling.value = NullHandling.Ignore
            markDirty()
        }

        override fun isActive(): Boolean {
            return value.value.isNotEmpty() || nullHandling.value != NullHandling.Ignore
        }
    }

    data class EnumData<E : Enum<*>>(
        private val _values: MutableState<List<E>> = mutableStateOf(emptyList()),
        val include: MutableState<Boolean> = mutableStateOf(true),
        override val nullHandling: MutableState<NullHandling> = mutableStateOf(NullHandling.Ignore)
    ) : FilterData<E>() {
        // Właściwość tylko do odczytu dla widoku
        val values: State<List<E>> get() = _values

        // Metody do modyfikacji listy
        fun addValue(value: Any) {
            @Suppress("UNCHECKED_CAST")
            _values.value += value as E
            markDirty()
        }

        fun removeValue(value: Any) {
            @Suppress("UNCHECKED_CAST")
            _values.value = _values.value.filter { it != value as E }
            markDirty()
        }

        fun containsValue(value: Any): Boolean {
            @Suppress("UNCHECKED_CAST")
            return _values.value.contains(value as E)
        }

        override fun reset() {
            _values.value = emptyList()
            include.value = true
            nullHandling.value = NullHandling.Ignore
            markDirty()
        }

        override fun isActive(): Boolean {
            return _values.value.isNotEmpty() || nullHandling.value != NullHandling.Ignore
        }
    }
}

enum class NumberFilterDataType {
    Equals,      // ==
    NotEquals,   // !=
    LessThan,    //
    LessEquals,  // <=
    GreaterThan, // >
    GreaterEquals, // >=
    Range        // between min and max
}

enum class StringFilterDataType {
    Exact,       // dokładne dopasowanie
    StartsWith,  // od początku
    EndsWith,    // od końca
    Contains,    // dowolny fragment
    NotContains  // nie zawiera
}

enum class NullHandling {
    Ignore,      // Ignoruj wartości null
    Include,     // Dołącz wartości null - dla pustej wartości - tylko nulle
    Exclude,     // Wyklucz wartości null
}