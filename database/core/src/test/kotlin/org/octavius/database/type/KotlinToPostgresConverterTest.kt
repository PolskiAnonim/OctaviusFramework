package org.octavius.database.type

import kotlinx.datetime.LocalDateTime
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.octavius.domain.test.*
import org.postgresql.util.PGobject
import java.math.BigDecimal

/**
 * Testy jednostkowe dla KotlinToPostgresConverter.
 *
 * Ta klasa testowa wykorzystuje w pełni funkcjonalną, ale sztuczną instancję TypeRegistry
 * (stworzoną przez `createFakeTypeRegistry`), aby dokładnie symulować rzeczywiste
 * mapowanie typów bez potrzeby łączenia się z bazą danych.
 *
 * Dzięki temu testujemy logikę konwersji w izolacji, ale na realistycznych,
 * złożonych i zagnieżdżonych strukturach danych.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class KotlinToPostgresConverterTest {

    // Używamy "prawdziwego", ale sztucznego rejestru typów zamiast mocka.
    // Dzięki temu testujemy interakcję z kompletnym, spójnym zestawem definicji typów.
    private val typeRegistry = createFakeTypeRegistry()
    private val converter = KotlinToPostgresConverter(typeRegistry)

    @Nested
    inner class SimpleTypeExpansion {

        @Test
        fun `should not change simple parameters like primitives, strings, and nulls`() {
            val sql = "SELECT * FROM users WHERE id = :id AND name = :name AND profile IS :profile"
            val params = mapOf("id" to 123, "name" to "John", "profile" to null)

            val result = converter.expandParametersInQuery(sql, params)

            assertThat(result.expandedSql).isEqualTo(sql)
            assertThat(result.expandedParams).isEqualTo(params)
        }

        @Test
        fun `should convert enum to PGobject with correct snake_case_lower value`() {
            val sql = "SELECT * FROM tasks WHERE category = :category"
            val params = mapOf("category" to TestCategory.BugFix) // Używamy prawdziwego enuma

            val result = converter.expandParametersInQuery(sql, params)

            assertThat(result.expandedSql).isEqualTo(sql)
            assertThat(result.expandedParams).hasSize(1)

            val pgObject = result.expandedParams["category"] as PGobject
            assertThat(pgObject.type).isEqualTo("test_category")
            assertThat(pgObject.value).isEqualTo("bug_fix") // Zgodnie z konwencją w fake registry
        }

        @Test
        fun `should convert JsonObject to jsonb PGobject`() {
            val sql = "UPDATE documents SET data = :data WHERE id = 1"
            val jsonData = Json.parseToJsonElement("""{"key": "value", "count": 100}""") as JsonObject
            val params = mapOf("data" to jsonData)

            val result = converter.expandParametersInQuery(sql, params)

            assertThat(result.expandedSql).isEqualTo(sql)
            assertThat(result.expandedParams).hasSize(1)

            val pgObject = result.expandedParams["data"] as PGobject
            assertThat(pgObject.type).isEqualTo("jsonb")
            assertThat(pgObject.value).isEqualTo("""{"key":"value","count":100}""")
        }
    }

    @Nested
    inner class ArrayExpansion {

        @Test
        fun `should expand simple array into ARRAY syntax`() {
            val sql = "SELECT * FROM users WHERE id = ANY(:ids)"
            val params = mapOf("ids" to listOf(10, 20, 30))

            val result = converter.expandParametersInQuery(sql, params)

            assertThat(result.expandedSql).isEqualTo("SELECT * FROM users WHERE id = ANY(ARRAY[:ids_p1, :ids_p2, :ids_p3])")
            assertThat(result.expandedParams).isEqualTo(mapOf("ids_p1" to 10, "ids_p2" to 20, "ids_p3" to 30))
        }

        @Test
        fun `should handle empty arrays by converting to empty array literal`() {
            val sql = "SELECT * FROM users WHERE tags && :tags"
            val params = mapOf("tags" to emptyList<String>())

            val result = converter.expandParametersInQuery(sql, params)

            assertThat(result.expandedSql).isEqualTo("SELECT * FROM users WHERE tags && '{}'")
            assertThat(result.expandedParams).isEmpty()
        }

        @Test
        fun `should expand array of enums correctly`() {
            val sql = "SELECT * FROM tasks WHERE status = ANY(:statuses)"
            val params = mapOf("statuses" to listOf(TestStatus.Active, TestStatus.Pending))

            val result = converter.expandParametersInQuery(sql, params)

            assertThat(result.expandedSql).isEqualTo("SELECT * FROM tasks WHERE status = ANY(ARRAY[:statuses_p1, :statuses_p2])")
            assertThat(result.expandedParams).hasSize(2)

            val status1 = result.expandedParams["statuses_p1"] as PGobject
            assertThat(status1.type).isEqualTo("test_status")
            assertThat(status1.value).isEqualTo("active")

            val status2 = result.expandedParams["statuses_p2"] as PGobject
            assertThat(status2.type).isEqualTo("test_status")
            assertThat(status2.value).isEqualTo("pending")
        }
    }

    @Nested
    inner class CompositeExpansion {

        @Test
        fun `should expand a single data class into ROW syntax with type cast`() {
            val sql = "INSERT INTO employees (person) VALUES (:person)"
            val person = TestPerson("John Doe", 35, "john.doe@example.com", true, listOf("developer", "team-lead"))
            val params = mapOf("person" to person)


            val finalResult = converter.expandParametersInQuery(sql, params) // Musimy rekurencyjnie rozwinąć

            assertThat(finalResult.expandedSql).isEqualTo("INSERT INTO employees (person) VALUES (ROW(:person_f1, :person_f2, :person_f3, :person_f4, ARRAY[:person_f5_p1, :person_f5_p2])::test_person)")
            assertThat(finalResult.expandedParams).isEqualTo(mapOf(
                "person_f1" to "John Doe",
                "person_f2" to 35,
                "person_f3" to "john.doe@example.com",
                "person_f4" to true,
                "person_f5_p1" to "developer",
                "person_f5_p2" to "team-lead"
            ))
        }

        @Test
        fun `should expand an array of data classes`() {
            val sql = "SELECT process_team(:team)"
            val team = listOf(
                TestPerson("Alice", 28, "a@a.com", true, listOf("frontend")),
                TestPerson("Bob", 42, "b@b.com", false, listOf("backend", "dba"))
            )
            val params = mapOf("team" to team)

            val result = converter.expandParametersInQuery(sql, params)

            val expectedSql = "SELECT process_team(ARRAY[ROW(:team_p1_f1, :team_p1_f2, :team_p1_f3, :team_p1_f4, ARRAY[:team_p1_f5_p1])::test_person, ROW(:team_p2_f1, :team_p2_f2, :team_p2_f3, :team_p2_f4, ARRAY[:team_p2_f5_p1, :team_p2_f5_p2])::test_person])"
            val expectedParams = mapOf(
                "team_p1_f1" to "Alice", "team_p1_f2" to 28, "team_p1_f3" to "a@a.com", "team_p1_f4" to true, "team_p1_f5_p1" to "frontend",
                "team_p2_f1" to "Bob", "team_p2_f2" to 42, "team_p2_f3" to "b@b.com", "team_p2_f4" to false, "team_p2_f5_p1" to "backend", "team_p2_f5_p2" to "dba"
            )

            assertThat(result.expandedSql).isEqualTo(expectedSql)
            assertThat(result.expandedParams).isEqualTo(expectedParams)
        }
    }

    @Nested
    inner class ComplexNestedStructureExpansion {
        /**
         * Ten test jest kluczowy - weryfikuje poprawne, rekurencyjne rozwinięcie
         * głęboko zagnieżdżonej struktury danych, która zawiera niemal wszystkie
         * możliwe typy: kompozyty, tablice kompozytów, enumy, tablice prostych typów,
         * daty, liczby itp.
         */
        @Test
        fun `should expand a deeply nested data class with all features`() {
            val sql = "SELECT update_project(:project_data)"
            val project = TestProject(
                name = "Enterprise \"Fusion\" Project",
                description = "A complex project.",
                status = TestStatus.Active,
                teamMembers = listOf(
                    TestPerson("Project Manager", 45, "pm@corp.com", true, listOf("management")),
                    TestPerson("Lead Developer", 38, "lead@corp.com", true, listOf("dev", "architecture"))
                ),
                tasks = listOf(
                    TestTask(
                        id = 101,
                        title = "Initial Setup",
                        description = "Setup dev environment.",
                        status = TestStatus.Active,
                        priority = TestPriority.High,
                        category = TestCategory.Enhancement,
                        assignee = TestPerson("DevOps", 32, "devops@corp.com", true, listOf("infra")),
                        metadata = TestMetadata(
                            createdAt = LocalDateTime(2024, 1, 1, 10, 0),
                            updatedAt = LocalDateTime(2024, 1, 1, 12, 0),
                            version = 1,
                            tags = listOf("setup", "ci-cd")
                        ),
                        subtasks = listOf("Install Docker", "Configure DB"),
                        estimatedHours = BigDecimal("16.5")
                    )
                ),
                metadata = TestMetadata(
                    createdAt = LocalDateTime(2024, 1, 1, 9, 0),
                    updatedAt = LocalDateTime(2024, 1, 15, 18, 0),
                    version = 3,
                    tags = listOf("enterprise", "q1-2024")
                ),
                budget = BigDecimal("250000.75")
            )
            val params = mapOf("project_data" to project)

            val result = converter.expandParametersInQuery(sql, params)

            // Sprawdzamy tylko ogólną strukturę SQL, bo pełna byłaby ogromna
            assertThat(result.expandedSql).startsWith("SELECT update_project(ROW(")
            assertThat(result.expandedSql).endsWith(")::test_project)")
            assertThat(result.expandedSql).contains("::test_person")
            assertThat(result.expandedSql).contains("::test_task")
            assertThat(result.expandedSql).contains("::test_metadata")
            assertThat(result.expandedSql).contains("ARRAY[")

            // Sprawdzamy kluczowe, spłaszczone parametry
            assertThat(result.expandedParams)
                .containsEntry("project_data_f1", "Enterprise \"Fusion\" Project")
                .containsEntry("project_data_f7", BigDecimal("250000.75"))

            // Sprawdzamy, czy status projektu został poprawnie skonwertowany na PGobject
            val projectStatus = result.expandedParams["project_data_f3"] as PGobject
            assertThat(projectStatus.type).isEqualTo("test_status")
            assertThat(projectStatus.value).isEqualTo("active")

            // Sprawdzamy zagnieżdżonego członka zespołu
            assertThat(result.expandedParams)
                .containsEntry("project_data_f4_p2_f1", "Lead Developer") // teamMembers[1].name

            // Sprawdzamy głęboko zagnieżdżone pole w zadaniu
            assertThat(result.expandedParams)
                .containsEntry("project_data_f5_p1_f7_f1", "DevOps") // tasks[0].assignee.name

            val taskPriority = result.expandedParams["project_data_f5_p1_f5"] as PGobject // tasks[0].priority
            assertThat(taskPriority.type).isEqualTo("test_priority")
            assertThat(taskPriority.value).isEqualTo("high")

            // Sprawdzamy pole w metadanych zadania
            assertThat(result.expandedParams)
                .containsEntry("project_data_f5_p1_f8_f3", 1) // tasks[0].metadata.version
        }
    }
}