rootProject.name = "Octavius"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    versionCatalogs {
        create("dbLibs") {
            from(files("database/gradle/libs.versions.toml"))
        }
        create("browserLibs") {
            from(files("browser-extension/gradle/libs.versions.toml"))
        }
    }
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

include(
    ":desktop-app",
    ":form-engine",
    ":report-engine",
    ":api-server",
    ":ui-core",
    ":core",
    ":api-contract",
    ":feature-contract",
    // Features
    ":feature-settings",
    ":feature-asian-media",
    ":feature-games",
    ":feature-books",
    // Database
    ":database:core",
    ":database:api",
    // Browser extension
    ":browser-extension",
    ":browser-extension:content-script",
    ":browser-extension:popup",
    ":browser-extension:chrome-api",
)