import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

/**
 * Backend configuration is read from the (git-ignored) local.properties file so
 * that no project identifiers land in the repository. Only values that are
 * safe to ship inside a public mobile app belong here (Firebase project id,
 * app id, API key, Google web client id, push Worker URL); access is enforced by Firestore
 * security rules. The Firebase service-account key must NEVER be placed here;
 * it lives only in the push Worker's secrets. See docs/ARCHITECTURE.md ->
 * "Configuration".
 */
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use { load(it) }
}

fun localConfig(key: String): String =
    (localProperties.getProperty(key) ?: "").replace("\"", "\\\"")

/**
 * Release versions come from git: versionCode = number of commits, so every
 * build from a newer commit can install over the last one (Android refuses
 * a lower or equal versionCode). Falls back to 1 if git isn't available.
 */
val commitCount: Int = runCatching {
    providers.exec {
        commandLine("git", "rev-list", "--count", "HEAD")
        isIgnoreExitValue = true
    }.standardOutput.asText.get().trim().toInt()
}.getOrDefault(1)

/** Release signing key, from local.properties only (see docs/RELEASING.md). Never committed. */
val releaseStoreFile: String? = localProperties.getProperty("choomhub.release.storeFile")?.takeIf { it.isNotBlank() }

android {
    namespace = "com.choomhub.app"
    compileSdk {
        version = release(libs.versions.compileSdk.get().toInt()) {
            minorApiLevel = libs.versions.compileSdkMinor.get().toInt()
        }
    }

    defaultConfig {
        applicationId = "com.choomhub.app"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = commitCount
        versionName = "1.0.$commitCount"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${localConfig("choomhub.firebase.projectId")}\"")
        buildConfigField("String", "FIREBASE_APP_ID", "\"${localConfig("choomhub.firebase.appId")}\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"${localConfig("choomhub.firebase.apiKey")}\"")
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${localConfig("choomhub.firebase.webClientId")}\"")
        buildConfigField("String", "PUSH_WORKER_URL", "\"${localConfig("choomhub.push.workerUrl")}\"")
    }

    signingConfigs {
        if (releaseStoreFile != null) {
            create("release") {
                storeFile = file(releaseStoreFile)
                storePassword = localProperties.getProperty("choomhub.release.storePassword")
                keyAlias = localProperties.getProperty("choomhub.release.keyAlias")
                keyPassword = localProperties.getProperty("choomhub.release.keyPassword")
            }
        }
    }

    buildTypes {
        release {
            // Unsigned (and so not installable) until the release key is set in local.properties.
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    testOptions {
        unitTests {
            // Needed so Robolectric UI tests can resolve resources and the manifest.
            isIncludeAndroidResources = true
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.androidx.datastore.preferences)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.exifinterface)
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.material3)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(libs.hilt.android.testing)
    kspTest(libs.hilt.compiler)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.test.ext.junit)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
