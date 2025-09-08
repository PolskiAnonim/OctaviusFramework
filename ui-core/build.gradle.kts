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
        // Wszystko, co ma być współdzielone, idzie do commonMain
        commonMain.dependencies {
            // Zależności Compose, które działają wszędzie
            api(compose.runtime)
            api(compose.foundation)
            api(compose.material3)
            api(compose.ui)
            api(compose.materialIconsExtended)
            // Inne współdzielone biblioteki
            implementation(libs.kotlinx.coroutines.core)
            implementation(projects.core) // ui-core może zależeć od core
        }

        // Zależności tylko dla desktopu (jeśli jakieś są)
        val desktopMain by getting
        val jsMain by getting
    }
}