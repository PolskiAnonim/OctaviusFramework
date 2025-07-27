package org.octavius.form.component

import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.octavius.data.contract.ColumnInfo
import org.octavius.data.contract.DataFetcher
import org.octavius.data.contract.DatabaseStep
import org.octavius.form.ControlResultData
import org.octavius.form.TableRelation

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

    protected val dataFetcher: DataFetcher by inject()

    /**
     * Dostarcza wartości początkowe dla kontrolek formularza.
     *
     * @param loadedId ID edytowanej encji (null dla nowych rekordów)
     * @return mapa kontrolka->wartość z wartościami domyślnymi lub obliczonymi
     */
    abstract fun initData(loadedId: Int?): Map<String, Any?>

    /**
     * Definiuje relacje między tabelami potrzebne do załadowania pełnych danych encji.
     *
     * @return lista relacji tabel z warunkami JOIN dla DatabaseManager
     */
    abstract fun defineTableRelations(): List<TableRelation>

    /**
     * Przetwarza dane z formularza na operacje bazodanowe.
     *
     * Konwertuje dane z kontrolek na sekwencję operacji INSERT/UPDATE/DELETE
     * z odpowiednimi foreign key i relacjami między tabelami.
     *
     * @param formData zebrane dane ze wszystkich kontrolek formularza
     * @param loadedId ID edytowanej encji (null dla nowych rekordów)
     * @return lista operacji do wykonania w bazie danych w odpowiedniej kolejności
     */
    abstract fun processFormData(
        formData: Map<String, ControlResultData>,
        loadedId: Int?
    ): List<DatabaseStep>

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

        return dataFetcher.fetchEntity(tables.toString(), "$mainTable.id = :id", mapOf("id" to id))
    }
}