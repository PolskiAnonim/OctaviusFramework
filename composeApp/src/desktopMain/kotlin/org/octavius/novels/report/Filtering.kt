package org.octavius.novels.report

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf

interface FilterInterface<T> {
    fun reset()
    fun isActive(): Boolean
}

sealed class FilterValue<T> : FilterInterface<T> {
    abstract val nullHandling: MutableState<NullHandling>

    data class BooleanFilter(
        val value: MutableState<Boolean?> = mutableStateOf(null),
        override val nullHandling: MutableState<NullHandling> = mutableStateOf(NullHandling.Ignore)
    ) : FilterValue<Boolean>() {
        override fun reset() {
            value.value = null
            nullHandling.value = NullHandling.Ignore
        }

        override fun isActive(): Boolean {
            return value.value != null || nullHandling.value != NullHandling.Ignore
        }
    }

    data class NumberFilter<T : Comparable<T>>(
        val filterType: MutableState<NumberFilterType> = mutableStateOf(NumberFilterType.Equals),
        val minValue: MutableState<T?> = mutableStateOf(null),
        val maxValue: MutableState<T?> = mutableStateOf(null),
        override val nullHandling: MutableState<NullHandling> = mutableStateOf(NullHandling.Ignore)
    ) : FilterValue<T>() {
        override fun reset() {
            minValue.value = null
            maxValue.value = null
            filterType.value = NumberFilterType.Equals
            nullHandling.value = NullHandling.Ignore
        }

        override fun isActive(): Boolean {
            return minValue.value != null || maxValue.value != null || nullHandling.value != NullHandling.Ignore
        }
    }

    data class TextFilter(
        val filterType: MutableState<TextFilterType> = mutableStateOf(TextFilterType.Contains),
        val value: MutableState<String> = mutableStateOf(""),
        val caseSensitive: MutableState<Boolean> = mutableStateOf(false),
        override val nullHandling: MutableState<NullHandling> = mutableStateOf(NullHandling.Ignore)
    ) : FilterValue<String>() {
        override fun reset() {
            value.value = ""
            filterType.value = TextFilterType.Contains
            caseSensitive.value = false
            nullHandling.value = NullHandling.Ignore
        }

        override fun isActive(): Boolean {
            return value.value.isNotEmpty() || nullHandling.value != NullHandling.Ignore
        }
    }

    data class EnumFilter<E : Enum<*>>(
        private val _values: MutableState<List<E>> = mutableStateOf(emptyList()),
        val include: MutableState<Boolean> = mutableStateOf(true),
        override val nullHandling: MutableState<NullHandling> = mutableStateOf(NullHandling.Ignore)
    ) : FilterValue<E>() {
        // Właściwość tylko do odczytu dla widoku
        val values: State<List<E>> get() = _values

        // Metody do modyfikacji listy
        fun addValue(value: Any) {
            @Suppress("UNCHECKED_CAST")
            _values.value += value as E
        }

        fun removeValue(value: Any) {
            @Suppress("UNCHECKED_CAST")
            _values.value = _values.value.filter { it != value as E }
        }

        fun containsValue(value: Any): Boolean {
            @Suppress("UNCHECKED_CAST")
            return _values.value.contains(value as E)
        }

        override fun reset() {
            _values.value = emptyList()
            include.value = true
            nullHandling.value = NullHandling.Ignore
        }

        override fun isActive(): Boolean {
            return _values.value.isNotEmpty() || nullHandling.value != NullHandling.Ignore
        }
    }
}

enum class NumberFilterType {
    Equals,      // ==
    NotEquals,   // !=
    LessThan,    //
    LessEquals,  // <=
    GreaterThan, // >
    GreaterEquals, // >=
    Range        // between min and max
}

enum class TextFilterType {
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