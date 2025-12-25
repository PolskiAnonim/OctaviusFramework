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
                implementation(projects.browserExtension.chromeApi)
                implementation(kotlin("stdlib-js"))
                implementation(projects.core)
                implementation(projects.apiContract)
                implementation(projects.uiCore)
                implementation(compose.runtime)
                implementation(compose.html.core)
                implementation(compose.material3)
                implementation(libs.ktor.client.core)
                implementation(libs.ktor.client.js) // Implementacja dla środowiska JS
                implementation(libs.ktor.client.content.negotiation)
                implementation(libs.ktor.serialization.kotlinx.json)
                implementation(browserLibs.kotlin.browser)

                implementation(projects.featureAsianMedia)
            }
        }
    }
}
