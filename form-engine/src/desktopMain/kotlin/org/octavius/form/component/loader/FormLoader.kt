package org.octavius.form.component.loader

import org.octavius.data.DataAccess
import org.octavius.data.DataResult
import org.octavius.data.builder.toList
import org.octavius.data.builder.toSingle
import org.octavius.util.Converters

// --- Główne klasy DSL ---

data class FieldMapping(val controlName: String, val dbColumnAlias: String)

class OneToOneMappingBuilder {
    lateinit var fromTable: String
    lateinit var joinCondition: String
    val mappings = mutableListOf<FieldMapping>()

    fun from(tableName: String, alias: String) {
        this.fromTable = "$tableName $alias"
    }

    fun on(condition: String) {
        this.joinCondition = condition
    }

    fun map(controlName: String, dbColumn: String? = null) {
        val finalDbColumn = dbColumn ?: Converters.toSnakeCase(controlName)
        mappings.add(FieldMapping(controlName, finalDbColumn))
    }
}

// Zmieniona nazwa dla większej elastyczności (obsługuje 1-N i N-N)
class RelatedDataMappingBuilder {
    lateinit var fromTable: String
    var joinClause: String = ""
    lateinit var whereClause: String
    val mappings = mutableListOf<FieldMapping>()

    fun from(tableName: String, alias: String) {
        this.fromTable = "$tableName $alias"
    }

    // Nowa, opcjonalna metoda do łączenia tabel (np. dla N-N)
    fun join(joinSql: String) {
        this.joinClause = joinSql
    }

    // Jasno określa warunek filtrowania
    fun where(condition: String) {
        this.whereClause = condition
    }

    fun map(controlName: String, dbColumn: String? = null) {
        val finalDbColumn = dbColumn ?: Converters.toSnakeCase(controlName)
        mappings.add(FieldMapping(controlName, finalDbColumn))
    }
}


// --- Główny Builder ---

sealed class RelationMapping
data class SimpleMapping(val mapping: FieldMapping) : RelationMapping()
data class OneToOneMapping(val existenceControl: String?, val table: String, val on: String, val fields: List<FieldMapping>) : RelationMapping()

data class RelatedDataMapping(val controlName: String, val builder: RelatedDataMappingBuilder) : RelationMapping()


class DataLoaderBuilder(private val dataAccess: DataAccess) {
    private lateinit var mainTable: String
    private val relations = mutableListOf<RelationMapping>()

    fun from(tableName: String, alias: String) {
        this.mainTable = "$tableName $alias"
    }

    fun map(controlName: String, dbColumn: String? = null) {
        val finalDbColumn = dbColumn ?: Converters.toSnakeCase(controlName)
        relations.add(SimpleMapping(FieldMapping(controlName, finalDbColumn)))
    }

    fun mapOneToOne(existenceControl: String? = null, block: OneToOneMappingBuilder.() -> Unit) {
        val builder = OneToOneMappingBuilder().apply(block)
        relations.add(OneToOneMapping(existenceControl, builder.fromTable, builder.joinCondition, builder.mappings))
    }

    fun mapMany(controlName: String): MapManyContext {
        return MapManyContext(controlName, this)
    }

    class MapManyContext(private val controlName: String, private val parentBuilder: DataLoaderBuilder) {
        fun asRelatedList(block: RelatedDataMappingBuilder.() -> Unit) {
            val builder = RelatedDataMappingBuilder().apply(block)
            parentBuilder.relations.add(RelatedDataMapping(controlName, builder))
        }
    }

    fun execute(id: Int?): Map<String, Any?> {
        if (id == null) return emptyMap()

        val result = mutableMapOf<String, Any?>()

        // --- Faza 1: Zapytanie dla relacji 1-1 i pól prostych ---
        val simpleFields = mutableListOf<String>()
        val joins = mutableListOf<String>()

        relations.forEach { rel ->
            when (rel) {
                is SimpleMapping -> simpleFields.add("${rel.mapping.dbColumnAlias} AS \"${rel.mapping.controlName}\"")
                is OneToOneMapping -> {
                    rel.fields.forEach { field ->
                        simpleFields.add("${field.dbColumnAlias} AS \"${field.controlName}\"")
                    }
                    if (rel.existenceControl != null) {
                        val pkColumn = rel.on.substringAfter(" = ").trim()
                        simpleFields.add("CASE WHEN ${pkColumn} IS NOT NULL THEN TRUE ELSE FALSE END AS \"${rel.existenceControl}\"")
                    }
                    joins.add("LEFT JOIN ${rel.table} ON ${rel.on}")
                }
                else -> { /* 1-N obsługiwane osobno */ }
            }
        }

        if (simpleFields.isNotEmpty()) {
            val mainQuery = dataAccess.select(*simpleFields.toTypedArray())
                .from("$mainTable ${joins.joinToString(" ")}")
                .where("${mainTable.split(" ").last()}.id = :id")
                .toSingle("id" to id)

            when (mainQuery) {
                is DataResult.Success -> result.putAll(mainQuery.value?.filterValues { it != null } ?: emptyMap())
                is DataResult.Failure -> throw mainQuery.error
            }
        }

        // --- Faza 2: Osobne zapytania dla relacji 1-N i N-N ---
        relations.filterIsInstance<RelatedDataMapping>().forEach { rel ->
            val builder = rel.builder
            val columns = builder.mappings.map { "${it.dbColumnAlias} AS \"${it.controlName}\"" }

            val relatedQuery = dataAccess.select(*columns.toTypedArray())
                .from("${builder.fromTable} ${builder.joinClause}")
                .where(builder.whereClause)
                .toList("id" to id)

            when(relatedQuery) {
                is DataResult.Success -> result[rel.controlName] = relatedQuery.value
                is DataResult.Failure -> throw relatedQuery.error
            }
        }

        return result
    }
}