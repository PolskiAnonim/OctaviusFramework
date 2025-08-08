@file:OptIn(ExperimentalDistributionDsl::class)

import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpack

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    js(IR) {
        browser {
            commonWebpackConfig {
                outputFileName = "browser-extension.js" // Nazwa wyjściowego pliku JS
            }

            // Konfiguracja dla zadania uruchamiającego serwer deweloperski (niepotrzebne dla wtyczki)
            runTask {
                // ...
            }

            // Konfiguracja dla zadania budującego dystrybucję wtyczki
            distribution {
                outputDirectory.set(project.buildDir.resolve("dist"))
            }
        }
        binaries.executable()
    }

    sourceSets {
        val jsMain by getting {
            dependencies {
                // Standardowa biblioteka Kotlina dla JS
                implementation(kotlin("stdlib-js"))

                // Zależności do modułów
                implementation(project(":core"))
                implementation(project(":api-contract"))

                // Zależności Compose for Web
                implementation(compose.runtime)
                implementation(compose.html.core) // Rdzeń Compose for Web
                implementation(compose.material3)  // Komponenty Material 3

                // Ktor Client
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.js) // Implementacja dla środowiska JS
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)

                // Coroutines
                implementation(libs.kotlinx.coroutines.core)

                // Wrapper do API przeglądarek
                // implementation("org.jetbrains.kotlin-wrappers:kotlin-browser:...")
            }
        }
    }
}

// Skopiuj plik manifest.json do katalogu dystrybucyjnego po zbudowaniu
tasks.withType<Copy> {
    from("src/jsMain/resources") {
        include("manifest.json", "popup.html") // Dodaj tu wszystkie statyczne pliki
    }
    into(project.buildDir.resolve("dist"))
}

// Upewnij się, że kopiowanie manifestu dzieje się po zbudowaniu JS
tasks.named("jsBrowserDistribution") {
    finalizedBy(tasks.withType<Copy>())
}