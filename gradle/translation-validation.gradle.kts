// Translation validation task
tasks.register("validateTranslations") {
    group = "verification"
    description = "Validates translation usage - checks for unused translations and missing translations in code"
    
    doLast {
        val translationFile = file("src/commonMain/resources/translations_pl.json")
        val sourceDir = file("src/desktopMain/kotlin")
        
        if (!translationFile.exists()) {
            throw GradleException("Translation file not found: ${translationFile.absolutePath}")
        }
        
        // Parse JSON translations
        val jsonContent = translationFile.readText()
        val translationKeys = mutableSetOf<String>()
        
        // Extract all translation keys from JSON (including nested keys)
        extractKeysFromJson(jsonContent, "", translationKeys)
        
        // Find all translation key usages in Kotlin code
        val usedKeys = mutableSetOf<String>()
        val missingKeys = mutableSetOf<String>()
        
        sourceDir.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .forEach { file ->
                val content = file.readText()
                
                // Find Translations.get() calls
                val getPattern = Regex("""Translations\.get\s*\(\s*"([^"]+)"""")
                getPattern.findAll(content).forEach { match ->
                    val key = match.groupValues[1]
                    usedKeys.add(key)
                    if (!translationKeys.contains(key)) {
                        missingKeys.add("${file.relativeTo(sourceDir)}: $key")
                    }
                }
                
                // Find Translations.getPlural() calls
                val getPluralPattern = Regex("""Translations\.getPlural\s*\(\s*"([^"]+)"""")
                getPluralPattern.findAll(content).forEach { match ->
                    val key = match.groupValues[1]
                    usedKeys.add(key)
                    if (!translationKeys.contains(key)) {
                        missingKeys.add("${file.relativeTo(sourceDir)}: $key")
                    }
                }
            }
        
        // Find unused translations
        val unusedKeys = translationKeys - usedKeys
        
        // Report results
        println("\n=== TRANSLATION VALIDATION REPORT ===")
        
        if (unusedKeys.isNotEmpty()) {
            println("\n[ERROR] UNUSED TRANSLATIONS (${unusedKeys.size}):")
            unusedKeys.sorted().forEach { key ->
                println("  - $key")
            }
        } else {
            println("\n[OK] No unused translations found")
        }
        
        if (missingKeys.isNotEmpty()) {
            println("\n[ERROR] MISSING TRANSLATIONS (${missingKeys.size}):")
            missingKeys.sorted().forEach { key ->
                println("  - $key")
            }
        } else {
            println("\n[OK] No missing translations found")
        }
        
        println("\n[SUMMARY]:")
        println("  Total translation keys: ${translationKeys.size}")
        println("  Used translation keys: ${usedKeys.size}")
        println("  Unused translations: ${unusedKeys.size}")
        println("  Missing translations: ${missingKeys.size}")
        
        if (missingKeys.isNotEmpty()) {
            throw GradleException("Found ${missingKeys.size} missing translations in code")
        }
    }
}

fun extractKeysFromJson(jsonContent: String, prefix: String, keys: MutableSet<String>) {
    val json = groovy.json.JsonSlurper().parseText(jsonContent) as Map<*, *>
    
    json.forEach { (key, value) ->
        val fullKey = if (prefix.isEmpty()) key.toString() else "$prefix.$key"
        
        when (value) {
            is Map<*, *> -> {
                // Check if it's a pluralization object (has 'one', 'few', 'many')
                if (value.containsKey("one") || value.containsKey("few") || value.containsKey("many")) {
                    // It's a pluralization object, add the key itself
                    keys.add(fullKey)
                } else {
                    // It's a nested object, recurse
                    extractKeysFromJson(groovy.json.JsonBuilder(value).toString(), fullKey, keys)
                }
            }
            is String -> {
                keys.add(fullKey)
            }
        }
    }
}