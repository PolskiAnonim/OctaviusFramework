package org.octavius.database.type

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.octavius.data.DataResult
import org.octavius.data.builder.toSingle
import org.octavius.data.toDataObject
import org.octavius.database.DatabaseAccess
import org.octavius.database.DatabaseConfig
import org.octavius.database.RowMappers
import org.octavius.domain.test.DynamicProfile
import org.octavius.domain.test.UserStats
import org.octavius.domain.test.UserWithDynamicProfile
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import org.springframework.jdbc.datasource.DataSourceTransactionManager
import java.nio.file.Files
import java.nio.file.Paths

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DynamicDtoMappingTest {

    private lateinit var dataAccess: org.octavius.data.DataAccess
    private lateinit var typeRegistry: TypeRegistry // Potrzebne do weryfikacji

    @BeforeAll
    fun setup() {
        // --- Krok 1: Konfiguracja i zabezpieczenia ---
        val databaseConfig = DatabaseConfig.loadFromFile("test-database.properties")
        val connectionUrl = databaseConfig.dbUrl
        val dbName = connectionUrl.substringAfterLast("/")
        if (!connectionUrl.contains("localhost:5432") || dbName != "octavius_test") {
            throw IllegalStateException("ABORTING TEST! Attempting to run on a non-test database. URL: '$connectionUrl'")
        }
        println("Safety guard passed for Dynamic DTO tests. Connected to: $dbName")

        // --- Krok 2: Inicjalizacja bazy i DAL-a ---
        val dataSource = HikariDataSource(HikariConfig().apply {
            jdbcUrl = databaseConfig.dbUrl
            username = databaseConfig.dbUsername
            password = databaseConfig.dbPassword
        })
        val jdbcTemplate = NamedParameterJdbcTemplate(dataSource)
        val transactionManager = DataSourceTransactionManager(dataSource)

        jdbcTemplate.jdbcTemplate.execute("DROP SCHEMA IF EXISTS public CASCADE;")
        jdbcTemplate.jdbcTemplate.execute("CREATE SCHEMA public;")
        val initSql = String(Files.readAllBytes(Paths.get(this::class.java.classLoader.getResource("init-dynamic-test-db.sql")!!.toURI())))
        jdbcTemplate.jdbcTemplate.execute(initSql)
        println("Dynamic DTO test DB schema initialized successfully.")

        // --- Krok 3: Załadowanie TypeRegistry i stworzenie pełnego DAL-a ---
        typeRegistry = runBlocking { TypeRegistryLoader(jdbcTemplate,databaseConfig.packagesToScan, databaseConfig.dbSchemas).load() }
        val kotlinToPostgresConverter = KotlinToPostgresConverter(typeRegistry)
        val postgresToKotlinConverter = PostgresToKotlinConverter(typeRegistry)
        val extractor = ResultSetValueExtractor(typeRegistry, postgresToKotlinConverter)
        val rowMappers = RowMappers(extractor)
        // Używamy pełnej implementacji DataAccess, aby testować jak najbliżej rzeczywistości
        dataAccess = DatabaseAccess(jdbcTemplate, transactionManager, rowMappers, kotlinToPostgresConverter)
    }

    @Test
    fun `should correctly load DynamicallyMappable classes into TypeRegistry`() {
        // Assert: Sprawdzamy, czy nasz loader poprawnie znalazł i zarejestrował klasy
        val profileClass = typeRegistry.getDynamicMappableClass("profile_dto")
        val statsClass = typeRegistry.getDynamicMappableClass("user_stats_dto")

        assertThat(profileClass).isNotNull
        assertThat(profileClass).isEqualTo(DynamicProfile::class)

        assertThat(statsClass).isNotNull
        assertThat(statsClass).isEqualTo(UserStats::class)

        assertThat(typeRegistry.getDynamicMappableClass("non_existent_dto")).isNull()
    }

    @Test
    fun `should map a dynamically created nested object using dynamic_dto and jsonb_build_object`() {
        // Arrange: Definiujemy zapytanie, które w locie tworzy zagnieżdżoną strukturę
        val sql = """
            SELECT
                u.user_id,
                u.username,
                (
                    SELECT dynamic_dto(
                        'profile_dto',
                        jsonb_build_object(
                            'role', p.role,
                            'permissions', p.permissions,
                            'lastLogin', '2024-01-01T12:00:00'
                        )
                    )
                    FROM dynamic_profiles p WHERE p.user_id = u.user_id
                ) AS profile
            FROM dynamic_users u
            WHERE u.user_id = :userId
        """.trimIndent()

        // Act: Wykonujemy zapytanie za pomocą naszego DAL-a
        // Używamy toSingle() aby dostać surową mapę i móc ją zbadać
        val result = dataAccess.rawQuery(sql)
            .toSingle("userId" to 1)
            .getOrThrow() // Używamy getOrThrow dla uproszczenia w teście

        assertThat(result).isNotNull

        // Assert: Sprawdzamy, czy konwersja zadziałała
        val userWithProfile = result!!.toDataObject<UserWithDynamicProfile>()

        assertThat(userWithProfile.userId).isEqualTo(1)
        assertThat(userWithProfile.username).isEqualTo("dynamic_user_1")
        assertThat(userWithProfile.profile).isNotNull
        assertThat(userWithProfile.profile.role).isEqualTo("administrator")
        assertThat(userWithProfile.profile.permissions).containsExactly("read", "write", "delete")
        assertThat(userWithProfile.profile.lastLogin).isEqualTo("2024-01-01T12:00:00")
    }

    @Test
    fun `should correctly map a different dynamic DTO to prove polymorphism`() {
        // Arrange: Zapytanie, które zwraca zupełnie inną dynamiczną strukturę
        val sql = """
            SELECT dynamic_dto(
                'user_stats_dto',
                jsonb_build_object(
                    'postCount', 150,
                    'commentCount', 3000
                )
            ) AS stats
        """.trimIndent()

        // Act
        val result = dataAccess.rawQuery(sql)
            .toSingle()
            .getOrThrow()

        // Assert
        val stats = result!!["stats"] as UserStats
        assertThat(stats.postCount).isEqualTo(150)
        assertThat(stats.commentCount).isEqualTo(3000)
    }

    // Prosta metoda rozszerzająca do uproszczenia testów
    private fun <T> DataResult<T>.getOrThrow(): T {
        return when (this) {
            is DataResult.Success -> this.value
            is DataResult.Failure -> throw this.error
        }
    }
}