plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "com.zenlauncher.zenmode.mock"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.zenlauncher.zenmode:core-api")
    // MockAppInitializer.kt uses StateFlow/MutableStateFlow - version matches
    // core-private's kotlinx-coroutines-android/play-services (1.7.3), the only
    // other place in this project pinning a coroutines version explicitly.
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}
