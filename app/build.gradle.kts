import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kover)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.paparazzi)
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localProperties.load(FileInputStream(localPropertiesFile))
}

// File search kill switch. With `zenmode.fileSearch=false` the marked block in
// AndroidManifest.xml is dropped before the merge, so the built APK declares no storage
// permissions and the Files row disappears from search. One property, no code changes.
val fileSearchEnabled =
    (project.findProperty("zenmode.fileSearch") as String?)?.toBoolean() ?: true

android {
    namespace = "com.zenlauncher.zenmode"
    compileSdk = 36

    buildFeatures {
        buildConfig = true
        compose = true
    }

    defaultConfig {
        applicationId = "com.zenlauncher.zenmode"
        minSdk = 28
        targetSdk = 36
        versionCode = 11
        versionName = "3.01"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        
        // Inject Web Client ID as a BuildConfig field
        val webClientId = localProperties.getProperty("GOOGLE_WEB_CLIENT_ID") ?: "YOUR_WEB_CLIENT_ID"
        buildConfigField("String", "WEB_CLIENT_ID", "\"$webClientId\"")

        // TEMP: Kite basket-redirect spike (gold-streak instrument) — see KiteBasketActivity.
        val kiteApiKey = localProperties.getProperty("KITE_API_KEY") ?: ""
        buildConfigField("String", "KITE_API_KEY", "\"$kiteApiKey\"")

        buildConfigField("boolean", "FILE_SEARCH_ENABLED", fileSearchEnabled.toString())
    }

    if (!fileSearchEnabled) {
        val stripped = layout.buildDirectory
            .file("generated/nofilesearch/AndroidManifest.xml").get().asFile
        stripped.parentFile.mkdirs()
        stripped.writeText(
            providers.fileContents(
                layout.projectDirectory.file("src/main/AndroidManifest.xml")
            ).asText.get().replace(
                Regex("(?s)\\s*<!-- file-search:start -->.*?<!-- file-search:end -->"), ""
            )
        )
        sourceSets.getByName("main").manifest.srcFile(stripped)
    }

    signingConfigs {
        create("release") {
            // local.properties is gitignored (machine-local secrets) and absent in CI -
            // only set these when actually present, so project configuration doesn't
            // crash for tasks (like `check`) that never touch release signing at all.
            // A real release build still fails clearly, later, if these are missing.
            val storeFilePath = localProperties.getProperty("RELEASE_STORE_FILE")
            if (!storeFilePath.isNullOrBlank()) {
                storeFile = file(storeFilePath)
                storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
                keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
                keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
            }
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
    implementation(libs.androidx.compose.material.icons.extended)
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
    testImplementation(libs.kotlinx.coroutines.test)
    // Real org.json impl so JVM unit tests can exercise JSON parsing (android.jar stub throws).
    testImplementation("org.json:json:20240303")
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.espresso.intents)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    androidTestImplementation(libs.androidx.test.rules)
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
                        "com.zenlauncher.zenmode.*ProgressBar*",
                        "com.zenlauncher.zenmode.*Service*",
                        "com.zenlauncher.zenmode.ui.screens.ContentBlockingBottomSheetKt"
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

// SOURCE OF TRUTH: design tokens. Real values live in res/values/colors.xml and
// ui/theme/Color.kt/Type.kt — never a bare Color(0x...) literal or #RRGGBB string elsewhere.
// Fails ./gradlew check if a new one shows up outside ui/theme/, so the AI/human writing the
// code sees a build error pointing at ZenTheme.colors instead of relying on remembering this
// rule from CLAUDE.md every time.
val legacyColorDebt: Map<String, Int> = file("config/legacy-color-debt.txt")
    .takeIf { it.exists() }
    ?.readLines()
    ?.map { it.trim() }
    ?.filter { it.isNotEmpty() && !it.startsWith("#") }
    ?.associate { line ->
        val (path, count) = line.split(":").let { it[0] to it[1].toInt() }
        path to count
    }
    ?: emptyMap()

tasks.register("checkSourceOfTruth") {
    group = "verification"
    description = "Fails if a hardcoded color literal appears outside ui/theme/ beyond the legacy-color-debt allowlist."
    // Resolved at configuration time, not inside doLast: the configuration cache can't
    // serialize a reference to the build script object, which is exactly what calling
    // file(...) or reading a script-level val from inside doLast would capture.
    val srcRoot = file("src/main/java")
    val debtSnapshot: Map<String, Int> = legacyColorDebt
    doLast {
        val hexPattern = Regex("""Color\(0x[0-9A-Fa-f]{6,8}\)|#[0-9A-Fa-f]{6}""")
        val countsByRelPath = mutableMapOf<String, MutableList<String>>()

        srcRoot.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { !it.path.replace('\\', '/').contains("/ui/theme/") }
            .forEach { file ->
                val relPath = file.relativeTo(srcRoot).path.replace('\\', '/')
                file.readLines().forEachIndexed { idx, line ->
                    if (hexPattern.containsMatchIn(line)) {
                        countsByRelPath.getOrPut(relPath) { mutableListOf() }.add("${idx + 1}: ${line.trim()}")
                    }
                }
            }

        val violations = mutableListOf<String>()
        countsByRelPath.forEach { (relPath, hits) ->
            val allowed = debtSnapshot[relPath] ?: 0
            if (hits.size > allowed) {
                violations += "$relPath: ${hits.size} found, $allowed allowed:\n" +
                    hits.joinToString("\n") { "    $it" }
            }
        }

        if (violations.isNotEmpty()) {
            throw GradleException(
                "checkSourceOfTruth failed: hardcoded color literal(s) outside ui/theme/.\n" +
                    "Use ZenTheme.colors (see ui/theme/Color.kt) instead — grep for " +
                    "'SOURCE OF TRUTH:' to find where the real values live.\n\n" +
                    violations.joinToString("\n\n") +
                    "\n\nIf this is deliberate, tracked legacy debt (a screen not yet migrated " +
                    "to v3), update its count in app/config/legacy-color-debt.txt."
            )
        }
    }
}

tasks.named("check") {
    dependsOn("checkSourceOfTruth")
    // verifyPaparazzi is the Paparazzi plugin's screenshot-diff task (name confirmed
    // against the installed plugin version - not verified from this environment).
    // Wired explicitly since some Paparazzi versions don't auto-attach to `check`
    // for com.android.application modules. Baselines: run `recordPaparazzi` once,
    // review the generated PNGs under app/src/test/snapshots/, then commit them.
    dependsOn("verifyPaparazzi")
}