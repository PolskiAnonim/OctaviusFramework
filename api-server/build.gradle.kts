plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(project(":core"))
    implementation(project(":ui-core"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.serialization.kotlinx.json)
}