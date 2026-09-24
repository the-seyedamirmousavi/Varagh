package com.mid.varagh.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

internal fun Project.configureAndroidApplication(extension: ApplicationExtension) = with(extension) {
    compileSdk = libs.intVersion("compileSdk")
    defaultConfig {
        minSdk = libs.intVersion("minSdk")
        targetSdk = libs.intVersion("targetSdk")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables.useSupportLibrary = true
    }
    compileOptions {
        sourceCompatibility = JAVA_VERSION
        targetCompatibility = JAVA_VERSION
        isCoreLibraryDesugaringEnabled = true
    }
    testOptions.unitTests.isIncludeAndroidResources = true
    testOptions.unitTests.isReturnDefaultValues = true
    configureDesugaring()
    configureTests()
    configureKotlin()
}

internal fun Project.configureAndroidLibrary(extension: LibraryExtension) = with(extension) {
    compileSdk = libs.intVersion("compileSdk")
    defaultConfig {
        minSdk = libs.intVersion("minSdk")
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    compileOptions {
        sourceCompatibility = JAVA_VERSION
        targetCompatibility = JAVA_VERSION
        isCoreLibraryDesugaringEnabled = true
    }
    testOptions.unitTests.isIncludeAndroidResources = true
    testOptions.unitTests.isReturnDefaultValues = true
    configureDesugaring()
    configureTests()
    configureKotlin()
}

/** java.time (streaks, stats by month) must work on API 24-25. */
private fun Project.configureDesugaring() {
    dependencies {
        add("coreLibraryDesugaring", libs.lib("android-desugarJdkLibs"))
    }
}

private fun Project.configureTests() {
    tasks.withType<Test>().configureEach {
        // Robolectric + Compose tests are memory hungry.
        maxHeapSize = "2g"
        // AGP generates unit-test resources even for modules without tests yet.
        failOnNoDiscoveredTests.set(false)
    }
}
