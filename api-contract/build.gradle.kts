plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(project(":core"))
    
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
}