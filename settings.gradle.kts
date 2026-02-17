@file:Suppress("UnstableApiUsage")

include(":feature:settings")


include(":feature:summary")


include(":feature:leaks")


include(":core:ui")


include(":core:shared")


include(":feature:details")

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
        maven { url = uri("https://www.jitpack.io") }
        maven { url = uri("https://mvn-repo.admost.com/artifactory/amr-2") }
        maven { url = uri("https://cboost.jfrog.io/artifactory/chartboost-ads/") }
        maven { url = uri("https://android-sdk.is.com/") }
        maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
        maven { url = uri("https://oss.sonatype.org/content/repositories/snapshots/") }
        maven { url = uri("https://artifacts.applovin.com/android") }
        maven { url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea") }
    }
}

rootProject.name = "WiredEye"
include(":app")
include(":core")
include(":feature")
include(":feature:monitor")
include(":core:network")
include(":core:data")
include(":core:common")
include(":core:nativelib")
include(":core:resources")
include(":core:preferences")

