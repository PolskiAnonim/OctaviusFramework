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
            implementation(projects.uiCore)
            implementation(composeLibs.components.resources)

            implementation(libs.kotlinx.coroutines.core)
        }

        desktopMain.dependencies {
            implementation(projects.core)
            implementation(projects.formEngine)
            implementation(projects.reportEngine)
            implementation(projects.featureContract)
            implementation(projects.apiContract)
            implementation(project.dependencies.platform(libs.koin.bom))

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }
    }
}
