package org.octavius.novels.form.control

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import org.octavius.novels.form.ColumnInfo
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.component.ErrorManager
import org.octavius.novels.form.component.FormSchema
import org.octavius.novels.form.component.FormState
import org.octavius.novels.form.control.validation.ControlValidator
import org.octavius.novels.form.control.validation.ValidationOptions

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
abstract class Control<T : Any>(
    val label: String?,
    val columnInfo: ColumnInfo?,
    val required: Boolean?,
    val dependencies: Map<String, ControlDependency<*>>?,
    var parentControl: String? = null,
    protected val hasStandardLayout: Boolean = true,
    val validationOptions: ValidationOptions? = null
) {
    /**
     * Validator odpowiedzialny za walidację tej kontrolki.
     * Każdy typ kontrolki ma własny validator dostosowany do typu danych.
     */
    protected abstract val validator: ControlValidator<T>

    // Referencje do komponentów formularza - ustawiane przez setupFormReferences
    protected var formState: FormState? = null
    protected var formSchema: FormSchema? = null
    protected var errorManager: ErrorManager? = null

    /**
     * Ustawia relacje hierarchiczne między kontrolkami.
     * Używane gdy kontrolka powinna zależeć od widoczności kontrolki nadrzędnej.
     */
    open fun setupParentRelationships(parentControlName: String, controls: Map<String, Control<*>>) {
        return // Domyślnie brak działania
    }

    /**
     * Ustawia referencje do komponentów formularza dla kontrolek które tego wymagają.
     * Używane przez kontrolki do dostępu do globalnego stanu, schemy i zarządzania błędami.
     */
    open fun setupFormReferences(
        formState: FormState,
        formSchema: FormSchema,
        errorManager: ErrorManager,
        controlName: String
    ) {
        this.formState = formState
        this.formSchema = formSchema
        this.errorManager = errorManager
        validator.setupFormReferences(formState, formSchema, errorManager)

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
    fun validateControl(
        controlName: String,
        state: ControlState<*>?
    ) {
        if (state == null) return
        validator.validate(controlName, state, this)
    }


    /**
     * Pobiera wynik kontrolki do zapisu w bazie danych.
     * Zwraca null jeśli kontrolka jest niewidoczna.
     */
    fun getResult(
        controlName: String,
        state: ControlState<*>
    ): Any? {
        if (!validator.isControlVisible(this, controlName)) return null
        return convertToResult(state)
    }

    /**
     * Konwertuje stan kontrolki na wynik do zapisu.
     * Może być przesłonięta dla niestandardowych konwersji.
     */
    protected open fun convertToResult(
        state: ControlState<*>
    ): Any? {
        return state.value.value
    }

    /**
     * Ustawia początkową wartość kontrolki i tworzy stan.
     *
     * @param value wartość z bazy danych lub wartość domyślna
     * @return utworzony stan kontrolki
     */
    open fun setInitValue(value: Any?): ControlState<T> {
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
     * Renderuje błędy pól z ErrorManagera.
     */
    @Composable
    protected fun DisplayFieldErrors(controlName: String) {
        errorManager?.let { em ->
            val fieldErrors = em.getFieldErrors(controlName)
            fieldErrors.forEach { error ->
                RenderFieldError(error)
            }
        }
    }

    /**
     * Renderuje kontrolkę w interfejsie użytkownika.
     * Obsługuje widoczność i animacje na podstawie zależności.
     */
    @Composable
    fun Render(
        controlName: String,
        controlState: ControlState<*>
    ) {
        val isVisible = validator.isControlVisible(this, controlName)
        val isRequired = validator.isControlRequired(this, controlName)

        AnimatedVisibility(visible = isVisible) {
            if (hasStandardLayout) {
                Column {
                    RenderNormalLabel(label, isRequired)

                    @Suppress("UNCHECKED_CAST")
                    Display(controlState as ControlState<T>, isRequired)

                    DisplayFieldErrors(controlName)
                }
            } else {
                @Suppress("UNCHECKED_CAST")
                Display(controlState as ControlState<T>, isRequired)
            }
        }
    }

    /**
     * Abstrakcyjna metoda renderowania specyficznego interfejsu kontrolki.
     * Każdy typ kontrolki implementuje własny wygląd i zachowanie.
     */
    @Composable
    protected abstract fun Display(
        controlState: ControlState<T>,
        isRequired: Boolean
    )
}

