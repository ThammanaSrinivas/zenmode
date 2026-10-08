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

// `-PuseMockCore=true` takes the open-source path even when the private sibling is present,
// so a contributor-shaped build can be run and tested on a machine that has core-private.
// app/build.gradle.kts reads the same property the same way (a Gradle property, so -P and
// gradle.properties both count) and must agree with this choice.
val forceMockCore = providers.gradleProperty("useMockCore").map { it.toBoolean() }.getOrElse(false)
val privateCoreDir = file("../zenmode_core_private")
if (privateCoreDir.exists() && !forceMockCore) {
    println("ZenMode: Found zenmode_core_private, including composite build.")
    includeBuild("../zenmode_core_private") {
        dependencySubstitution {
            substitute(module("com.zenlauncher.zenmode:core-private")).using(project(":core-private"))
        }
    }
} else {
    if (forceMockCore) {
        println("ZenMode: -PuseMockCore=true, using core-mock.")
    } else {
        println("ZenMode: zenmode_core_private not found, falling back to core-mock.")
    }
    include(":core-mock")
}