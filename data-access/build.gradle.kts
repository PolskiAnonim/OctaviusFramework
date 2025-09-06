plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(project(":core"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.datetime)
    implementation(libs.postgres)
    implementation(libs.hikari)
    implementation(libs.kotlin.reflect)
    implementation(libs.spring.jdbc)
    implementation(libs.classgraph)

    implementation(libs.kotlinx.coroutines.core)

    implementation(libs.logback.classic)
    implementation(libs.kotlin.logging.jvm)


    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
}

tasks.withType<Test> {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
    }
}