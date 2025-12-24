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

            api(projects.database.api)

            implementation(libs.kotlinx.coroutines.core)

            implementation(libs.kotlinx.datetime)
        }

        desktopMain.dependencies {
            implementation(projects.core)
            implementation(project.dependencies.platform(libs.koin.bom))

            implementation(libs.koin.core)
            implementation(libs.koin.compose)
        }
    }
}
