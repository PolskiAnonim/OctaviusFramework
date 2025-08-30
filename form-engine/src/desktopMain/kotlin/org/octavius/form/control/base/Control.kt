package org.octavius.form.control.base

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import kotlinx.coroutines.CoroutineScope
import org.octavius.data.contract.ColumnInfo
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.component.ErrorManager
import org.octavius.form.component.FormActionTrigger
import org.octavius.form.component.FormSchema
import org.octavius.form.component.FormState
import org.octavius.form.control.layout.RenderFieldError
import org.octavius.form.control.layout.RenderNormalLabel
import org.octavius.form.control.validator.DefaultValidator

/**
 * Abstrakcyjna klasa bazowa dla wszystkich kontrolek formularza.
 *
 * @param T typ danych przechowywanych przez kontrolkę
 */
abstract class Control<T : Any> internal constructor(
    // --- 1. Konfiguracja i Właściwości ---
    internal val label: String?,
    internal val columnInfo: ColumnInfo?,
    internal val required: Boolean?,
    internal val dependencies: Map<String, ControlDependency<*>>?,
    internal val validationOptions: ValidationOptions? = null,
    private val actions: List<ControlAction<T>>? = null,
    protected val hasStandardLayout: Boolean = true
) {
    // --- 2. Cykl Życia i Kontekst Formularza ---
    // Referencje wstrzykiwane przez FormHandler przy inicjalizacji.
    protected lateinit var formState: FormState
    protected lateinit var formSchema: FormSchema
    protected lateinit var errorManager: ErrorManager
    protected lateinit var formActionTrigger: FormActionTrigger

    /**
     * Ustawia referencje do komponentów formularza. Jest to drugi etap inicjalizacji kontrolki,
     * wywoływany przez FormHandler. Daje dostęp do globalnego stanu, schemy i managera błędów.
     */
    internal open fun setupFormReferences(
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

    // --- 3. Zarządzanie Hierarchią ---
    internal var parentControl: String? = null

    /**
     * Ustawia relacje hierarchiczne między kontrolkami. Używane przez kontrolki-kontenery
     * (np. SectionControl), aby poinformować kontrolki-dzieci o swoim istnieniu.
     */
    internal open fun setupParentRelationships(parentControlName: String, controls: Map<String, Control<*>>) {
        // Domyślnie puste, implementują to kontenery.
    }

    // --- 4. Zarządzanie Stanem i Danymi ---
    /**
     * Ustawia początkową wartość kontrolki i tworzy jej stan (`ControlState`).
     * @param value Wartość z bazy danych lub wartość domyślna.
     * @return Utworzony stan kontrolki.
     */
    internal open fun setInitValue(value: Any?): ControlState<T> {
        val state = ControlState<T>()
        if (value == null) {
            return state
        }

        try {
            @Suppress("UNCHECKED_CAST")
            state.initValue.value = value as T
            state.value.value = copyInitToValue(value)
        } catch (e: ClassCastException) {
            GlobalDialogManager.show(
                ErrorDialogConfig(
                    "Błąd", //TODO tłumaczenie
                    "Nie można skonwertować wartości '$value' na typ kontrolki ${this::class.simpleName}"
                )
            )
        }
        return state
    }

    /**
     * Tworzy głęboką kopię wartości początkowej dla bieżącej wartości.
     * Kluczowe dla typów mutowalnych (np. List), aby zmiany w `value` nie wpływały na `initValue`.
     */
    internal open fun copyInitToValue(value: T): T {
        return value // Domyślna implementacja dla typów niemutowalnych.
    }

    /**
     * Pobiera wynik kontrolki (wartość bieżącą i początkową) do dalszego przetwarzania.
     * Jeśli kontrolka jest niewidoczna, jej `currentValue` jest ustawiane na null.
     */
    internal fun getResult(controlName: String, state: ControlState<*>): ControlResultData {
        val result = convertToResult(state)
        // Niewidoczne kontrolki nie powinny przekazywać swojej wartości do zapisu.
        return if (!validator.isControlVisible(this, controlName)) {
            result.copy(currentValue = null)
        } else {
            result
        }
    }

    /**
     * Konwertuje wewnętrzny stan kontrolki (`ControlState`) na wynik do zapisu (`ControlResultData`).
     * Może być przesłonięta dla niestandardowych konwersji (np. w `RepeatableControl`).
     */
    protected open fun convertToResult(state: ControlState<*>): ControlResultData {
        return ControlResultData(currentValue = state.value.value, initialValue = state.initValue.value)
    }

    // --- 5. Logika Walidacji ---
    /**
     * Validator odpowiedzialny za walidację tej kontrolki.
     * Każdy typ kontrolki dostarcza własną implementację (np. `StringValidator`).
     * Domyślnie pusta implementacja
     */
    protected open val validator: ControlValidator<T> = DefaultValidator()

    /**
     * Uruchamia proces walidacji dla tej kontrolki przy użyciu przypisanego walidatora.
     */
    internal fun validateControl(controlName: String, state: ControlState<*>) {
        validator.validate(controlName, state, this)
    }

    // --- 6. Obsługa Akcji ---
    /**
     * Wykonuje zdefiniowane akcje dla tej kontrolki, zazwyczaj po zmianie wartości.
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

    // --- 7. Renderowanie UI ---
    /**
     * Renderuje kontrolkę w interfejsie użytkownika.
     * Ta metoda jest publicznym API dla `FormScreen`. Obsługuje logikę widoczności.
     */
    @Composable
    internal fun Render(controlName: String, controlState: ControlState<*>) {
        val isVisible = validator.isControlVisible(this, controlName)
        val isRequired = validator.isControlRequired(this, controlName)

        AnimatedVisibility(visible = isVisible) {
            @Suppress("UNCHECKED_CAST")
            val typedState = controlState as ControlState<T>
            if (hasStandardLayout) {
                // Standardowy układ: Etykieta nad kontrolką, błędy pod.
                Column {
                    RenderNormalLabel(label, isRequired)
                    Display(controlName, typedState, isRequired)
                    DisplayFieldErrors(controlName)
                }
            } else {
                // Układ niestandardowy: Kontrolka sama zarządza swoim layoutem.
                Display(controlName, typedState, isRequired)
            }
        }
    }

    /**
     * Abstrakcyjna metoda renderowania specyficznego interfejsu kontrolki.
     * Każdy konkretny typ kontrolki (np. `StringControl`) musi ją zaimplementować.
     */
    @Composable
    protected abstract fun Display(
        controlName: String,
        controlState: ControlState<T>,
        isRequired: Boolean
    )

    /**
     * Pomocnicza funkcja renderująca błędy walidacji dla tej kontrolki.
     */
    @Composable
    protected fun DisplayFieldErrors(controlName: String) {
        val formatError = errorManager.getFormatError(controlName)
        formatError?.let { error -> RenderFieldError(error) }

        val fieldErrors = errorManager.getFieldErrors(controlName)
        fieldErrors.forEach { error ->
            RenderFieldError(error)
        }
    }
}