package org.octavius.novels.form.component

import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control

abstract class FormHandler(protected val entityId: Int? = null) {
    protected val formSchema: FormSchema
    protected val formState: FormState = FormState()
    protected val formDataManager: FormDataManager
    protected val formValidator: FormValidator = FormValidator()

    init {
        formSchema = createFormSchema()
        formDataManager = createDataManager()

        if (entityId != null) {
            loadData()
        } else {
            clearForm()
        }
    }


    // Publiczne API dla FormScreen
    fun getControlsInOrder(): List<String> = formSchema.order
    fun getControl(name: String): Control<*>? = formSchema.getControl(name)
    fun getControlState(name: String): ControlState<*>? = formState.getControlState(name)
    fun getAllControls(): Map<String, Control<*>> = formSchema.getAllControls()
    fun getAllStates(): Map<String, ControlState<*>> = formState.getAllStates()

    // Metody dla akcji z UI
    fun onSaveClicked(): Boolean = saveForm()
    fun onCancelClicked() { /* logika anulowania */ }

    // Metody abstrakcyjne
    protected abstract fun createFormSchema(): FormSchema
    protected abstract fun createDataManager(): FormDataManager

    fun loadData() {
        val initValues = formDataManager.initData(entityId!!)
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

        formState.initializeStates(formSchema, mergedData)
    }

    fun clearForm() {
        val initValues = formDataManager.initData(null)
        formState.initializeStates(formSchema, initValues)
    }

    private fun saveForm(): Boolean {
        if (!formValidator.validateFields(formSchema.getAllControls(), formState.getAllStates())) return false

        val rawFormData = formState.collectFormData(formSchema)

        if (!formValidator.validateBusinessRules(rawFormData)) return false
        val databaseOperations = formDataManager.processFormData(rawFormData, entityId)

        return try {
            DatabaseManager.updateDatabase(databaseOperations)
            true
        } catch (e: Exception) {
            println("Błąd zapisu formularza: ${e.message}")
            false
        }
    }
}