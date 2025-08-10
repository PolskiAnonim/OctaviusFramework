plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    js(IR) {
        browser()
        binaries.executable()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                implementation(kotlin("stdlib-js"))
                implementation(project(":core"))
                implementation(project(":api-contract"))
                implementation(project(":ui-core"))
                implementation(compose.runtime)
                implementation(compose.html.core)
                implementation(compose.material3)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.js) // Implementacja dla środowiska JS
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation("org.jetbrains.kotlin-wrappers:kotlin-browser:2025.8.6")
            }
        }
    }
}
