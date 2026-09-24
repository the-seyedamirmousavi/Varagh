package com.mid.varagh.buildlogic

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
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
    }
    testOptions.unitTests.isIncludeAndroidResources = true
    testOptions.unitTests.isReturnDefaultValues = true
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
    }
    testOptions.unitTests.isIncludeAndroidResources = true
    testOptions.unitTests.isReturnDefaultValues = true
    configureTests()
    configureKotlin()
}

private fun Project.configureTests() {
    tasks.withType<Test>().configureEach {
        // Robolectric + Compose tests are memory hungry.
        maxHeapSize = "2g"
        // AGP generates unit-test resources even for modules without tests yet.
        failOnNoDiscoveredTests.set(false)
    }
}
