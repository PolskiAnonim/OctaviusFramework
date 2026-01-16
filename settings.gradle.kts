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
        create("browserLibs") {
            from(files("browser-extension/gradle/libs.versions.toml"))
        }
        create("composeLibs") {
            from(files("ui-core/gradle/libs.versions.toml"))
        }
    }
    repositories {
        mavenLocal()
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/PolskiAnonim/OctaviusDatabase")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GITHUB_TOKEN")
            }
        }
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
    // Browser extension
    ":browser-extension",
    ":browser-extension:content-script",
    ":browser-extension:popup",
    ":browser-extension:chrome-api",
)