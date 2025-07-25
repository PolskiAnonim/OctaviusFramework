plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

//dependencies {
//    implementation(libs.kotlinx.serialization.json)
//    implementation(libs.kotlinx.datetime)
//    implementation(libs.postgres)
//    implementation(libs.hikari)
//    implementation(libs.kotlin.reflect)
//    implementation(libs.spring.jdbc)
//}

kotlin {
    // Definiujemy, że ten moduł jest tylko dla desktopa
    jvm("desktop")

    sourceSets {
        val desktopMain by getting

        commonMain.dependencies {
            // Zależności Compose
            implementation(compose.runtime)
            implementation(compose.foundation)

            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlin.reflect)
            implementation(libs.kotlinx.datetime)

        }

        desktopMain.dependencies {
            implementation(libs.postgres)
            implementation(libs.hikari)
            implementation(libs.spring.jdbc)
        }
    }
}