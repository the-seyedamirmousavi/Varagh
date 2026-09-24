plugins {
    alias(libs.plugins.varagh.android.library)
    alias(libs.plugins.varagh.android.compose)
}

android {
    namespace = "com.mid.varagh.core.designsystem"
}

dependencies {
    api(project(":core:model"))
    implementation(libs.androidx.core.ktx)
    api(libs.coil.compose)
}
