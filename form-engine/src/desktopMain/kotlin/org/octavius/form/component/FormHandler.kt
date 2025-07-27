package org.octavius.form.component

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.contract.TransactionManager
import org.octavius.form.ControlState
import org.octavius.form.control.base.Control

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
    val formValidator: FormValidator = FormValidator()
): KoinComponent {
    val errorManager: ErrorManager = ErrorManager()
    private val formState: FormState = FormState()
    private val formSchema: FormSchema = formSchemaBuilder.build()

    // Wstrzykujemy transaction manager
    private val transactionManager: TransactionManager by inject()

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
            transactionManager.execute(databaseOperations)
            true
        } catch (e: Exception) {
            errorManager.addGlobalError("Błąd zapisu do bazy danych: ${e.message}")
            false
        }
    }
}