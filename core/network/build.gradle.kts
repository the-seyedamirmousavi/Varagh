plugins {
    alias(libs.plugins.varagh.android.library)
    alias(libs.plugins.varagh.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.mid.varagh.core.network"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)
    api(libs.retrofit.core)
    implementation(libs.retrofit.kotlinx.serialization)
    api(libs.okhttp.core)
    implementation(libs.okhttp.logging)
}
