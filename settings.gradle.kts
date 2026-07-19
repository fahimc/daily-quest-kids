pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "DailyQuestKids"

include(":app")
include(":core-common")
include(":core-model")
include(":core-design")
include(":core-data")
include(":core-testing")
include(":puzzle-engine")
include(":puzzle-validator")
