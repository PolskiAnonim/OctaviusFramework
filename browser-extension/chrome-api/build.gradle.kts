plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization) // Potrzebne do DTO
}

kotlin {
    js(IR) {
        browser()
    }
    sourceSets {
        val jsMain by getting {
            dependencies {
                // Nie potrzebuje api-contract, bo tylko wysyła dane, nie używa DTO
                // Ale potrzebuje wrapperów do komunikacji
                implementation(browserLibs.kotlin.browser)
            }
        }
    }
}