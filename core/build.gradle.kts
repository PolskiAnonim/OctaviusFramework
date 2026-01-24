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
            // Dodajemy wygenerowany katalog z Tr.kt
            kotlin.srcDir(layout.buildDirectory.dir("generated/kotlin/commonMain"))
        }
        val desktopMain by getting
        val jsMain by getting
    }
}

// Upewniamy się, że Tr.kt jest wygenerowane przed kompilacją i dokumentacją
tasks.matching { it.name.startsWith("compileKotlin") }.configureEach {
    dependsOn(rootProject.tasks.named("generateTranslationAccessors"))
}

tasks.matching { it.name.startsWith("dokka") }.configureEach {
    dependsOn(rootProject.tasks.named("generateTranslationAccessors"))
}