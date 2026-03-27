plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    jvm("desktop")
    js(IR) {
        browser()
        binaries.executable()
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                // Zależności Compose, które działają wszędzie

                api(composeLibs.runtime)
                api(composeLibs.foundation)
                api(composeLibs.material3)
                api(composeLibs.ui)
                api(composeLibs.components.resources)
                api(composeLibs.materialIconsExtended)


                // Inne współdzielone biblioteki
                implementation(libs.kotlinx.coroutines.core)
                implementation(libs.kotlinx.datetime)
                implementation(libs.octavius.database.api)
            }
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
