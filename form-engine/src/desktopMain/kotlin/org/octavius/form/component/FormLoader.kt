package org.octavius.form.component

import org.octavius.data.DataAccess
import org.octavius.data.DataResult
import org.octavius.data.builder.toList
import org.octavius.data.builder.toSingle
import org.octavius.dialog.ErrorDialogConfig
import org.octavius.dialog.GlobalDialogManager
import org.octavius.data.util.Converters

// --- Główne klasy DSL ---

data class FieldMapping(val controlName: String, val dbColumn: String)
data class ExistenceFlag(val controlName: String, val checkColumn: String)

abstract class BaseTableMappingBuilder {
    protected lateinit var tableName: String
    protected lateinit var tableAlias: String

    val fromTable: String
        get() = if (::tableName.isInitialized) "$tableName $tableAlias" else ""

    val mappings = mutableListOf<FieldMapping>()

    fun from(tableName: String, alias: String) {
        this.tableName = tableName
        this.tableAlias = alias
    }

    fun map(controlName: String, dbColumn: String? = null) {
        val finalDbColumn = dbColumn ?: Converters.toSnakeCase(controlName)
        mappings.add(FieldMapping(controlName, finalDbColumn))
    }

    protected fun validateBase() {
        if (!::tableName.isInitialized) throw IllegalStateException("Klauzula `from` jest wymagana w mapowaniu.")
    }
}

class OneToOneMappingBuilder : BaseTableMappingBuilder() {
    lateinit var joinCondition: String
    private var existenceFlag: ExistenceFlag? = null

    fun existenceFlag(controlName: String, checkColumn: String) {
        this.existenceFlag = ExistenceFlag(controlName, checkColumn)
    }

    fun getExistenceFlag(): ExistenceFlag? = existenceFlag

    fun on(condition: String) {
        this.joinCondition = condition
    }

    fun validate() {
        validateBase()
        if (!::joinCondition.isInitialized) throw IllegalStateException("Klauzula `on` jest wymagana w mapowaniu jeden-do-jednego.")
    }
}

class RelatedDataMappingBuilder : BaseTableMappingBuilder() {
    var joinClause: String = ""
    private lateinit var linkColumn: String

    fun join(joinSql: String) {
        this.joinClause = joinSql
    }

    fun linkedBy(foreignKeyColumn: String) {
        this.linkColumn = foreignKeyColumn
    }

    fun validate() {
        validateBase()
        if (!::linkColumn.isInitialized) throw IllegalStateException("Klauzula `linkedBy` jest wymagana w mapowaniu listy powiązanych danych.")
    }

    fun buildWhereClause(): String {
        return "$linkColumn = :id"
    }
}

// --- Reprezentacje wewnętrzne ---

sealed class RelationMapping
data class SimpleMapping(val mapping: FieldMapping) : RelationMapping()
data class OneToOneMapping(val existenceFlag: ExistenceFlag?, val table: String, val on: String, val fields: List<FieldMapping>) : RelationMapping()
data class RelatedDataMapping(val controlName: String, val builder: RelatedDataMappingBuilder) : RelationMapping()


// --- Główny Builder ---

class DataLoaderBuilder(private val dataAccess: DataAccess) {
    private lateinit var mainTableName: String
    private lateinit var mainTableAlias: String
    private val relations = mutableListOf<RelationMapping>()

    private val mainTable: String
        get() = if (::mainTableName.isInitialized) "$mainTableName $mainTableAlias" else ""


    fun from(tableName: String, alias: String) {
        this.mainTableName = tableName
        this.mainTableAlias = alias
    }

    fun map(controlName: String, dbColumn: String? = null) {
        val finalDbColumn = dbColumn ?: Converters.toSnakeCase(controlName)
        relations.add(SimpleMapping(FieldMapping(controlName, finalDbColumn)))
    }

    fun mapOneToOne(block: OneToOneMappingBuilder.() -> Unit) {
        val builder = OneToOneMappingBuilder().apply(block)
        builder.validate()
        relations.add(OneToOneMapping(builder.getExistenceFlag(), builder.fromTable, builder.joinCondition, builder.mappings))
    }

    fun mapRelatedList(controlName: String, block: RelatedDataMappingBuilder.() -> Unit) {
        val builder = RelatedDataMappingBuilder().apply(block)
        builder.validate()
        relations.add(RelatedDataMapping(controlName, builder))
    }

    fun execute(id: Int?): Map<String, Any?> {
        check(::mainTableName.isInitialized) { "Main table must be defined using from()" }
        if (id == null) return emptyMap()

        // Krok 1: Wczytaj dane z głównego zapytania (pola proste i 1-1)
        val mainData = loadMainData(id)

        // Krok 2: Wczytaj dane z relacji 1-N / N-N
        val relatedData = loadRelatedData(id)

        // Krok 3: Połącz wyniki
        return mainData + relatedData
    }

    private fun loadMainData(id: Int): Map<String, Any?> {
        val simpleFields = mutableListOf<String>()
        val joins = mutableListOf<String>()

        relations.forEach { rel ->
            when (rel) {
                is SimpleMapping ->
                    simpleFields.add(selectAs(rel.mapping.dbColumn, rel.mapping.controlName))
                is OneToOneMapping -> {
                    joins.add("LEFT JOIN ${rel.table} ON ${rel.on}")
                    rel.fields.forEach { field ->
                        simpleFields.add(selectAs(field.dbColumn, field.controlName))
                    }
                    rel.existenceFlag?.let { flag ->
                        simpleFields.add("CASE WHEN ${flag.checkColumn} IS NOT NULL THEN TRUE ELSE FALSE END AS \"${flag.controlName}\"")
                    }
                }
                is RelatedDataMapping -> { /* Ignorowane w tej fazie */ }
            }
        }

        if (simpleFields.isEmpty()) return emptyMap()

        val query = dataAccess.select(*simpleFields.toTypedArray())
            .from("$mainTable ${joins.joinToString(" ")}")
            .where("$mainTableAlias.id = :id")
            .toSingle("id" to id)

        return when (query) {
            is DataResult.Success -> query.value ?: emptyMap()
            is DataResult.Failure -> {
                GlobalDialogManager.show(ErrorDialogConfig(query.error))
                return emptyMap()
            }
        }
    }

    private fun loadRelatedData(id: Int): Map<String, Any?> {
        val result = mutableMapOf<String, Any?>()
        relations.filterIsInstance<RelatedDataMapping>().forEach { rel ->
            val builder = rel.builder
            val columns = builder.mappings.map { selectAs(it.dbColumn, it.controlName) }

            val relatedQuery = dataAccess.select(*columns.toTypedArray())
                .from("${builder.fromTable} ${builder.joinClause}")
                .where(builder.buildWhereClause())
                .toList("id" to id)

            when(relatedQuery) {
                is DataResult.Success -> result[rel.controlName] = relatedQuery.value
                is DataResult.Failure -> GlobalDialogManager.show(ErrorDialogConfig(relatedQuery.error))
            }
        }
        return result
    }

    // Helper do generowania "column AS control"
    private fun selectAs(dbColumn: String, controlName: String) = "$dbColumn AS \"$controlName\""
}