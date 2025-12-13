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

// Zależności Dokka - agregacja dokumentacji ze wszystkich modułów
dependencies {
    dokka(project(":core"))
    dokka(project(":api-server"))
    dokka(project(":api-contract"))
    dokka(project(":ui-core"))
    dokka(project(":desktop-app"))
    dokka(project(":form-engine"))
    dokka(project(":report-engine"))
    dokka(project(":feature-asian-media"))
    dokka(project(":feature-settings"))
    dokka(project(":feature-games"))
    dokka(project(":feature-contract"))
    dokka(project(":database:core"))
    dokka(project(":database:api"))
    dokka(project(":browser-extension:popup"))
    dokka(project(":browser-extension:content-script"))
    dokka(project(":browser-extension:chrome-api"))
}

// Aplikujemy plugin Dokka do wszystkich subprojektów z kodem Kotlin
subprojects {
    // Pomijamy moduły bez kodu źródłowego
    val excludedModules = setOf("browser-extension", "buildSrc")
    if (name !in excludedModules) {
        apply(plugin = "org.jetbrains.dokka")

        // Ustawiamy unikalną nazwę modułu na podstawie pełnej ścieżki projektu
        // np. :database:api -> database-api, :core -> core
        afterEvaluate {
            extensions.findByType<org.jetbrains.dokka.gradle.DokkaExtension>()?.apply {
                moduleName.set(path.removePrefix(":").replace(":", "-"))
            }
        }
    }
}
