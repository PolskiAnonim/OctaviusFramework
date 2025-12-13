import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.nio.charset.StandardCharsets

plugins {
    id("translation-validation")
    // this is necessary to avoid the plugins to be loaded multiple times
    // in each subproject's classloader
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.dokka)
}

// Konfiguracja Dokka - agregacja dokumentacji z wielu modułów
dokka {
    moduleName.set("Octavius Framework")

    dokkaPublications.html {
        outputDirectory.set(layout.buildDirectory.dir("dokka/html"))
    }
}

// Aplikujemy plugin Dokka do wszystkich subprojektów z kodem Kotlin
subprojects {
    // Pomijamy moduły bez kodu źródłowego
    val excludedModules = setOf("browser-extension", "buildSrc")
    if (name !in excludedModules) {
        apply(plugin = "org.jetbrains.dokka")
    }
}
