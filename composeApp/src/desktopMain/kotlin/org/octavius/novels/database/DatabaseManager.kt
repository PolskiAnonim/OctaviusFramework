package org.octavius.novels.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.octavius.novels.domain.NovelStatus
import java.sql.Connection
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.full.primaryConstructor

class DatabaseManager(
    private val jdbcUrl: String,
    private val username: String,
    private val password: String
) {
    private val dataSource: HikariDataSource

    init {
        val config = HikariConfig().apply {
            this.jdbcUrl = this@DatabaseManager.jdbcUrl
            this.username = this@DatabaseManager.username
            this.password = this@DatabaseManager.password
            maximumPoolSize = 10
        }
        dataSource = HikariDataSource(config)
    }

    private fun getConnection(): Connection = dataSource.connection

    fun <T : Any> executeQuery(sql: String, resultClass: KClass<T>): List<T> {
        val results = mutableListOf<T>()

        getConnection().use { connection ->
            connection.createStatement().use { statement ->
                statement.executeQuery(sql).use { resultSet ->
                    val constructor = resultClass.primaryConstructor!!
                    val parameters = constructor.parameters

                    while (resultSet.next()) {
                        val args = parameters.associateWith { param ->
                            val columnName = param.name!!
                            when (param.type.classifier) {
                                String::class -> resultSet.getString(columnName)
                                Int::class -> resultSet.getInt(columnName)
                                Long::class -> resultSet.getLong(columnName)
                                Double::class -> resultSet.getDouble(columnName)
                                Boolean::class -> resultSet.getBoolean(columnName)
                                NovelStatus::class -> NovelStatus.valueOf(resultSet.getString(columnName))
                                List::class -> listOf(resultSet.getArray(columnName).array)
                                else -> throw IllegalArgumentException("Unsupported type: ${param.type}")
                            }
                        }

                        results.add(constructor.callBy(args))
                    }
                }
            }
        }

        return results
    }
}