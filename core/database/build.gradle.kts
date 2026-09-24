plugins {
    alias(libs.plugins.varagh.android.library)
    alias(libs.plugins.varagh.hilt)
    alias(libs.plugins.varagh.room)
}

android {
    namespace = "com.mid.varagh.core.database"
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.android)
}
