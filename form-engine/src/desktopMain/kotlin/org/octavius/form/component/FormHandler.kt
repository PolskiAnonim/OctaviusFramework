package org.octavius.form.component

import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.ControlState
import org.octavius.form.FormActionResult
import org.octavius.form.FormActionTrigger
import org.octavius.form.control.base.Control
import org.octavius.localization.T
import org.octavius.navigation.AppRouter
import org.octavius.ui.snackbar.SnackbarManager

/**
 * Klasa obsługująca cykl życia formularza.
 *
 * FormHandler koordynuje pracę wszystkich komponentów formularza:
 * - FormSchema - definicja struktury formularza
 * - FormState - zarządzanie stanem kontrolek
 * - FormDataManager - ładowanie i przetwarzanie danych
 * - FormValidator - walidacja pól i reguł biznesowych
 *
 * @param entityId ID edytowanej encji (null dla nowych rekordów)
 */
class FormHandler(
    private val entityId: Int? = null,
    formSchemaBuilder: FormSchemaBuilder,
    val formDataManager: FormDataManager,
    val formValidator: FormValidator = FormValidator(),
    private val payload: Map<String, Any?>? = null
) : FormActionTrigger {
    val errorManager: ErrorManager = ErrorManager()
    private val formState: FormState = FormState()
    private val formSchema: FormSchema = formSchemaBuilder.build()

    init {
        setupFormReferences()
        if (entityId != null) {
            loadData()
        } else {
            clearForm()
        }
    }

    /**
     * Funkcja ustawia referencje do komponentów formularza dla kontrolek które tego wymagają
     */
    private fun setupFormReferences() {
        formSchema.getAllControls().forEach { (controlName, control) ->
            control.setupFormReferences(formState, formSchema, errorManager, controlName, this)
        }
        formValidator.setupFormReferences(formState, formSchema, errorManager)
        formDataManager.setupFormReferences(errorManager)
    }


    /**
     * Publiczne API dla FormScreen - metody dostępu do komponentów formularza
     */
    fun getContentControlsInOrder(): List<String> = formSchema.contentOrder
    fun getActionBarControlsInOrder(): List<String> = formSchema.actionBarOrder
    fun getControl(name: String): Control<*>? = formSchema.getControl(name)
    fun getControlState(name: String): ControlState<*>? = formState.getControlState(name)

    /**
     * Ładuje dane dla edytowanej encji z bazy danych i inicjalizuje stan formularza.
     */
    fun loadData() {
        val initValues = formDataManager.initData(entityId!!, payload)
        val databaseData = formDataManager.loadEntityData(entityId)

        // Merge danych z priorytetem dla initData
        val mergedData = mutableMapOf<String, Any?>()
        formSchema.getAllControls().forEach { (controlName, control) ->
            mergedData[controlName] = when {
                initValues.containsKey(controlName) -> initValues[controlName]
                control.columnInfo != null -> databaseData[control.columnInfo]
                else -> null
            }
        }

        formState.initializeStates(formSchema, mergedData, errorManager)
    }

    /**
     * Czyści formularz i ustawia wartości domyślne dla nowego rekordu.
     */
    fun clearForm() {
        val initValues = formDataManager.initData(null, payload)
        formState.initializeStates(formSchema, initValues, errorManager)
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