@file:OptIn(ExperimentalDistributionDsl::class)

import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalDistributionDsl

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
                outputFileName = "browser-extension.js"

                devServer = (devServer ?: org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig.DevServer()).apply {
                    static = (static ?: mutableListOf()).apply {
                        add("src/jsMain/resources")
                    }
                }
            }

            distribution {
                outputDirectory = project.buildDir.resolve("dist")
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

                implementation(project(":ui-core"))
                // Wrapper do API przeglądarek
                // implementation("org.jetbrains.kotlin-wrappers:kotlin-browser:...")
            }
        }
    }
}