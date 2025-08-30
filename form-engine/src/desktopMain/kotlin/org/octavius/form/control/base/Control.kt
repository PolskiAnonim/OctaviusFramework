package org.octavius.form.control.base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import org.octavius.data.contract.ColumnInfo
import org.octavius.form.component.FormActionTrigger
import org.octavius.form.component.ErrorManager
import org.octavius.form.component.FormSchema
import org.octavius.form.component.FormState
import org.octavius.form.control.layout.RenderFieldError
import org.octavius.form.control.layout.RenderNormalLabel
import org.octavius.form.control.validator.DefaultValidator

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
    val validationOptions: ValidationOptions? = null,
    val actions: List<ControlAction<T>>? = null
) {

    /**
     * Wykonuje zdefiniowane akcje dla tej kontrolki.
     * Wywoływane po zmianie wartości.
     */
    protected fun executeActions(
        controlName: String,
        newValue: T?,
        scope: CoroutineScope,
        payload: Any? = null
    ) {
        actions?.forEach { action ->
            val context = ActionContext(
                sourceValue = newValue,
                sourceControlName = controlName,
                formState = formState,
                formSchema = formSchema,
                errorManager = errorManager,
                trigger = formActionTrigger,
                coroutineScope = scope,
                payload = payload
            )
            action.action.invoke(context)
        }
    }
    /**
     * Validator odpowiedzialny za walidację tej kontrolki.
     * Każdy typ kontrolki ma własny validator dostosowany do typu danych.
     */
    protected open val validator: ControlValidator<T> = DefaultValidator()

    // Referencje do komponentów formularza - ustawiane przez setupFormReferences
    protected lateinit var formState: FormState
    protected lateinit var formSchema: FormSchema
    protected lateinit var errorManager: ErrorManager
    protected lateinit var formActionTrigger: FormActionTrigger
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
        controlName: String,
        formActionTrigger: FormActionTrigger
    ) {
        this.formState = formState
        this.formSchema = formSchema
        this.errorManager = errorManager
        this.formActionTrigger = formActionTrigger
        validator.setupFormReferences(formState, formSchema, errorManager)
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
     * Pobiera wynik kontrolki (wartość bieżącą i początkową) do dalszego przetwarzania.
     * Jeśli kontrolka jest niewidoczna, jej currentValue jest ustawiane na null,
     * ale initialValue jest zachowywane, aby dać kontekst do FormDataManager.
     */
    fun getResult(
        controlName: String,
        state: ControlState<*>
    ): ControlResultData {
        val result = convertToResult(state)
        // Jeśli kontrolka jest niewidoczna, zerujemy jej bieżącą wartość, ale zachowujemy początkową.
        return if (!validator.isControlVisible(this, controlName)) {
            result.copy(currentValue = null)
        } else {
            result
        }
    }

    /**
     * Konwertuje stan kontrolki na wynik do zapisu.
     * Może być przesłonięta dla niestandardowych konwersji.
     */
    protected open fun convertToResult(state: ControlState<*>): ControlResultData {
        return ControlResultData(currentValue = state.value.value, initialValue = state.initValue.value)
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
        val fieldErrors = errorManager.getFieldErrors(controlName)

        val formatError = errorManager.getFormatError(controlName)

        formatError?.let { error -> RenderFieldError(error) }

        fieldErrors.forEach { error ->
            RenderFieldError(error)
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
            @Suppress("UNCHECKED_CAST")
            val typedState = controlState as ControlState<T>
            if (hasStandardLayout) {
                Column {
                    RenderNormalLabel(label, isRequired)
                    Display(controlName, typedState, isRequired)

                    DisplayFieldErrors(controlName)
                }
            } else {
                Display(controlName, typedState, isRequired)
            }
        }
    }

    /**
     * Abstrakcyjna metoda renderowania specyficznego interfejsu kontrolki.
     * Każdy typ kontrolki implementuje własny wygląd i zachowanie.
     */
    @Composable
    protected abstract fun Display(
        controlName: String,
        controlState: ControlState<T>,
        isRequired: Boolean
    )
}