package org.octavius.novels.form.control

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.runtime.Composable
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.validation.ControlValidator

/**
 * Abstrakcyjna klasa bazowa dla wszystkich kontrolek formularza.
 * 
 * Każda kontrolka zawiera:
 * - Label - etykietę wyświetlaną użytkownikowi
 * - ColumnInfo - powiązanie z kolumną w bazie danych
 * - Required - informację o wymagalności
 * - Dependencies - zależności od innych kontrolek (widoczność, wymagalność)
 * - ParentControl - hierarchię kontrolek (dla kontrolek zagnieżdżonych)
 * 
 * @param T typ danych przechowywanych przez kontrolkę
 */
abstract class Control<T: Any>(
    val label: String?,
    val columnInfo: ColumnInfo?,
    val required: Boolean?,
    val dependencies: Map<String, ControlDependency<*>>?,
    var parentControl: String? = null
) {
    /**
     * Validator odpowiedzialny za walidację tej kontrolki.
     * Każdy typ kontrolki ma własny validator dostosowany do typu danych.
     */
    protected abstract val validator: ControlValidator<T>

    /**
     * Ustawia relacje hierarchiczne między kontrolkami.
     * Używane gdy kontrolka powinna zależeć od widoczności kontrolki nadrzędnej.
     */
    open fun setupParentRelationships(parentControlName: String ,controls: Map<String, Control<*>>) {
        return // Domyślnie brak działania
    }

    /**
     * Aktualizuje stan kontrolki, ustawiając flagę dirty jeśli wartość się zmieniła.
     */
    fun updateState(state: ControlState<T>) {
        if (state.value.value != state.initValue.value) {
            state.dirty.value = true
        }
    }

    /**
     * Waliduje kontrolkę przy użyciu przypisanego validatora.
     */
    fun validateControl(controlName: String, state: ControlState<*>?, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>) {
        if (state == null) return
        validator.validate(controlName, state, this, controls, states)
    }


    /**
     * Pobiera wynik kontrolki do zapisu w bazie danych.
     * Zwraca null jeśli kontrolka jest niewidoczna.
     */
    fun getResult(
        state: ControlState<*>,
        controls: Map<String, Control<*>>,
        states: Map<String, ControlState<*>>
    ): Any? {
        if (!validator.isControlVisible(this, controls, states)) return null
        return convertToResult(state, controls, states)
    }

    /**
     * Konwertuje stan kontrolki na wynik do zapisu.
     * Może być przesłonięta dla niestandardowych konwersji.
     */
    protected open fun convertToResult(state: ControlState<*>, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>): Any? {
        return state.value.value
    }

    /**
     * Ustawia początkową wartość kontrolki i tworzy stan.
     * 
     * @param value wartość z bazy danych lub wartość domyślna
     * @return utworzony stan kontrolki
     */
    open fun setInitValue(value: Any?) : ControlState<T> {
        val state = ControlState<T>()
        if (value == null) {
            return state
        }

        try {
            @Suppress("UNCHECKED_CAST")
            state.initValue.value = value as T
            state.value.value = copyInitToValue(value)
        } catch (e: ClassCastException) {
            println("Nie można skonwertować wartości $value na typ kontrolki ${this::class.simpleName}")
        }
        return state
    }

    /**
     * Tworzy kopię wartości początkowej dla bieżącej wartości.
     * Wymagane dla kontrolek przechowujących złożone obiekty (listy, mapy).
     * 
     * @param value wartość do skopiowania
     * @return kopia wartości
     */
    open fun copyInitToValue(value: T): T {
        @Suppress("UNCHECKED_CAST")
        return value
    }

    /**
     * Renderuje kontrolkę w interfejsie użytkownika.
     * Obsługuje widoczność i animacje na podstawie zależności.
     */
    @Composable
    fun Render(controlState: ControlState<*>, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>) {
        val isVisible = validator.isControlVisible(this, controls, states)
        val isRequired = validator.isControlRequired(this, controls, states)
        AnimatedVisibility(visible = isVisible) {
            @Suppress("UNCHECKED_CAST")
            Display(controlState as ControlState<T>, controls, states, isRequired)
        }
    }

    /**
     * Abstrakcyjna metoda renderowania specyficznego interfejsu kontrolki.
     * Każdy typ kontrolki implementuje własny wygląd i zachowanie.
     */
    @Composable
    protected abstract fun Display(controlState: ControlState<T>, controls: Map<String, Control<*>>, states: Map<String, ControlState<*>>, isRequired: Boolean)
}

/**
 * Definuje zależność między kontrolkami.
 * 
 * @param controlName nazwa kontrolki od której zależy ta kontrolka
 * @param value wartość kontrolki która powoduje aktywację zależności
 * @param dependencyType typ zależności (widoczność lub wymagalność)
 * @param comparisonType sposób porównania wartości
 */
data class ControlDependency<T>(
    val controlName: String,
    val value: T,
    val dependencyType: DependencyType,
    val comparisonType: ComparisonType
)

/**
 * Typ zależności między kontrolkami.
 */
enum class DependencyType {
    /** Kontrolka jest widoczna/niewidoczna w zależności od wartości innej kontrolki */
    Visible,
    /** Kontrolka jest wymagana/niewymagana w zależności od wartości innej kontrolki */
    Required,
}

/**
 * Sposób porównania wartości w zależnościach.
 */
enum class ComparisonType {
    /** Wartość musi być jedną z podanych wartości */
    OneOf,
    /** Wartość musi być równa podanej wartości */
    Equals,
    /** Wartość musi być różna od podanej wartości */
    NotEquals
}