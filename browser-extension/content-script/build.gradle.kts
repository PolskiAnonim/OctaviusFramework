plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization) // Potrzebne do DTO
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
                implementation(projects.featureAsianMedia)
                // Ale potrzebuje wrapperów do komunikacji
                implementation(browserLibs.kotlin.browser)
                implementation(libs.kotlinx.serialization.json)
            }
        }
    }
}