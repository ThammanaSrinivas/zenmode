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
        mavenCentral()
    }
}

rootProject.name = "ZenMode"
include(":app")

includeBuild("core-api") {
    dependencySubstitution {
        substitute(module("com.zenlauncher.zenmode:core-api")).using(project(":"))
    }
}

val privateCoreDir = file("../zenmode_core_private")
if (privateCoreDir.exists()) {
    println("ZenMode: Found zenmode_core_private, including composite build.")
    includeBuild("../zenmode_core_private") {
        dependencySubstitution {
            substitute(module("com.zenlauncher.zenmode:core-private")).using(project(":core-private"))
        }
    }
} else {
    println("ZenMode: zenmode_core_private not found, falling back to core-mock.")
    include(":core-mock")
}