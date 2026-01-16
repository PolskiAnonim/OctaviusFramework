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
        val commonMain by getting {
            dependencies {
                implementation(libs.kotlinx.serialization.json)
                implementation(libs.kotlinx.datetime)
                implementation(libs.octavius.database.api)
            }
        }
        val desktopMain by getting
        val jsMain by getting
    }
}