plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    // Definiujemy, że ten moduł jest tylko dla desktopa
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            // Zależności Compose
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.materialIconsExtended)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)

            implementation(libs.kotlinx.coroutines.core)
        }

        desktopMain.dependencies {
            implementation(project(":core"))
            implementation(project(":ui-core"))
            implementation(project(":form-engine"))
            implementation(project(":report-engine"))
            implementation(project(":api-contract"))
            implementation(project(":feature-contract"))
            implementation(project.dependencies.platform(libs.koin.bom))

            implementation(libs.koin.core)
            implementation(libs.koin.compose)

            implementation(libs.ktor.server.core)
        }
    }
}
