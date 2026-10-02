plugins {
    alias(libs.plugins.varagh.android.feature)
}

android {
    namespace = "com.mid.varagh.feature.social"
}

dependencies {
    // Remote avatars (server-only screens).
    implementation(libs.coil.network.okhttp)
}
