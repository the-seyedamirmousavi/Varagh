import java.util.Properties

plugins {
    alias(libs.plugins.varagh.android.application)
    alias(libs.plugins.varagh.android.compose)
    alias(libs.plugins.varagh.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.mid.varagh"

    defaultConfig {
        applicationId = "com.mid.varagh"
        versionCode = 1
        versionName = "1.0.0"
    }

    // Release signing comes from keystore.properties (git-ignored), see README "Release builds".
    val keystoreFile = rootProject.file("keystore.properties")
    val releaseSigning = if (keystoreFile.isFile) {
        val props = Properties().apply { keystoreFile.inputStream().use(::load) }
        signingConfigs.create("release") {
            storeFile = rootProject.file(props.getProperty("storeFile"))
            storePassword = props.getProperty("storePassword")
            keyAlias = props.getProperty("keyAlias")
            keyPassword = props.getProperty("keyPassword")
        }
    } else {
        logger.warn("keystore.properties not found: release build is signed with the DEBUG key (not for Play upload).")
        signingConfigs.getByName("debug")
    }

    androidResources {
        // Only ship the locales we translate (Persian is the default `values/`).
        localeFilters += listOf("fa", "en")
        generateLocaleConfig = true
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = releaseSigning
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))

    implementation(project(":feature:library"))
    implementation(project(":feature:reader"))
    implementation(project(":feature:history"))
    implementation(project(":feature:profile"))
    implementation(project(":feature:social"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.work.runtime)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.androidx.navigation.testing)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.espresso.core)
}

// The network permission exists only in remote-backend builds.
val useRemoteBackend = providers.gradleProperty("varagh.useRemoteBackend").getOrElse("false").toBoolean()
androidComponents {
    onVariants { variant ->
        if (useRemoteBackend) variant.sources.manifests.addStaticManifestFile("src/remote/AndroidManifest.xml")
    }
}
