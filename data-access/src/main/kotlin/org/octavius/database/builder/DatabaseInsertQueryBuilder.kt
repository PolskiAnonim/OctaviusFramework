package org.octavius.database.builder

import org.octavius.data.builder.InsertQueryBuilder
import org.octavius.data.builder.OnConflictClauseBuilder
import org.octavius.database.RowMappers
import org.octavius.database.type.KotlinToPostgresConverter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

internal class DatabaseInsertQueryBuilder(
    jdbcTemplate: NamedParameterJdbcTemplate,
    kotlinToPostgresConverter: KotlinToPostgresConverter,
    rowMappers: RowMappers,
    table: String,
    private val columns: List<String>
) : AbstractQueryBuilder<InsertQueryBuilder>(jdbcTemplate, kotlinToPostgresConverter, rowMappers, table), InsertQueryBuilder {
    override val canReturnResultsByDefault = false
    private val valuePlaceholders = mutableMapOf<String, String>()
    private var selectSource: String? = null
    private var onConflictBuilder: DatabaseOnConflictClauseBuilder? = null

    override fun valuesExpressions(expressions: Map<String, String>): InsertQueryBuilder = apply {
        expressions.forEach { (key, value) ->
            valuePlaceholders[key] = value
        }
    }

    override fun valueExpression(column: String, expression: String): InsertQueryBuilder = apply {
        valuePlaceholders[column] = expression
    }

    override fun values(data: Map<String, Any?>): InsertQueryBuilder {
        val placeholders = data.keys.associateWith { key -> ":$key" }
        // Delegujemy do metody niskopoziomowej
        return this.valuesExpressions(placeholders)
    }

    override fun value(column: String): InsertQueryBuilder {
        // Delegujemy do metody niskopoziomowej
        return this.valueExpression(column, ":$column")
    }

    override fun fromSelect(query: String): InsertQueryBuilder = apply {
        if (valuePlaceholders.isNotEmpty()) {
            throw IllegalStateException("Cannot use fromSelect() when values() has already been called.")
        }
        if (columns.isEmpty()) {
            throw IllegalStateException("Must specify columns in insertInto() to use fromSelect().")
        }
        this.selectSource = query
    }

    /**
     * Konfiguruje klauzulę ON CONFLICT w sposób płynny i bezpieczny.
     * Przykład:
     * .onConflict {
     *   onColumns("email")
     *   doUpdate("last_login = NOW()")
     * }
     */
    override fun onConflict(config: OnConflictClauseBuilder.() -> Unit): InsertQueryBuilder = apply {
        if (onConflictBuilder == null) {
            onConflictBuilder = DatabaseOnConflictClauseBuilder()
        }
        onConflictBuilder!!.apply(config)
    }

    override fun buildSql(): String {
        val hasValues = valuePlaceholders.isNotEmpty()
        val hasSelect = selectSource != null

        if (!hasValues && !hasSelect) {
            throw IllegalStateException("Cannot build an INSERT statement without values or a SELECT source.")
        }

        val targetColumns = columns.ifEmpty { valuePlaceholders.keys.toList() }
        val columnsSql = targetColumns.joinToString(", ")

        val sql = StringBuilder(buildWithClause())
        sql.append("INSERT INTO $table ($columnsSql) ")

        if (hasValues) {
            val placeholders = targetColumns.joinToString(", ") { key -> valuePlaceholders[key]!! }
            sql.append("VALUES ($placeholders)")
        } else {
            sql.append(selectSource!!)
        }

        onConflictBuilder?.let { builder ->
            val target = builder.target ?: throw IllegalStateException("ON CONFLICT target (columns or constraint) must be specified.")
            val action = builder.action ?: throw IllegalStateException("ON CONFLICT action (doNothing or doUpdate) must be specified.")

            // onConstraint już zawiera "ON CONSTRAINT", więc nie dodajemy nawiasów
            val targetSql = if (target.startsWith("ON CONSTRAINT")) target else "($target)"

            sql.append(" ON CONFLICT $targetSql $action")
        }

        sql.append(buildReturningClause())

        return sql.toString()
    }

    //------------------------------------------------------------------------------------------------------------------
    //                                          KOPIA
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Tworzy i zwraca głęboką kopię tego buildera.
     * Umożliwia bezpieczne tworzenie wariantów zapytania bez modyfikowania oryginału.
     */
    override fun copy(): DatabaseInsertQueryBuilder {
        // 1. Stwórz nową, "czystą" instancję za pomocą głównego konstruktora
        val newBuilder = DatabaseInsertQueryBuilder(
            this.jdbcTemplate,
            this.kotlinToPostgresConverter,
            this.rowMappers,
            this.table!!, // Wiemy, że table nie jest nullem dla INSERT
            this.columns
        )

        newBuilder.copyBaseStateFrom(this)

        newBuilder.valuePlaceholders.putAll(this.valuePlaceholders) // Kopiujemy zawartość mapy, a nie referencję!
        newBuilder.selectSource = this.selectSource
        // Kopiujemy także onConflictBuilder, jeśli istnieje! Używamy jego własnej metody copy().
        newBuilder.onConflictBuilder = this.onConflictBuilder?.copy()

        // 4. Zwróć w pełni skonfigurowaną kopię
        return newBuilder
    }

}

internal class DatabaseOnConflictClauseBuilder : OnConflictClauseBuilder {
    internal var target: String? = null
    internal var action: String? = null

    /** Definiuje cel konfliktu (kolumny). */
    override fun onColumns(vararg columns: String) {
        target = columns.joinToString(", ")
    }

    /** Definiuje cel konfliktu (nazwa ograniczenia). */
    override fun onConstraint(constraintName: String) {
        target = "ON CONSTRAINT $constraintName"
    }

    /** Definiuje akcję DO NOTHING. */
    override fun doNothing() {
        action = "DO NOTHING"
    }

    /**
     * Definiuje akcję DO UPDATE.
     * @param setExpression Wyrażenie SET, np. "name = EXCLUDED.name, updated_at = NOW()"
     * @param whereCondition Opcjonalny warunek WHERE, np. "target_table.version < EXCLUDED.version"
     */
    override fun doUpdate(setExpression: String, whereCondition: String?) {
        val updateAction = StringBuilder("DO UPDATE SET $setExpression")
        whereCondition?.let {
            updateAction.append(" WHERE $it")
        }
        action = updateAction.toString()
    }

    /**
     * Tworzy kopię tego buildera klauzuli ON CONFLICT.
     */
    fun copy(): DatabaseOnConflictClauseBuilder {
        val newBuilder = DatabaseOnConflictClauseBuilder()
        newBuilder.target = this.target
        newBuilder.action = this.action
        return newBuilder
    }
}