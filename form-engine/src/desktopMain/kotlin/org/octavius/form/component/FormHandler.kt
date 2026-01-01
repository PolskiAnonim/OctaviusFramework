package org.octavius.form.component

import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.control.base.Control
import org.octavius.form.control.base.ControlState
import org.octavius.localization.T
import org.octavius.navigation.AppRouter
import org.octavius.ui.snackbar.SnackbarManager

/**
 * Klasa obsługująca cykl życia formularza - główny koordynator systemu formularzy.
 *
 * FormHandler koordynuje pracę wszystkich komponentów formularza:
 * - **FormSchema** - definicja struktury i kontrolek formularza
 * - **FormState** - reaktywne zarządzanie stanem wszystkich kontrolek
 * - **FormDataManager** - ładowanie danych z bazy i przetwarzanie wyników
 * - **FormValidator** - walidacja pól i reguł biznesowych
 *
 * Cykl życia:
 * 1. Inicjalizacja komponentów i ustawienie referencji między nimi
 * 2. Ładowanie danych (dla edycji) lub ustawienie wartości domyślnych (nowy rekord)
 * 3. Obsługa akcji użytkownika (walidacja + wykonanie akcji)
 * 4. Nawigacja lub zamknięcie formularza na podstawie wyniku akcji
 *
 * @param entityId ID edytowanej encji (null dla nowych rekordów).
 * @param formSchemaBuilder Builder dostarczający definicję struktury formularza.
 * @param formDataManager Manager odpowiedzialny za operacje na danych.
 * @param formValidator Validator do sprawdzania poprawności danych.
 * @param payload Dodatkowe dane przekazane do formularza (np. ID rodzica).
 */
class FormHandler(
    private val entityId: Int? = null,
    formSchemaBuilder: FormSchemaBuilder,
    val formDataManager: FormDataManager,
    val formValidator: FormValidator = FormValidator(),
    private val payload: Map<String, Any?> = emptyMap()
) : FormActionTrigger {
    internal val errorManager: ErrorManager = ErrorManager()
    private val formState: FormState = FormState()
    private val formSchema: FormSchema = formSchemaBuilder.build()

    init {
        initializeControlsLifecycle()
        loadData()
    }

    /**
     * Ustawia referencje do komponentów formularza dla wszystkich kontrolek.
     *
     * Ta metoda jest kluczowa dla działania systemu - umożliwia kontrolkom
     * dostęp do globalnego stanu, schemy i menedżera błędów.
     */
    private fun initializeControlsLifecycle() {
        formSchema.getAllControls().values.forEach { control ->
            control.initializeControlLifecycle(formState, formSchema, errorManager, this)
        }
        formValidator.setupFormReferences(formState, formSchema, errorManager)
        formDataManager.setupFormReferences(errorManager)
    }


    /**
     * Publiczne API dla FormScreen - metody dostępu do komponentów formularza
     */
    internal fun getContentControlsInOrder(): List<String> = formSchema.contentOrder
    internal fun getActionBarControlsInOrder(): List<String> = formSchema.actionBarOrder
    internal fun getControl(name: String): Control<*>? = formSchema.getControl(name)
    internal fun getControlState(name: String): ControlState<*>? = formState.getControlState(name)

    /**
     * Ładuje dane dla edytowanej encji z bazy danych i inicjalizuje stan formularza.
     *
     * Proces ładowania:
     * 1. Pobiera wartości inicjalne z FormDataManager
     * 2. Ładuje dane encji z bazy danych
     * 3. Łączy dane z priorytetem dla wartości inicjalnych
     * 4. Inicjalizuje stany wszystkich kontrolek
     */
    internal fun loadData() {
        val initValues = formDataManager.initData(entityId, payload)
        formState.initializeStates(formSchema, initValues)
    }

    override fun triggerAction(actionKey: String, validates: Boolean): FormActionResult {
        val formActions = formDataManager.definedFormActions()
        val action = formActions[actionKey] ?: run {
            GlobalDialogManager.show(ErrorDialogConfig("Exception", "No form action defined for key: $actionKey"))
            return handleActionResult(FormActionResult.Failure)
        }

        errorManager.clearAll()

        if (validates && !formValidator.validateFields()) {
            SnackbarManager.showMessage(T.get("form.actions.containsErrors"))
            return handleActionResult(FormActionResult.ValidationFailed)
        }

        val rawFormData = formState.collectFormData(formSchema)

        // Walidacja
        if (validates && !formValidator.validateBusinessRules(rawFormData)) {
            SnackbarManager.showMessage(T.get("form.actions.containsErrors"))
            return handleActionResult(FormActionResult.ValidationFailed)
        }

        // Walidacja specyficzna dla akcji (zawsze uruchamiana, niezależnie od flagi 'validates')
        val actionValidator = formValidator.defineActionValidations()[actionKey]
        if (actionValidator != null && !actionValidator.invoke(rawFormData)) {
            SnackbarManager.showMessage(T.get("form.actions.containsErrors"))
            return handleActionResult(FormActionResult.ValidationFailed)
        }

        val actionResult = action.invoke(rawFormData, entityId)

        // Obsługa wyniku akcji
        return handleActionResult(actionResult)
    }

    private fun handleActionResult(result: FormActionResult): FormActionResult {
        when (result) {
            is FormActionResult.CloseScreen -> {
                AppRouter.goBack()
            }

            is FormActionResult.Navigate -> {
                AppRouter.navigateTo(result.screen)
            }

            is FormActionResult.Failure, is FormActionResult.Success, is FormActionResult.ValidationFailed -> {
                //no op
            }
        }
        return result
    }
}