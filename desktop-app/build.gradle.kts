import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.nio.charset.StandardCharsets

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.materialIconsExtended)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
        }
        desktopMain.dependencies {
            implementation(projects.core)
            implementation(projects.dataAccess)
            implementation(projects.uiCore)
            implementation(projects.featureContract)
            implementation(projects.formEngine)
            implementation(projects.reportEngine)
            implementation(projects.featureGames)
            implementation(projects.featureSettings)
            implementation(projects.featureAsianMedia)
            implementation(projects.apiServer)
            implementation(projects.apiContract)
            implementation(compose.desktop.currentOs)

            implementation(project.dependencies.platform(libs.koin.bom))

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.slf4j)

            // Implementacja loggera
            implementation(libs.logback.classic)
        }
        desktopMain.resources.srcDir(layout.buildDirectory.dir("generated/translations"))
    }
}

compose.desktop {
    application {
        mainClass = "org.octavius.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            includeAllModules = true
            packageName = "org.octavius"
            packageVersion = "1.0.0"
//            windows {
//                iconFile.set(project.file("icon.ico"))
//            }
        }
    }
}

// Rekursywna funkcja do głębokiego łączenia obiektów JSON
fun mergeJsonMaps(target: MutableMap<String, Any?>, source: Map<String, Any?>) {
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

val mergeTranslations by tasks.registering {
    group = "build"
    description = "Merges translation files from all module dependencies."

    val outputDir = project.layout.buildDirectory.dir("generated/translations")
    outputs.dir(outputDir)

    // Definiujemy, że nasze zadanie zależy od plików zasobów wszystkich zależności.
    // To pomaga Gradle'owi w inteligentnym cache'owaniu.
    val configuration = project.configurations.getByName("desktopRuntimeClasspath")
    inputs.files(configuration)

    doLast {
        val gson = Gson()
        val mapType = object : TypeToken<Map<String, Any?>>() {}.type
        val mergedByLang = mutableMapOf<String, MutableMap<String, Any?>>()

        // Zbieramy listę projektów do przeskanowania w sposób zalecany przez Gradle
        val projectsToScan = mutableSetOf(project) // Zawsze dodajemy bieżący projekt
        configuration.incoming.resolutionResult.allComponents.forEach { component ->
            val componentId = component.id
            // Interesują nas tylko komponenty, które są innymi modułami w naszym projekcie
            if (componentId is ProjectComponentIdentifier) {
                val dependencyProject = project.project(componentId.projectPath)
                projectsToScan.add(dependencyProject)
            }
        }

        logger.info("Scanning projects for translations: ${projectsToScan.map { it.name }}")

        projectsToScan.forEach { depProject ->
            // Przeszukujemy katalog src w każdym module
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
            logger.warn("No translation files were found! The application might not have any text.")
        } else {
            mergedByLang.forEach { (lang, mergedMap) ->
                val finalJsonString = gson.toJson(mergedMap)
                val outputFile = outputDir.get().file("translations_$lang.json").asFile
                outputFile.writeText(finalJsonString, StandardCharsets.UTF_8)
                logger.lifecycle("Successfully merged and wrote translations for '$lang' to ${outputFile.path}")
            }
        }
    }
}

tasks.named("desktopProcessResources") {
    dependsOn(mergeTranslations)
}