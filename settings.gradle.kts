pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    // The foojay-resolver-convention plugin is simply the engine that tells Gradle where to download that missing JDK.
    //
    // If you (or a teammate, or a CI/CD server like GitHub Actions) try to build this project but
    // don't have Java 17 installed, Gradle's "Toolchains" feature is smart enough to
    // automatically download and install the correct JDK for you so the build doesn't fail.
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Track My Path v2"
include(":app")
include(":benchmark")
