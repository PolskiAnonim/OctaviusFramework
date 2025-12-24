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
        commonMain.dependencies {
            // Zależności Compose, które działają wszędzie

            api(composeLibs.runtime)
            api(composeLibs.foundation)
            api(composeLibs.material3)
            api(composeLibs.ui)
            api(composeLibs.components.resources)
            api(composeLibs.materialIconsExtended)


            // Inne współdzielone biblioteki
            implementation(libs.kotlinx.coroutines.core)
            implementation(projects.core) // ui-core może zależeć od core
            implementation(libs.kotlinx.datetime)
            implementation(projects.database.api)
        }

        val desktopMain by getting
        val jsMain by getting
    }
}