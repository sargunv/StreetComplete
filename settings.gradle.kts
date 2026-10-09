rootProject.name = "StreetComplete"

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
        mavenLocal {
            content { includeGroup("org.maplibre.nativeffi") }
        }
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

include(":app")
include(":androidApp")
include(":desktopApp")

// Build against a local checkout: -PmaplibreComposePath=/path/to/maplibre-compose
includeBuild(providers.gradleProperty("maplibreComposePath").get()) {
    dependencySubstitution {
        substitute(module("org.maplibre.compose:maplibre-compose"))
            .using(project(":lib:maplibre-compose"))
        substitute(module("org.maplibre.compose:location"))
            .using(project(":lib:location"))
        substitute(module("org.maplibre.compose:maplibre-compose-runtime-metal-macos-arm64"))
            .using(project(":lib:maplibre-compose-runtime-metal-macos-arm64"))
    }
}
