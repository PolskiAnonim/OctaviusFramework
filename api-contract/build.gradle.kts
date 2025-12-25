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
                api(libs.ktor.server.core)
                implementation(projects.uiCore)
            }
        }
        val desktopMain by getting
        val jsMain by getting
    }
}