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
                implementation(kotlin("stdlib-js"))
                implementation(projects.core)
                // Nie potrzebuje api-contract, bo tylko wysyła dane, nie używa DTO
                // Ale potrzebuje wrapperów do komunikacji
                implementation(browserLibs.kotlin.browser)
            }
        }
    }
}