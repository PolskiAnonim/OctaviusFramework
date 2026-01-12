package org.octavius.database.builder

import org.octavius.data.builder.InsertQueryBuilder
import org.octavius.data.builder.OnConflictClauseBuilder
import org.octavius.database.RowMappers
import org.octavius.database.type.KotlinToPostgresConverter
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

internal class DatabaseInsertQueryBuilder(
    jdbcTemplate: JdbcTemplate,
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
        check(selectSource == null) { "Cannot use valuesExpressions() when fromSelect() has already been called." }
        expressions.forEach { (key, value) ->
            valuePlaceholders[key] = value
        }
    }

    override fun valueExpression(column: String, expression: String): InsertQueryBuilder = apply {
        check(selectSource == null) { "Cannot use valueExpression() when fromSelect() has already been called." }
        valuePlaceholders[column] = expression
    }

    override fun values(data: Map<String, Any?>): InsertQueryBuilder {
        check(selectSource == null) { "Cannot use values() when fromSelect() has already been called." }
        val placeholders = data.keys.associateWith { key -> ":$key" }
        // Delegujemy do metody niskopoziomowej
        return this.valuesExpressions(placeholders)
    }

    override fun values(values: List<String>): InsertQueryBuilder {
        check(selectSource == null) { "Cannot use values() when fromSelect() has already been called." }
        val placeholders = values.associateWith { key -> ":$key" }
        // Delegujemy do metody niskopoziomowej
        return this.valuesExpressions(placeholders)
    }

    override fun value(column: String): InsertQueryBuilder {
        check(selectSource == null) { "Cannot use value() when fromSelect() has already been called." }
        // Delegujemy do metody niskopoziomowej
        return this.valueExpression(column, ":$column")
    }

    override fun fromSelect(query: String): InsertQueryBuilder = apply {
        check(valuePlaceholders.isEmpty()) { "Cannot use fromSelect() when values() has already been called." }
        check(columns.isNotEmpty()) { "Must specify columns in insertInto() to use fromSelect()." }
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

        check(hasValues || hasSelect) { "Cannot build an INSERT statement without values or a SELECT source." }

        val targetColumns = columns.ifEmpty { valuePlaceholders.keys.toList() }
        val columnsSql = targetColumns.joinToString(", ")

        val sql = StringBuilder(buildWithClause())
        sql.append("INSERT INTO $table ($columnsSql)")

        if (hasValues) {
            val placeholders = targetColumns.joinToString(", ") { key -> valuePlaceholders.getValue(key) }
            sql.append("\nVALUES ($placeholders)")
        } else {
            sql.append("\n").append(selectSource!!)
        }

        onConflictBuilder?.let { builder ->
            val target = builder.target ?: throw IllegalStateException("ON CONFLICT target (columns or constraint) must be specified.")
            val action = builder.action ?: throw IllegalStateException("ON CONFLICT action (doNothing or doUpdate) must be specified.")

            // onConstraint już zawiera "ON CONSTRAINT", więc nie dodajemy nawiasów
            val targetSql = if (target.startsWith("ON CONSTRAINT")) target else "($target)"

            sql.append("\nON CONFLICT $targetSql $action")
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
     */
    override fun doUpdate(setExpression: String, whereCondition: String?) {
        require(setExpression.isNotBlank()) { "doUpdate cannot be blank." }
        val updateAction = StringBuilder("DO UPDATE SET $setExpression")
        whereCondition?.let {
            updateAction.append(" WHERE $it")
        }
        action = updateAction.toString()
    }

    /**
     * Przeciążenie dla vararg Pair
     */
    override fun doUpdate(vararg setPairs: Pair<String, String>, whereCondition: String?) {
        val setExpression = setPairs.joinToString(",\n") { (column, expression) ->
            "$column = $expression"
        }

        // Wywołujemy oryginalną metodę, aby nie duplikować logiki budowania klauzuli
        doUpdate(setExpression, whereCondition)
    }

    /**
     * Przeciążenie dla Map
     */
    override fun doUpdate(setMap: Map<String, String>, whereCondition: String?) {
        val setExpression = setMap.map { (column, expression) ->
            "$column = $expression"
        }.joinToString(",\n")

        doUpdate(setExpression, whereCondition)
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
