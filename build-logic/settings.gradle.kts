pluginManagement {
    repositories {
        maven("https://maven.myket.ir") { name = "MyketMirror" }
    }
}

dependencyResolutionManagement {
    repositories {
        // Same mirror as the root settings.gradle.kts (Google Maven + Central + Plugin Portal).
        maven("https://maven.myket.ir") { name = "MyketMirror" }
    }
    versionCatalogs {
        create("libs") {
            from(files("../gradle/libs.versions.toml"))
        }
    }
}

rootProject.name = "build-logic"
include(":convention")
