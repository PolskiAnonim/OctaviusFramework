plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    // Definiujemy targety dla obu platform
    jvm("desktop")
    js(IR) {
        browser()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Potrzebujemy zależności do commonMain z core
                api(projects.core)

                api(libs.ktor.server.core)
            }
        }
        val desktopMain by getting
        val jsMain by getting
    }
}