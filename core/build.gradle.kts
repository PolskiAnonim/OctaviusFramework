plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {

    jvm("desktop") // Dla aplikacji desktopowej
    js(IR) {       // Dla wtyczki
        browser()
    }

    sourceSets {
        // Wszystkie zależności idą do commonMain, bo są wieloplatformowe
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
            }
        }
        val desktopMain by getting
        val jsMain by getting
    }
}