plugins {
    kotlin("jvm")
    alias(libs.plugins.kotlinSerialization)
}

dependencies {
    implementation(projects.core)
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


    // Test dependencies - precyzyjnie tylko to co potrzebne
    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter.api)  // @Test, @BeforeAll etc.
    testRuntimeOnly(libs.junit.jupiter.engine)  // silnik do uruchamiania testów
    testRuntimeOnly(libs.junit.platform.launcher)
    testImplementation(libs.junit.jupiter.params)

    testImplementation(libs.mockk)
    testImplementation(libs.assertj.core)
}

tasks.withType<Test> {
    useJUnitPlatform()

    testLogging {
        events("passed", "skipped", "failed")
    }
}