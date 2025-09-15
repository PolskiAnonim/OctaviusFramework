package org.octavius.form.control.base

import kotlinx.coroutines.CoroutineScope
import org.octavius.form.component.ErrorManager
import org.octavius.form.component.FormActionTrigger
import org.octavius.form.component.FormSchema
import org.octavius.form.component.FormState

/**
 * Definiuje akcję do wykonania po zmianie wartości kontrolki.
 *
 * @param T typ wartości kontrolki wyzwalającej akcję.
 * @param action Lambda, która zostanie wykonana. Otrzymuje ActionContext jako receiver (`this`).
 */
class ControlAction<T>(
    val action: ActionContext<T>.() -> Unit
)

/**
 * Kontekst dostarczany do ControlAction, dający dostęp do stanu formularza i narzędzi.
 *
 * @param T typ wartości kontrolki, która wyzwoliła akcję.
 * @property sourceValue Nowa wartość kontrolki, która wyzwoliła akcję.
 * @property sourceRenderContext Kontekst kontrolki, która wyzwoliła akcję.
 * @property formState Dostęp do globalnego stanu formularza.
 * @property formSchema Dostęp do schemy formularza.
 * @property errorManager Dostęp do managera błędów.
 * @property coroutineScope Scope do uruchamiania operacji asynchronicznych (np. API).
 */
data class ActionContext<T>(
    val sourceValue: T?,
    val sourceRenderContext: RenderContext,
    val formState: FormState,
    val formSchema: FormSchema,
    val errorManager: ErrorManager,
    val trigger: FormActionTrigger,
    val coroutineScope: CoroutineScope,
    val payload: Any? = null // Dodatkowe dane, przykładowo dla kontrolek dropdown dodatkowa wartość
) {
    /**
     * Aktualizuje wartość kontrolki o podanej nazwie.
     * Działa na kontrolkach globalnych.
     */
    fun <V: Any> updateControl(controlName: String, newValue: V?) {
        formState.getControlState(controlName)?.let { state ->
            // Używamy "unsafe" cast, ponieważ programista jest odpowiedzialny za poprawny typ
            @Suppress("UNCHECKED_CAST")
            val typedState = state as ControlState<V>
            typedState.value.value = newValue
            typedState.revision.value++ // ZAWSZE inkrementuj rewizję
        }
    }

    /**
     * Aktualizuje wartość kontrolki w tym samym wierszu (w obrębie RepeatableControl).
     * Jeśli kontrolka nie jest w RepeatableControl, działa jak updateControl.
     */
    fun <V: Any> updateLocalControl(controlName: String, newValue: V?) {
        val path = sourceRenderContext.basePath
        val resolvedName ="$path.$controlName"
        updateControl(resolvedName, newValue)
    }

}