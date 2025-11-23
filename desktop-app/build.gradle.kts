import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import org.octavius.gradle.registerMergeTranslationsTask
import java.nio.charset.StandardCharsets

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.materialIconsExtended)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.kotlinx.coroutines.core)
        }
        desktopMain.dependencies {
            implementation(projects.core)
            implementation(projects.database.core)
            implementation(projects.uiCore)
            implementation(projects.featureContract)
            implementation(projects.formEngine)
            implementation(projects.reportEngine)
            implementation(projects.featureGames)
            implementation(projects.featureSettings)
            implementation(projects.featureAsianMedia)
            implementation(projects.apiServer)
            implementation(projects.apiContract)
            implementation(compose.desktop.currentOs)

            implementation(project.dependencies.platform(libs.koin.bom))

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
            implementation(libs.koin.slf4j)

            // Implementacja loggera
            implementation(libs.logback.classic)
        }
        desktopMain.resources.srcDir(layout.buildDirectory.dir("generated/translations"))
    }
}

compose.desktop {
    application {
        mainClass = "org.octavius.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Exe)
            includeAllModules = true
            packageName = "org.octavius"
            packageVersion = "1.0.0"
//            windows {
//                iconFile.set(project.file("icon.ico"))
//            }
        }
    }
}

val mergeTranslations = registerMergeTranslationsTask(
    configuration = configurations.getByName("desktopRuntimeClasspath")
)
// Upewniamy się, że zasoby są procesowane dopiero po zmergowaniu tłumaczeń
tasks.named("desktopProcessResources") {
    dependsOn(mergeTranslations)
}