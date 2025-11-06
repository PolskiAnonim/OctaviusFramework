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
    ":feature-asian-media",
    ":feature-settings",
    ":feature-games",
    ":form-engine",
    ":report-engine",
    ":api-server",
    ":ui-core",
    ":data-access",
    "data-access-api",
    ":core",
    ":api-contract",
    ":feature-contract",
    ":browser-extension",
    ":browser-extension:content-script",
    ":browser-extension:popup",
    ":browser-extension:chrome-api",
)