package org.octavius.novels.form.component

import org.octavius.novels.database.DatabaseManager
import org.octavius.novels.form.ControlState
import org.octavius.novels.form.control.Control

/**
 * Abstrakcyjna klasa obsługująca cykl życia formularza.
 *
 * FormHandler koordynuje pracę wszystkich komponentów formularza:
 * - FormSchema - definicja struktury formularza
 * - FormState - zarządzanie stanem kontrolek
 * - FormDataManager - ładowanie i przetwarzanie danych
 * - FormValidator - walidacja pól i reguł biznesowych
 *
 * @param entityId ID edytowanej encji (null dla nowych rekordów)
 */
abstract class FormHandler(protected val entityId: Int? = null) {
    val errorManager: ErrorManager = ErrorManager()
    protected val formState: FormState = FormState()
    protected val formSchema: FormSchema
    protected val formDataManager: FormDataManager
    protected val formValidator: FormValidator

    init {
        formSchema = createFormSchema()
        formDataManager = createDataManager()
        formValidator = createFormValidator()
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
            control.setupFormReferences(formState, formSchema, errorManager, controlName)
        }
        formValidator.setupFormReferences(formState, formSchema, errorManager)
    }


    /**
     * Publiczne API dla FormScreen - metody dostępu do komponentów formularza
     */
    fun getControlsInOrder(): List<String> = formSchema.order
    fun getControl(name: String): Control<*>? = formSchema.getControl(name)
    fun getControlState(name: String): ControlState<*>? = formState.getControlState(name)

    /**
     * Metody obsługi akcji użytkownika z UI
     */
    fun onSaveClicked(): Boolean = saveForm()
    fun onCancelClicked() { /* logika anulowania */
    }

    /**
     * Metody abstrakcyjne do implementacji w klasach pochodnych
     */
    protected abstract fun createFormSchema(): FormSchema
    protected abstract fun createDataManager(): FormDataManager

    /**
     * Tworzy validator dla formularza. Można przesłonić dla niestandardowych reguł walidacji.
     */
    protected open fun createFormValidator(): FormValidator {
        return FormValidator()
    }

    /**
     * Ładuje dane dla edytowanej encji z bazy danych i inicjalizuje stan formularza.
     */
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

        formState.initializeStates(formSchema, mergedData, errorManager)
    }

    /**
     * Czyści formularz i ustawia wartości domyślne dla nowego rekordu.
     */
    fun clearForm() {
        val initValues = formDataManager.initData(null)
        formState.initializeStates(formSchema, initValues, errorManager)
    }

    /**
     * Przetwarza i zapisuje dane formularza do bazy danych.
     *
     * Proces zapisu:
     * 1. Walidacja pól (wymagalność, format, zależności)
     * 2. Zbieranie danych z kontrolek
     * 3. Walidacja reguł biznesowych
     * 4. Przetwarzanie danych do operacji bazodanowych
     * 5. Wykonanie operacji w bazie danych
     *
     * @return true jeśli zapis się powiódł, false w przypadku błędów
     */
    private fun saveForm(): Boolean {
        errorManager.clearAll()

        if (!formValidator.validateFields()) return false

        val rawFormData = formState.collectFormData(formSchema)

        if (!formValidator.validateBusinessRules(rawFormData)) return false
        val databaseOperations = formDataManager.processFormData(rawFormData, entityId)

        return try {
            DatabaseManager.updateDatabase(databaseOperations)
            true
        } catch (e: Exception) {
            errorManager.addGlobalError("Błąd zapisu do bazy danych: ${e.message}")
            false
        }
    }
}