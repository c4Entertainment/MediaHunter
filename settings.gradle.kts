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
        // TorrentSearch pulls a couple of its dependencies from JitPack.
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "Movie Hunter"
include(":app")

// TorrentSearch fork (git submodule), built from source as a composite build so
// it keeps its own settings and version catalog.
includeBuild("external/TorrentSearch") {
    dependencySubstitution {
        substitute(module("com.prajwalch:torrentsearch")).using(project(":app"))
    }
}
