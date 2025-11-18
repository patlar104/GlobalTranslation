pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        // Remove snapshot repository for production builds
        // Add back only if you need preview/alpha versions:
        // maven {
        //     url = uri("https://androidx.dev/snapshots/builds/12443078/artifacts/repository")
        // }
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Remove duplicate - google() already includes maven.google.com
    }
}

rootProject.name = "GlobalTranslation"
include(":app", ":core", ":data")
