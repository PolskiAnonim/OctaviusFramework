package org.octavius.gradle

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction
import java.io.File

/**
 * Rejestruje task `validateTranslations` w projekcie root.
 * Plugin powinien być zaaplikowany tylko w głównym pliku `build.gradle.kts`.
 */
class TranslationValidationPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // Upewniamy się, że plugin jest aplikowany tylko w projekcie root
        if (project.rootProject != project) {
            throw GradleException("TranslationValidationPlugin can only be applied to the root project.")
        }

        project.tasks.register("validateTranslations", ValidateTranslationsTask::class.java) {
            group = "verification"
            description = "Validates translation usage - checks for unused translations and missing translations in code"
        }
    }
}

/**
 * Task, który wykonuje całą logikę walidacji tłumaczeń.
 */
abstract class ValidateTranslationsTask : DefaultTask() {

    private val gson = Gson()

    /**
     * Główna akcja taska.
     */
    @TaskAction
    fun execute() {
        logger.lifecycle("Starting translation validation across all modules...")

        val allTranslationFiles = mutableListOf<File>()
        val allSourceFiles = mutableListOf<File>()

        // Skanujemy wszystkie subprojekty (włącznie z root) w poszukiwaniu plików
        project.allprojects.forEach { proj ->
            logger.info("Scanning project: ${proj.path}")

            val potentialResourceDirs = listOf("src/commonMain/resources", "src/main/resources")
            val potentialSourceDirs = listOf("src/desktopMain/kotlin", "src/commonMain/kotlin", "src/main/kotlin", "src/jsMain/kotlin")

            // Szukamy plików z tłumaczeniami (tylko polskie jako plik bazowy)
            potentialResourceDirs.forEach { dir ->
                val translationFile = proj.file("$dir/translations_pl.json")
                if (translationFile.exists()) {
                    allTranslationFiles.add(translationFile)
                    logger.lifecycle("  -> Found translation file: ${translationFile.relativeTo(project.rootDir)}")
                }
            }

            // Szukamy plików źródłowych Kotlin
            potentialSourceDirs.forEach { dir ->
                val sourceDir = proj.file(dir)
                if (sourceDir.exists() && sourceDir.isDirectory) {
                    sourceDir.walkTopDown()
                        .filter { it.isFile && it.extension == "kt" }
                        .filter { it.name != "T.kt" } // Ignorujemy plik generujący
                        .toCollection(allSourceFiles)
                    logger.lifecycle("  -> Found source directory: ${sourceDir.relativeTo(project.rootDir)}")
                }
            }
        }

        // --- PARSOWANIE TŁUMACZEŃ ---
        val translationKeys = parseAllTranslationKeys(allTranslationFiles)

        // --- ANALIZA KODU ŹRÓDŁOWEGO ---
        val (usedKeys, missingKeys) = analyzeSourceCode(allSourceFiles, translationKeys)

        // --- RAPORTOWANIE WYNIKÓW ---
        reportResults(translationKeys, usedKeys, missingKeys)

        if (missingKeys.isNotEmpty()) {
            throw GradleException("Validation failed: Found ${missingKeys.size} missing translation keys. Check the report above.")
        }
    }

    /**
     * Parsuje wszystkie znalezione pliki JSON i zwraca zbiór kluczy.
     */
    private fun parseAllTranslationKeys(files: List<File>): Set<String> {
        val keys = mutableSetOf<String>()
        files.forEach { file ->
            try {
                val jsonContent = file.readText()
                extractKeysFromJson(jsonContent, "", keys)
            } catch (e: Exception) {
                logger.error("Failed to parse translation file: ${file.path}", e)
            }
        }
        return keys
    }

    /**
     * Analizuje pliki źródłowe w poszukiwaniu użycia T.get() i T.getPlural().
     */
    private fun analyzeSourceCode(sourceFiles: List<File>, translationKeys: Set<String>): Pair<Set<String>, Set<String>> {
        val usedKeys = mutableSetOf<String>()
        val missingKeys = mutableSetOf<String>() // Format: "ścieżka/do/pliku.kt: 'klucz'"

        val getPattern = Regex("""T\.get\s*\(\s*"([^"]+)"""")
        val getPluralPattern = Regex("""T\.getPlural\s*\(\s*"([^"]+)"""")

        sourceFiles.forEach { file ->
            val content = file.readText()

            (getPattern.findAll(content).asSequence() + getPluralPattern.findAll(content).asSequence())
                .forEach { match ->
                    val key = match.groupValues[1]
                    usedKeys.add(key)
                    if (key !in translationKeys) {
                        missingKeys.add("${file.relativeTo(project.rootDir)}: '$key'")
                    }
                }
        }
        return Pair(usedKeys, missingKeys)
    }

    /**
     * Wyświetla sformatowany raport walidacji w konsoli.
     */
    private fun reportResults(translationKeys: Set<String>, usedKeys: Set<String>, missingKeys: Set<String>) {
        val unusedKeys = translationKeys - usedKeys

        logger.lifecycle("\n=== TRANSLATION VALIDATION REPORT ===")

        if (unusedKeys.isNotEmpty()) {
            logger.warn("\n[WARNING] UNUSED TRANSLATIONS (${unusedKeys.size}):")
            unusedKeys.sorted().forEach { logger.warn("  - $it") }
        } else {
            logger.lifecycle("\n[OK] No unused translations found.")
        }

        if (missingKeys.isNotEmpty()) {
            logger.error("\n[ERROR] MISSING TRANSLATIONS IN CODE (${missingKeys.size}):")
            missingKeys.sorted().forEach { logger.error("  - $it") }
        } else {
            logger.lifecycle("\n[OK] All used keys are defined in translation files.")
        }

        logger.lifecycle("\n[SUMMARY]:")
        logger.lifecycle("  Total translation keys defined: ${translationKeys.size}")
        logger.lifecycle("  Total translation keys used in code: ${usedKeys.size}")
        logger.lifecycle("  - Unused keys: ${unusedKeys.size}")
        logger.lifecycle("  - Missing keys: ${missingKeys.size}")
        logger.lifecycle("\n======================================")
    }

    /**
     * Rekurencyjna funkcja pomocnicza do parsowania zagnieżdżonego JSONa przy użyciu Gson.
     */
    private fun extractKeysFromJson(jsonContent: String, prefix: String, keys: MutableSet<String>) {
        try {
            val mapType = object : TypeToken<Map<String, Any>>() {}.type
            val jsonMap: Map<String, Any> = gson.fromJson(jsonContent, mapType) ?: return
            parseJsonMap(jsonMap, prefix, keys)
        } catch (e: Exception) {
            logger.error("Error parsing JSON content.", e)
        }
    }

    private fun parseJsonMap(jsonMap: Map<*, *>, prefix: String, keys: MutableSet<String>) {
        jsonMap.forEach { (key, value) ->
            val fullKey = if (prefix.isEmpty()) key.toString() else "$prefix.$key"

            when (value) {
                is Map<*, *> -> {
                    // Sprawdzamy, czy to obiekt pluralizacji (one, few, many, etc.)
                    if (value.keys.any { it in listOf("one", "few", "many", "other") }) {
                        keys.add(fullKey)
                    } else {
                        // Jeśli nie, wchodzimy głębiej
                        parseJsonMap(value, fullKey, keys)
                    }
                }
                is String -> {
                    keys.add(fullKey)
                }
            }
        }
    }
}