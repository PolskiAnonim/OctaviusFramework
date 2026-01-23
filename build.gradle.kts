import org.jetbrains.dokka.gradle.DokkaExtension
import org.jetbrains.dokka.gradle.engine.parameters.VisibilityModifier
import org.octavius.gradle.registerGenerateTranslationAccessorsTask

plugins {
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
    dokka(project(":browser-extension:popup"))
    dokka(project(":browser-extension:content-script"))
    dokka(project(":browser-extension:chrome-api"))
}

// Aplikujemy plugin Dokka do wszystkich subprojektów z kodem Kotlin
subprojects {
    val excludedModules = setOf("browser-extension", "buildSrc")
    if (name !in excludedModules) {
        apply(plugin = "org.jetbrains.dokka")

        // Konfigurujemy Dokkę dla KAŻDEGO subprojektu
        extensions.configure<DokkaExtension> {
            // Ustawiamy unikalną nazwę modułu na podstawie pełnej ścieżki projektu
            // np. :database:api -> database-api, :core -> core
            moduleName.set(path.removePrefix(":").replace(":", "-"))

            dokkaSourceSets.configureEach {
                documentedVisibilities.set(
                    setOf(
                        VisibilityModifier.Public,
                        VisibilityModifier.Private,
                        VisibilityModifier.Protected,
                        VisibilityModifier.Internal
                    )
                )
                skipEmptyPackages.set(true)
            }
        }
    }
}

// Rejestracja taska generującego type-safe accessory do tłumaczeń
val generateTranslationAccessors = registerGenerateTranslationAccessorsTask(
    coreProject = project(":core")
)
