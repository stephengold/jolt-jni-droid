// global build settings shared by all jolt-jni-droid subprojects

rootProject.name = "jolt-jni-droid"

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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral() // to find libraries released to the Maven Central repository
        //mavenLocal() // to find libraries installed locally
    }
}

// subprojects:
include("app")
