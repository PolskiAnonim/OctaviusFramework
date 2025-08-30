package org.octavius.form.component

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.contract.BatchExecutor
import org.octavius.data.contract.ColumnInfo
import org.octavius.data.contract.DataFetcher
import org.octavius.data.contract.DataResult
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.form.control.base.FormResultData

/**
 * Abstrakcyjna klasa zarządzająca przepływem danych w formularzach.
 *
 * FormDataManager odpowiada za:
 * - Ładowanie danych z bazy danych dla edytowanych encji
 * - Dostarczanie wartości domyślnych dla nowych rekordów
 * - Definiowanie relacji między tabelami
 * - Przetwarzanie danych formularza do operacji bazodanowych
 *
 * Każdy formularz musi implementować własny DataManager dostosowany
 * do specyfiki domeny i struktury bazy danych.
 */
abstract class FormDataManager: KoinComponent {

    lateinit var errorManager: ErrorManager
    fun setupFormReferences(errorManager: ErrorManager) {
        this.errorManager = errorManager
    }

    protected val dataFetcher: DataFetcher by inject()
    protected val batchExecutor: BatchExecutor by inject()

    /**
     * Dostarcza wartości początkowe dla kontrolek formularza.
     *
     * @param loadedId ID edytowanej encji (null dla nowych rekordów)
     * @return mapa kontrolka->wartość z wartościami domyślnymi lub obliczonymi
     */
    abstract fun initData(loadedId: Int?, payload: Map<String, Any?>?): Map<String, Any?>

    /**
     * Definiuje relacje między tabelami potrzebne do załadowania pełnych danych encji.
     *
     * @return lista relacji tabel z warunkami JOIN dla DatabaseManager
     */
    abstract fun defineTableRelations(): List<TableRelation>

    /**
     * Ładuje kompletne dane encji z bazy danych używając zdefiniowanych relacji.
     *
     * @param id ID encji do załadowania
     * @return mapa kolumn->wartości z wszystkich powiązanych tabel
     */
    fun loadEntityData(id: Int): Map<ColumnInfo, Any?> {
        val tableRelations = defineTableRelations()

        if (tableRelations.isEmpty()) {
            throw IllegalArgumentException("Lista relacji tabel nie może być pusta")
        }

        val mainTable = tableRelations.first().tableName
        val tables = StringBuilder(mainTable)

        for (i in 1 until tableRelations.size) {
            val relation = tableRelations[i]
            tables.append(" LEFT JOIN ${relation.tableName} ON ${relation.joinCondition}")
        }

        val entity = dataFetcher.query().from(tables.toString()).where("$mainTable.id = :id")
            .toSingleWithColumnInfo(mapOf("id" to id))

        return when (entity) {
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(entity.error))
                mapOf()
            }
            is DataResult.Success<Map<ColumnInfo, Any?>?> -> entity.value ?: mapOf()
        }
    }

    /**
    * Definiuje logikę dla wszystkich akcji formularza (Zapisz, Anuluj, Usuń, etc.).
    * Klucz mapy odpowiada `actionKey` w `SubmitButtonControl`.
    * Wartość to lambda, która otrzymuje:
    * - formData: aktualne dane z formularza
    * - loadedId: ID edytowanego rekordu
    * Powinna zwrócić `FormActionResult`, aby FormHandler wiedział, czy operacja się udała.
    *
    * @return Mapa akcji formularza.
    */
    open fun definedFormActions(): Map<String, (formResultData: FormResultData, loadedId: Int?) -> FormActionResult> {
        return emptyMap()
    }
}