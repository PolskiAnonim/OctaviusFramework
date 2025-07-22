

plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.postgres)
    implementation(libs.hikari)
    implementation(libs.kotlin.reflect)
    implementation(libs.spring.jdbc)
}