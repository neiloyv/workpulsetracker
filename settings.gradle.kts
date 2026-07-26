pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_PROJECT)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "workpulsetracker"

include(
    "tracker-common",
    "tracker-agent",
    "tracker-server",
    "tracker-ui"
)

// tracker-android — заглушка под будущее мобильное приложение (не подключён к Gradle)
