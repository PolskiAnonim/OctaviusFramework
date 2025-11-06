import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.nio.charset.StandardCharsets

plugins {
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
}

// Funkcja pomocnicza do parsowania JSON - musi być dostępna globalnie
fun extractKeysFromJson(jsonContent: String, prefix: String, keys: MutableSet<String>) {
    val json = groovy.json.JsonSlurper().parseText(jsonContent) as Map<*, *>

    json.forEach { (key, value) ->
        val fullKey = if (prefix.isEmpty()) key.toString() else "$prefix.$key"

        when (value) {
            is Map<*, *> -> {
                if (value.containsKey("one") || value.containsKey("few") || value.containsKey("many")) {
                    keys.add(fullKey)
                } else {
                    extractKeysFromJson(groovy.json.JsonBuilder(value).toString(), fullKey, keys)
                }
            }

            is String -> {
                keys.add(fullKey)
            }
        }
    }
}

// Zarejestruj task walidacji
tasks.register("validateTranslations") {
    group = "verification"
    description = "Validates translation usage - checks for unused translations and missing translations in code"

    doLast {
        println("Starting translation validation across all modules...")

        val allTranslationFiles = mutableListOf<File>()
        val allSourceDirs = mutableListOf<File>()

        // POPRAWNA SKŁADNIA DLA KOTLIN DSL
        // Używamy `allprojects` lub `subprojects` jako właściwości (kolekcji) i iterujemy po niej
        subprojects.forEach { project ->
            logger.lifecycle("Scanning project: ${project.name}")

            val potentialResourceDirs =
                listOf("src/desktopMain/resources", "src/commonMain/resources", "src/main/resources")
            val potentialSourceDirs = listOf("src/desktopMain/kotlin", "src/commonMain/kotlin", "src/main/kotlin")

            potentialResourceDirs.forEach { dir ->
                val translationFile = project.file("$dir/translations_pl.json")
                if (translationFile.exists()) {
                    allTranslationFiles.add(translationFile)
                    println("  -> Found translation file: ${translationFile.path}")
                }
            }

            potentialSourceDirs.forEach { dir ->
                val sourceDir = project.file(dir)
                if (sourceDir.exists() && sourceDir.isDirectory) {
                    allSourceDirs.add(sourceDir)
                    println("  -> Found source directory: ${sourceDir.path}")
                }
            }
        }

        // --- PARSOWANIE TŁUMACZEŃ ---
        val translationKeys = mutableSetOf<String>()
        allTranslationFiles.forEach { file ->
            try {
                val jsonContent = file.readText()
                extractKeysFromJson(jsonContent, "", translationKeys)
            } catch (e: Exception) {
                logger.error("Failed to parse translation file: ${file.path}", e)
            }
        }

        // --- ANALIZA KODU ŹRÓDŁOWEGO ---
        val usedKeys = mutableSetOf<String>()
        val missingKeys = mutableSetOf<String>()

        allSourceDirs.forEach { sourceDir ->
            sourceDir.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .filter { it.name != "Translations.kt" }
                .forEach { file ->
                    val content = file.readText()
                    val getPattern = Regex("""T\.get\s*\(\s*"([^"]+)"""")
                    val getPluralPattern = Regex("""T\.getPlural\s*\(\s*"([^"]+)"""")

                    (getPattern.findAll(content).asSequence() + getPluralPattern.findAll(content).asSequence())
                        .forEach { match ->
                            val key = match.groupValues[1]
                            usedKeys.add(key)
                            if (key !in translationKeys) {
                                missingKeys.add("${file.relativeTo(rootDir)}: '$key'")
                            }
                        }
                }
        }

        // --- RAPORTOWANIE WYNIKÓW ---
        val unusedKeys = translationKeys - usedKeys

        println("\n=== TRANSLATION VALIDATION REPORT ===")

        if (unusedKeys.isNotEmpty()) {
            println("\n[WARNING] UNUSED TRANSLATIONS (${unusedKeys.size}):")
            unusedKeys.sorted().forEach { println("  - $it") }
        } else {
            println("\n[OK] No unused translations found.")
        }

        if (missingKeys.isNotEmpty()) {
            println("\n[ERROR] MISSING TRANSLATIONS IN CODE (${missingKeys.size}):")
            missingKeys.sorted().forEach { println("  - $it") }
        } else {
            println("\n[OK] All used keys are defined in translation files.")
        }

        println("\n[SUMMARY]:")
        println("  Total translation keys: ${translationKeys.size}")
        println("  Used translation keys in code: ${usedKeys.size}")
        println("  - Unused: ${unusedKeys.size}")
        println("  - Missing: ${missingKeys.size}")
        println("\n======================================")

        if (missingKeys.isNotEmpty()) {
            throw GradleException("Validation failed: Found ${missingKeys.size} missing translation keys in the codebase. Check the report above.")
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


tasks.register("assembleBrowserExtension") {
    group = "Octavius Extension"
    description = "Assembles the final browser extension into build/extension"

    // Zależności zostają. To jest kluczowe.
    // Gwarantuje, że Webpack skończy robotę, zanim my zaczniemy.
    dependsOn(
        project(":extension-popup").tasks.named("jsBrowserProductionWebpack"),
        project(":extension-content-script").tasks.named("jsBrowserProductionWebpack")
    )

    // CAŁA LOGIKA PRZENIESIONA DO `doLast`
    doLast {
        println(">>>>>> ZACZYNAM assembleBrowserExtension <<<<<<")

        val extensionDir = project.buildDir.resolve("extension")

        // 1. Czyszczenie
        println(">>>>>> Czyszczenie starego folderu: ${extensionDir.path}")
        if (extensionDir.exists()) {
            extensionDir.deleteRecursively()
        }
        extensionDir.mkdirs()

        // 2. Mergowanie tłumaczeń
        val translationsDir = project.buildDir.resolve("extension_temp/translations")
        translationsDir.mkdirs()

        val outputDir = project.buildDir.resolve("extension_temp/translations")
        outputDir.mkdirs() // Tworzymy tymczasowy folder

        val gson = Gson()
        val mapType = object : TypeToken<Map<String, Any?>>() {}.type
        val mergedByLang = mutableMapOf<String, MutableMap<String, Any?>>()

        // Skanujemy zależności TYLKO modułu popup - tylko on ma UI
        val configuration = project.project(":extension-popup").configurations.getByName("jsRuntimeClasspath")
        val projectsToScan = mutableSetOf(project.project(":extension-popup"))
        configuration.incoming.resolutionResult.allComponents.forEach { component ->
            val componentId = component.id
            if (componentId is ProjectComponentIdentifier) {
                projectsToScan.add(project.project(componentId.projectPath))
            }
        }

        logger.info("Scanning for translations: ${projectsToScan.map { it.name }}")
        projectsToScan.forEach { depProject ->
            depProject.file("src").walk().forEach { file ->
                if (file.isFile && file.name.startsWith("translations_") && file.name.endsWith(".json")) {
                    val lang = file.name.substringAfter("translations_").substringBefore(".json")
                    val content = file.readText(Charsets.UTF_8)
                    if (content.isNotBlank()) {
                        val sourceMap: Map<String, Any?> = gson.fromJson(content, mapType)
                        val targetMap = mergedByLang.getOrPut(lang) { mutableMapOf() }
                        mergeJsonMaps(targetMap, sourceMap)
                    }
                }
            }
        }

        mergedByLang.forEach { (lang, mergedMap) ->
            val finalJsonString = gson.toJson(mergedMap)
            val outputFile = outputDir.resolve("translations_$lang.json")
            outputFile.writeText(finalJsonString, StandardCharsets.UTF_8)
            logger.lifecycle("Created merged translation file: ${outputFile.path}")
        }


        println(">>>>>> Mergowanie tłumaczeń zakończone.")

        // 3. Kopiowanie plików (używając prostego API plików)
        val popupSrcDir = project.project(":extension-popup").buildDir.resolve("kotlin-webpack/js/productionExecutable")
        val contentScriptSrcDir =
            project.project(":extension-content-script").buildDir.resolve("kotlin-webpack/js/productionExecutable")

        println(">>>>>> Kopiowanie z ${popupSrcDir.path}")
        popupSrcDir.listFiles()?.forEach { file ->
            println(">>>>>>  - Kopiowanie pliku: ${file.name}")
            file.copyTo(extensionDir.resolve(file.name), true)
        }

        println(">>>>>> Kopiowanie z ${contentScriptSrcDir.path}")
        contentScriptSrcDir.listFiles()?.forEach { file ->
            if (file.name == "extension-content-script.js") {
                println(">>>>>>  - Kopiowanie pliku: ${file.name}")
                file.copyTo(extensionDir.resolve(file.name), true)
            }
        }

        println(">>>>>> Kopiowanie z ${translationsDir.path}")
        translationsDir.listFiles()?.forEach { file ->
            println(">>>>>>  - Kopiowanie pliku: ${file.name}")
            file.copyTo(extensionDir.resolve(file.name), true)
        }
        val popupResourcesSrcDir = project.project(":extension-popup").file("src/jsMain/resources")
        println("[4/4] Kopiowanie zasobów statycznych i tłumaczeń...")
        println("  -> Źródło zasobów popupa: ${popupResourcesSrcDir.path}")
        popupResourcesSrcDir.listFiles()?.forEach { file ->
            println("    - Kopiuję: ${file.name}")
            file.copyTo(extensionDir.resolve(file.name), true)
        }

        println(">>>>>> Zakończono. Folder ${extensionDir.path} powinien być gotowy.")
    }
}