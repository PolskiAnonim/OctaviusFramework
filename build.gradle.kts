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

            val potentialResourceDirs = listOf("src/desktopMain/resources", "src/commonMain/resources", "src/main/resources")
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
                    val getPattern = Regex("""Translations\.get\s*\(\s*"([^"]+)"""")
                    val getPluralPattern = Regex("""Translations\.getPlural\s*\(\s*"([^"]+)"""")

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

tasks.register<Copy>("assembleBrowserExtension") {
    group = "Octavius Extension"
    description = "Assembles the final browser extension into build/extension"

    // Krok 1: Wyczyść stary katalog
    delete(project.buildDir.resolve("extension"))

    // Krok 2: Skopiuj wszystkie pliki z modułu popup
    from(project(":extension-popup").buildDir.resolve("dist/js/productionExecutable"))

    // Krok 3: Skopiuj tylko plik JS z modułu content-script
    from(project(":extension-content-script").buildDir.resolve("dist/js/productionExecutable")) {
        include("extension-content-script.js") // Kopiujemy tylko to, co potrzebne
    }

    // Krok 4: Zależności - upewnij się, że oba moduły są zbudowane przed kopiowaniem
    dependsOn(
        project(":extension-popup").tasks.named("jsBrowserDistribution"),
        project(":extension-content-script").tasks.named("jsBrowserDistribution")
    )

    // Krok 5: Określ katalog docelowy
    into(project.buildDir.resolve("extension"))
}