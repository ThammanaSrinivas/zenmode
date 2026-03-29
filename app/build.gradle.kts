import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kover)
    alias(libs.plugins.compose.compiler)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

android {
    namespace = "com.zenlauncher.zenmode"
    compileSdk = 35

    buildFeatures {
        buildConfig = true
        compose = true
    }

    defaultConfig {
        applicationId = "com.zenlauncher.zenmode"
        minSdk = 28
        targetSdk = 35
        versionCode = 7
        versionName = "2.0.3"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Inject Web Client ID as a BuildConfig field
        val webClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID") ?: "YOUR_WEB_CLIENT_ID"
        buildConfigField("String", "WEB_CLIENT_ID", "\"$webClientId\"")
    }

    signingConfigs {
        create("release") {
            storeFile = file(localProperties.getProperty("RELEASE_STORE_FILE") ?: "")
            storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
            keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
            keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")

            ndk {
                debugSymbolLevel = "full"
            }
        }
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
    
    val usePrivateCore = rootProject.file("../zenmode_core_private").exists()
    if (usePrivateCore) {
        // Intercepted and built locally via Composite Build (includeBuild in settings)
        runtimeOnly("com.zenlauncher.zenmode:core-private:1.0.0")
    } else {
        // Fallback for public open-source contributors
        runtimeOnly(project(":core-mock"))
    }

    implementation(libs.androidx.work.runtime.ktx)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.runtime.livedata)
    implementation(libs.androidx.activity.compose)
    implementation(libs.coil.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    testImplementation(libs.junit)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
    testImplementation(libs.androidx.core.testing)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.intents)
    debugImplementation("com.squareup.leakcanary:leakcanary-android:2.14")
    implementation("androidx.fragment:fragment-ktx:1.8.6")
}

kover {
    reports {
        variant("debug") {
            filters {
                excludes {
                    classes(
                        "com.zenlauncher.zenmode.*Activity*",
                        "com.zenlauncher.zenmode.*Fragment*",
                        "com.zenlauncher.zenmode.*Adapter*",
                        "com.zenlauncher.zenmode.*ProgressBar*"
                    )
                }
            }
            verify {
                rule {
                    minBound(80) // Fail build if coverage is below 80%
                }
            }
        }
    }
}