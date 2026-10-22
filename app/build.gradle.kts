plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}
android {
    namespace = "dev.profilebridge.app"
    compileSdk = 37
    defaultConfig {
        applicationId = "dev.profilebridge.app"
        minSdk = 30
        targetSdk = 37
        versionCode = 26
        versionName = "0.4.1-stable-signing"
    }
    buildFeatures { compose = true; buildConfig = true }
    val releaseStore = providers.environmentVariable("PLB_RELEASE_KEYSTORE_PATH").orNull
    if (releaseStore != null) {
        signingConfigs.create("persistentRelease") {
            storeFile = file(releaseStore)
            storePassword = providers.environmentVariable("PLB_RELEASE_PASSWORD").get()
            keyAlias = "profile-bridge-release"
            keyPassword = providers.environmentVariable("PLB_RELEASE_PASSWORD").get()
            storeType = "PKCS12"
        }
    }
    buildTypes {
        release {
            if (releaseStore != null) signingConfig = signingConfigs.getByName("persistentRelease")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    lint { abortOnError = true; warningsAsErrors = true }
}
kotlin { compilerOptions { allWarningsAsErrors.set(true) } }
dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.coroutines.android)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
}
