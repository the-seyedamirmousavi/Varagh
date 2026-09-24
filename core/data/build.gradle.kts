plugins {
    alias(libs.plugins.varagh.android.library)
    alias(libs.plugins.varagh.hilt)
    alias(libs.plugins.kotlin.serialization)
}

// ---------------------------------------------------------------------------------------------
// THE BACKEND SWITCH
//
// USE_REMOTE_BACKEND=false (default): every repository is the Room-backed Local… implementation,
// the app is fully offline, and all server-only UI (login, social feed, public profiles, follow)
// is hidden.
// USE_REMOTE_BACKEND=true: the Remote… repositories (Retrofit + Room cache) are bound instead and
// the server-only UI appears. No code changes are needed, only a rebuild and a real server.
//
// Change the default below, or override without editing: ./gradlew assembleDebug -Pvaragh.useRemoteBackend=true
// ---------------------------------------------------------------------------------------------
val useRemoteBackend: String = providers.gradleProperty("varagh.useRemoteBackend").getOrElse("false")
val apiBaseUrl: String = providers.gradleProperty("varagh.apiBaseUrl").getOrElse("https://api.example.com/v1/")

android {
    namespace = "com.mid.varagh.core.data"

    defaultConfig {
        buildConfigField("boolean", "USE_REMOTE_BACKEND", useRemoteBackend)
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    api(project(":core:domain"))
    implementation(project(":core:model"))
    implementation(project(":core:database"))
    implementation(project(":core:network"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.datastore.preferences)

    // Repository tests run against a real in-memory Room database.
    testImplementation(libs.room.runtime)
    testImplementation(libs.room.ktx)
}
