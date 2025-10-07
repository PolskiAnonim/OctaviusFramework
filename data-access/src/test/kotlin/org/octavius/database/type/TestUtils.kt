package org.octavius.database.type

import org.octavius.data.EnumCaseConvention
import org.octavius.domain.test.* // Importuj swoje klasy domenowe

/**
 * Tworzy w pełni funkcjonalną, ale sztuczną instancję TypeRegistry
 * na potrzeby testów jednostkowych, bez łączenia się z bazą danych.
 *
 * Odzwierciedla strukturę typów z pliku init-complex-test-db.sql.
 */
internal fun createFakeTypeRegistry(): TypeRegistry {
    // Pełne ścieżki do klas, żeby uniknąć literówek
    val tsPath = TestStatus::class.qualifiedName!!
    val tpPath = TestPriority::class.qualifiedName!!
    val tcPath = TestCategory::class.qualifiedName!!
    val tmPath = TestMetadata::class.qualifiedName!!
    val tpePath = TestPerson::class.qualifiedName!!
    val ttPath = TestTask::class.qualifiedName!!
    val tprojPath = TestProject::class.qualifiedName!!

    // Mapa 1: Nazwa typu PG -> Szczegóły typu (PostgresTypeInfo)
    val postgresTypeMap = buildMap {
        // --- Standardowe typy ---
        put("text", PostgresTypeInfo("text", TypeCategory.STANDARD))
        put("_text", PostgresTypeInfo("_text", TypeCategory.ARRAY, elementType = "text"))
        put("int4", PostgresTypeInfo("int4", TypeCategory.STANDARD))
        put("_int4", PostgresTypeInfo("_int4", TypeCategory.ARRAY, elementType = "int4"))
        put("bool", PostgresTypeInfo("bool", TypeCategory.STANDARD))

        put("timestamp", PostgresTypeInfo("timestamp", TypeCategory.STANDARD))
        put("timestamptz", PostgresTypeInfo("timestamptz", TypeCategory.STANDARD))
        put("interval", PostgresTypeInfo("interval", TypeCategory.STANDARD))

        put("numeric", PostgresTypeInfo("numeric", TypeCategory.STANDARD))
        put("jsonb", PostgresTypeInfo("jsonb", TypeCategory.STANDARD))
        put("uuid", PostgresTypeInfo("uuid", TypeCategory.STANDARD))
        put("date", PostgresTypeInfo("date", TypeCategory.STANDARD))
        // --- Enumy ---
        put("test_status", PostgresTypeInfo("test_status", TypeCategory.ENUM, enumConvention = EnumCaseConvention.SNAKE_CASE_LOWER))
        put("_test_status", PostgresTypeInfo("_test_status", TypeCategory.ARRAY, elementType = "test_status"))
        put("test_priority", PostgresTypeInfo("test_priority", TypeCategory.ENUM, enumConvention = EnumCaseConvention.SNAKE_CASE_LOWER))
        put("test_category", PostgresTypeInfo("test_category", TypeCategory.ENUM, enumConvention = EnumCaseConvention.SNAKE_CASE_LOWER))

        // --- Kompozyty ---
        put("test_metadata", PostgresTypeInfo("test_metadata", TypeCategory.COMPOSITE, attributes = mapOf(
            "created_at" to "timestamp", "updated_at" to "timestamp", "version" to "int4", "tags" to "_text"
        )))
        put("test_person", PostgresTypeInfo("test_person", TypeCategory.COMPOSITE, attributes = mapOf(
            "name" to "text", "age" to "int4", "email" to "text", "active" to "bool", "roles" to "_text"
        )))
        put("_test_person", PostgresTypeInfo("_test_person", TypeCategory.ARRAY, elementType = "test_person"))

        put("test_task", PostgresTypeInfo("test_task", TypeCategory.COMPOSITE, attributes = mapOf(
            "id" to "int4", "title" to "text", "description" to "text", "status" to "test_status",
            "priority" to "test_priority", "category" to "test_category", "assignee" to "test_person",
            "metadata" to "test_metadata", "subtasks" to "_text", "estimated_hours" to "numeric"
        )))
        put("_test_task", PostgresTypeInfo("_test_task", TypeCategory.ARRAY, elementType = "test_task"))

        put("test_project", PostgresTypeInfo("test_project", TypeCategory.COMPOSITE, attributes = mapOf(
            "name" to "text", "description" to "text", "status" to "test_status", "team_members" to "_test_person",
            "tasks" to "_test_task", "metadata" to "test_metadata", "budget" to "numeric"
        )))
        put("_test_project", PostgresTypeInfo("_test_project", TypeCategory.ARRAY, elementType = "test_project"))
    }

    // Mapa 2: Pełna ścieżka klasy -> Nazwa typu PG
    val classToPgMap = mapOf(
        tsPath to "test_status",
        tpPath to "test_priority",
        tcPath to "test_category",
        tmPath to "test_metadata",
        tpePath to "test_person",
        ttPath to "test_task",
        tprojPath to "test_project"
    )

    // Mapa 3: Nazwa typu PG -> Pełna ścieżka klasy
    val pgToClassMap = classToPgMap.entries.associate { (k, v) -> v to k }

    return TypeRegistry(postgresTypeMap, classToPgMap, pgToClassMap, mapOf())
}