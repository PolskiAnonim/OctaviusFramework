package org.octavius.database.builder

import org.octavius.data.builder.SelectQueryBuilder
import org.octavius.database.RowMappers
import org.octavius.database.type.KotlinToPostgresConverter
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate

/**
 * Wewnętrzna implementacja [SelectQueryBuilder] do budowania zapytań SQL SELECT.
 * Dziedziczy z [AbstractQueryBuilder], aby ponownie wykorzystać logikę
 * klauzuli WITH i metod terminalnych.
 */
internal class DatabaseSelectQueryBuilder(
    jdbcTemplate: NamedParameterJdbcTemplate,
    rowMappers: RowMappers,
    kotlinToPostgresConverter: KotlinToPostgresConverter,
    private val selectClause: String
) : AbstractQueryBuilder<SelectQueryBuilder>(jdbcTemplate, kotlinToPostgresConverter, rowMappers, null), SelectQueryBuilder {
    override val canReturnResultsByDefault = true
    //------------------------------------------------------------------------------------------------------------------
    //                                    STAN WEWNĘTRZNY KLAUZULI SELECT
    //------------------------------------------------------------------------------------------------------------------

    private var fromClause: String? = null
    private var whereCondition: String? = null
    private var groupByClause: String? = null
    private var havingClause: String? = null
    private var orderByClause: String? = null
    private var limitValue: Long? = null
    private var offsetValue: Long? = null

    //------------------------------------------------------------------------------------------------------------------
    //                                      BUDOWANIE KLAUZULI SELECT
    //------------------------------------------------------------------------------------------------------------------

    /**
     * Ustawia klauzulę FROM.
     * Programista jest w pełni odpowiedzialny za przekazanie poprawnej składni.
     * Metoda nie dokonuje żadnego formatowania ani opakowywania w nawiasy.
     *
     * Przykłady poprawnych wartości:
     * - "users"
     * - "users u"
     * - "users AS u JOIN profiles p ON u.id = p.user_id"
     * - "(SELECT id FROM active_users) AS u"
     * - "UNNEST(:ids) AS id"
     *
     */
    override fun from(source: String): SelectQueryBuilder = apply {
        this.fromClause = source
    }

    override fun fromSubquery(subquery: String, alias: String?): SelectQueryBuilder {
        val query = if (alias == null) {
            "($subquery)"
        } else {
            "($subquery) AS $alias"
        }
        return this.from(query)
    }

    override fun where(condition: String?): SelectQueryBuilder = apply {
        this.whereCondition = condition
    }

    override fun groupBy(columns: String?): SelectQueryBuilder = apply {
        this.groupByClause = columns
    }

    override fun having(condition: String?): SelectQueryBuilder = apply {
        this.havingClause = condition
    }

    override fun orderBy(ordering: String?): SelectQueryBuilder = apply {
        this.orderByClause = ordering
    }

    override fun limit(count: Long?): SelectQueryBuilder = apply {
        this.limitValue = count
    }

    override fun offset(position: Long): SelectQueryBuilder = apply {
        this.offsetValue = position
    }

    override fun page(page: Long, size: Long): SelectQueryBuilder = apply {
        require(page >= 0) { "Page number cannot be negative." }
        require(size > 0) { "Page size must be positive." }
        this.offsetValue = page * size
        this.limitValue = size
    }

    //------------------------------------------------------------------------------------------------------------------
    //                                              BUDOWANIE SQL
    //------------------------------------------------------------------------------------------------------------------

    override fun buildSql(): String {
        check(!selectClause.isBlank()) { "Cannot build a SELECT query without a SELECT clause." }
        // Warunek: FROM musi istnieć LUB żadna z zależnych klauzul nie może istnieć.
        check(
            !fromClause.isNullOrBlank() || (whereCondition == null && groupByClause == null && orderByClause == null)
        ) {
            "WHERE, GROUP BY, or ORDER BY clauses require a FROM clause."
        }
        check(
            havingClause.isNullOrBlank() || !groupByClause.isNullOrBlank()
        ) {
            "HAVING clause requires a GROUP BY clause."
        }
        val sqlBuilder = StringBuilder(buildWithClause())

        sqlBuilder.append("SELECT ").append(selectClause)
        fromClause?.let { sqlBuilder.append("\nFROM ").append(it) }
        whereCondition?.takeIf { it.isNotBlank() }?.let { sqlBuilder.append("\nWHERE ").append(it) }
        groupByClause?.takeIf { it.isNotBlank() }?.let { sqlBuilder.append("\nGROUP BY ").append(it) }
        havingClause?.takeIf { it.isNotBlank() }?.let { sqlBuilder.append("\nHAVING ").append(it) }
        orderByClause?.takeIf { it.isNotBlank() }?.let { sqlBuilder.append("\nORDER BY ").append(it) }
        limitValue?.takeIf { it > 0 }?.let { sqlBuilder.append("\nLIMIT ").append(it) }
        offsetValue?.takeIf { it >= 0 }?.let { sqlBuilder.append("\nOFFSET ").append(it) }

        return sqlBuilder.toString()
    }

    //------------------------------------------------------------------------------------------------------------------
    //                                          KOPIA
    //------------------------------------------------------------------------------------------------------------------

    override fun copy(): DatabaseSelectQueryBuilder {
        // 1. Stwórz nową, "czystą" instancję za pomocą głównego konstruktora
        val newBuilder = DatabaseSelectQueryBuilder(
            this.jdbcTemplate,
            this.rowMappers,
            this.kotlinToPostgresConverter,
            this.selectClause
        )

        // 2. Skopiuj stan z klasy bazowej używając metody pomocniczej
        newBuilder.copyBaseStateFrom(this)

        // 3. Skopiuj stan specyficzny dla TEJ klasy
        newBuilder.fromClause = this.fromClause
        newBuilder.whereCondition = this.whereCondition
        newBuilder.groupByClause = this.groupByClause
        newBuilder.havingClause = this.havingClause
        newBuilder.orderByClause = this.orderByClause
        newBuilder.limitValue = this.limitValue
        newBuilder.offsetValue = this.offsetValue

        // 4. Zwróć w pełni skonfigurowaną kopię
        return newBuilder
    }
}