plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.ksp)
    alias(libs.plugins.ktlint)
}

android {
    namespace = "com.stproject.client.android"
    compileSdk = 35

    val apiBaseUrlProvider = providers.gradleProperty("ST_API_BASE_URL")
    val defaultCharacterIdProvider = providers.gradleProperty("ST_DEFAULT_CHARACTER_ID")
    val privacyUrlProvider = providers.gradleProperty("ST_PRIVACY_URL")
    val termsUrlProvider = providers.gradleProperty("ST_TERMS_URL")

    defaultConfig {
        applicationId = "com.stproject.client.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "com.stproject.client.android.HiltTestRunner"

        // Default: release-safe. Debug overrides to true.
        manifestPlaceholders["usesCleartextTraffic"] = "false"
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            manifestPlaceholders["usesCleartextTraffic"] = "true"
            val apiBaseUrl =
                apiBaseUrlProvider
                    .orElse("http://10.0.2.2:8080/api/v1/")
                    .get()
            val defaultCharacterId =
                defaultCharacterIdProvider
                    .orElse("")
                    .get()
            val privacyUrl =
                privacyUrlProvider
                    .orElse("https://nea-i.com/privacy")
                    .get()
            val termsUrl =
                termsUrlProvider
                    .orElse("https://nea-i.com/terms")
                    .get()
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            buildConfigField("String", "DEFAULT_CHARACTER_ID", "\"$defaultCharacterId\"")
            buildConfigField("String", "PRIVACY_URL", "\"$privacyUrl\"")
            buildConfigField("String", "TERMS_URL", "\"$termsUrl\"")
        }
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            val apiBaseUrl =
                apiBaseUrlProvider
                    .orElse("__SET_ME__")
                    .get()
            val defaultCharacterId =
                defaultCharacterIdProvider
                    .orElse("")
                    .get()
            val privacyUrl =
                privacyUrlProvider
                    .orElse("__SET_ME__")
                    .get()
            val termsUrl =
                termsUrlProvider
                    .orElse("__SET_ME__")
                    .get()
            buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
            buildConfigField("String", "DEFAULT_CHARACTER_ID", "\"$defaultCharacterId\"")
            buildConfigField("String", "PRIVACY_URL", "\"$privacyUrl\"")
            buildConfigField("String", "TERMS_URL", "\"$termsUrl\"")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    packaging {
        resources {
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.coil.compose)

    // Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // Network
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.okhttp.sse)

    // Database
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    ksp(libs.room.compiler)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Logging
    implementation(libs.timber)
    implementation(libs.androidx.security.crypto)
    implementation(libs.errorprone.annotations)
    implementation(libs.billing.ktx)

    // Unit Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.okhttp.mockwebserver)

    // Instrumentation Testing
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.androidx.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
