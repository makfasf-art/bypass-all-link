plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.modlink.app"
    compileSdk = 35
    defaultConfig {
        applicationId = "com.modlink.app"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
}
dependencies {
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
}
