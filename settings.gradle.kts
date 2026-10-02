// All dependencies come from the Myket mirror, which proxies Google Maven, Maven Central and the
// Gradle Plugin Portal. The default repositories are intentionally not used (dl.google.com is
// unreliable on our network). To switch mirrors, change the URL here and in build-logic/settings.gradle.kts.
pluginManagement {
    includeBuild("build-logic")
    repositories {
        maven("https://maven.myket.ir") { name = "MyketMirror" }
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://maven.myket.ir") { name = "MyketMirror" }
    }
}

rootProject.name = "Varagh"

include(":app")

include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:database")
include(":core:network")
include(":core:designsystem")

include(":feature:library")
include(":feature:reader")
include(":feature:history")
include(":feature:profile")
include(":feature:social")
include(":feature:settings")
