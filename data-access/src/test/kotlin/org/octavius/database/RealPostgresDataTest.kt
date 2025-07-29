package org.octavius.database

import com.zaxxer.hikari.HikariConfig
import com.zaxxer.hikari.HikariDataSource
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.octavius.domain.test.TestPerson
import org.octavius.domain.test.TestPriority
import org.octavius.domain.test.TestProject
import org.octavius.domain.test.TestStatus
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate
import java.nio.file.Files
import java.nio.file.Paths

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class RealPostgresDataTest {

    // Te pola będą dostępne we wszystkich testach w tej klasie
    private lateinit var jdbcTemplate: NamedParameterJdbcTemplate
    private lateinit var postgresToKotlinConverter: PostgresToKotlinConverter

    @BeforeAll
    fun setup() {
        // 1. Ładujemy konfigurację
        DatabaseConfig.loadFromFile("test-database.properties")

        // 2. Tworzymy DataSource i JdbcTemplate
        val hikariConfig = HikariConfig().apply {
            jdbcUrl = DatabaseConfig.dbUrl
            username = DatabaseConfig.dbUsername
            password = DatabaseConfig.dbPassword
        }
        val dataSource = HikariDataSource(hikariConfig)
        jdbcTemplate = NamedParameterJdbcTemplate(dataSource)

        // 3. Wrzucamy skrypt testowy do bazy DOKŁADNIE RAZ
        try {
            // Najpierw usuwamy stary schemat, żeby mieć pewność czystego startu
            jdbcTemplate.jdbcTemplate.execute("DROP SCHEMA IF EXISTS public CASCADE;")
            jdbcTemplate.jdbcTemplate.execute("CREATE SCHEMA public;")

            // Wczytujemy i wykonujemy cały skrypt SQL (łącznie z INSERT)
            val initSql = String(Files.readAllBytes(Paths.get(this::class.java.classLoader.getResource("init-complex-test-db.sql")!!.toURI())))
            jdbcTemplate.jdbcTemplate.execute(initSql)
            println("Complex test DB schema and data initialized successfully.")
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }

        // 4. Inicjalizujemy zależności dla konwerterów
        val typeRegistry = TypeRegistry(jdbcTemplate)
        postgresToKotlinConverter = PostgresToKotlinConverter(typeRegistry)
    }

    // Nie potrzebujemy @BeforeEach, bo tylko czytamy dane!

    @Test
    fun `should convert a row with complex types into a map of correct Kotlin objects`() {
        // Given: dane są już w bazie dzięki setupowi

        // When: Używamy RowMappers (które używają konwertera) do pobrania danych
        val result: Map<String, Any?> = jdbcTemplate.queryForObject(
            "SELECT * FROM complex_test_data WHERE id = 1",
            emptyMap<String, Any>(),
            RowMappers(postgresToKotlinConverter).ColumnNameMapper()
        )!!

// Then: Sprawdzamy każdy przekonwertowany obiekt
        assertThat(result["simple_text"]).isEqualTo("Test \"quoted\" text with special chars: ąćęłńóśźż")
        assertThat(result["simple_bool"]).isEqualTo(true)
        assertThat(result["single_status"]).isEqualTo(TestStatus.Active)

        // Sprawdzamy tablicę enumów
        assertThat(result["status_array"] as List<*>).containsExactly(TestStatus.Active, TestStatus.Pending, TestStatus.NotStarted)

        // Sprawdzamy pojedynczy kompozyt
        val person = result["single_person"] as TestPerson
        assertThat(person.name).isEqualTo("John \"The Developer\" Doe")
        assertThat(person.age).isEqualTo(30)
        assertThat(person.roles).containsExactly("admin", "developer", "team-lead")

        // Sprawdzamy "mega" kompozyt - projekt
        val project = result["project_data"] as TestProject
        assertThat(project.name).isEqualTo("Complex \"Enterprise\" Project")
        assertThat(project.status).isEqualTo(TestStatus.Active)
        assertThat(project.teamMembers).hasSize(4)
        assertThat(project.teamMembers[0].name).isEqualTo("Project Manager")
        assertThat(project.teamMembers[3].name).isEqualTo(null)

        val firstTask = project.tasks[0]
        assertThat(firstTask.title).isEqualTo("Setup \"Development\" Environment")
        assertThat(firstTask.priority).isEqualTo(TestPriority.High)
        assertThat(firstTask.assignee.name).isEqualTo("DevOps Guy")
        assertThat(firstTask.metadata.tags).containsExactly("setup", "infrastructure", "priority")

        // Sprawdzamy tablicę projektów (zagnieżdżenie do potęgi)
        val projectArray = result["project_array"] as List<TestProject>
        assertThat(projectArray).hasSize(2)
        assertThat(projectArray[0].name).isEqualTo("Small \"Maintenance\" Project")
        assertThat(projectArray[1].tasks[0].assignee.name).isEqualTo("AI Specialist")
    }
}