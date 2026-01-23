package org.octavius.gradle

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.component.ProjectComponentIdentifier
import org.gradle.api.tasks.TaskProvider
import java.nio.charset.StandardCharsets

internal fun mergeJsonMaps(target: MutableMap<String, Any?>, source: Map<String, Any?>) {
    for ((key, sourceValue) in source) {
        val targetValue = target[key]
        if (sourceValue is Map<*, *> && targetValue is Map<*, *>) {
            @Suppress("UNCHECKED_CAST")
            val newTarget = (targetValue as Map<String, Any?>).toMutableMap()
            @Suppress("UNCHECKED_CAST")
            mergeJsonMaps(newTarget, sourceValue as Map<String, Any?>)
            target[key] = newTarget
        } else {
            target[key] = sourceValue
        }
    }
}

/**
 * Rejestruje reużywalny task `mergeTranslations` w danym projekcie.
 *
 * @param configuration Obiekt konfiguracji zależności do przeskanowania.
 * @param projectsToScan Dodatkowe projekty do skanowania (oprócz tych z konfiguracji).
 * @return Zarejestrowany TaskProvider.
 */
fun Project.registerMergeTranslationsTask(
    configuration: Configuration
): TaskProvider<*> {
    return tasks.register("mergeTranslations") {
        group = "build"
        description = "Merges translation files from all module dependencies."

        val outputDir = layout.buildDirectory.dir("generated/translations")
        outputs.dir(outputDir)
        inputs.files(configuration)

        doLast {
            val gson = Gson()
            val mapType = object : TypeToken<Map<String, Any?>>() {}.type
            val mergedByLang = mutableMapOf<String, MutableMap<String, Any?>>()

            // Zbieramy projekty do przeskanowania
            val projectsToScan = mutableSetOf(project) // Zawsze dodajemy bieżący projekt
            configuration.incoming.resolutionResult.allComponents.forEach { component ->
                val componentId = component.id
                if (componentId is ProjectComponentIdentifier) {
                    projectsToScan.add(rootProject.project(componentId.projectPath))
                }
            }

            logger.lifecycle("Scanning projects for translations: ${projectsToScan.map { it.name }}")

            projectsToScan.forEach { depProject ->
                // Przeszukujemy katalog src w każdym module (commonMain, desktopMain, jsMain, etc.)
                depProject.file("src").walk().forEach { file ->
                    if (file.isFile && file.name.startsWith("translations_") && file.name.endsWith(".json")) {
                        val lang = file.name.substringAfter("translations_").substringBefore(".json")
                        val content = file.readText(Charsets.UTF_8)
                        if (content.isNotBlank()) {
                            logger.info("Found translation for '$lang' in ${depProject.name}/${file.relativeTo(depProject.projectDir)}")
                            try {
                                val sourceMap: Map<String, Any?> = gson.fromJson(content, mapType)
                                val targetMap = mergedByLang.getOrPut(lang) { mutableMapOf() }
                                mergeJsonMaps(targetMap, sourceMap)
                            } catch (e: Exception) {
                                logger.error("Failed to parse translation file: ${file.path}", e)
                            }
                        }
                    }
                }
            }

            outputDir.get().asFile.deleteRecursively()
            outputDir.get().asFile.mkdirs()

            if (mergedByLang.isEmpty()) {
                logger.warn("No translation files were found!")
            } else {
                mergedByLang.forEach { (lang, mergedMap) ->
                    val finalJsonString = gson.toJson(mergedMap)
                    val outputFile = outputDir.get().file("merged_translations_$lang.json").asFile
                    outputFile.writeText(finalJsonString, StandardCharsets.UTF_8)
                    logger.lifecycle("Successfully merged and wrote translations for '$lang' to ${outputFile.path}")
                }
            }
        }
    }
}