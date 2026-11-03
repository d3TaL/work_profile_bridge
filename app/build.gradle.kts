plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.roborazzi)
}
android {
    namespace = "dev.profilebridge.app"
    compileSdk = 37
    defaultConfig {
        applicationId = "dev.profilebridge.app"
        minSdk = 30
        targetSdk = 37
        versionCode = 32
        versionName = "0.5.3-back-insets"
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
    testOptions { unitTests { isIncludeAndroidResources = true } }
    lint { abortOnError = true; warningsAsErrors = true }
}
kotlin { compilerOptions { allWarningsAsErrors.set(true) } }

// Robolectric must not download the Android framework jar at test time. Gradle resolves it,
// so it is covered by the lockfile and SHA-256 verification metadata, and tests run offline.
val robolectricRuntime: Configuration = configurations.create("robolectricRuntime") {
    isCanBeConsumed = false
    isTransitive = false
}
val robolectricJars = tasks.register<Sync>("robolectricJars") {
    from(robolectricRuntime)
    into(layout.buildDirectory.dir("robolectric-jars"))
}
val robolectricJarDir = layout.buildDirectory.dir("robolectric-jars").get().asFile.absolutePath
val testLauncher = extensions.getByType<JavaToolchainService>().launcherFor {
    // Robolectric 4.17 needs Java 21 for SDK 36+; the APK is still compiled with JDK 17.
    languageVersion.set(JavaLanguageVersion.of(21))
}
tasks.withType<Test>().configureEach {
    dependsOn(robolectricJars)
    javaLauncher.set(testLauncher)
    maxHeapSize = "2g"
    systemProperty("robolectric.offline", "true")
    systemProperty("robolectric.dependency.dir", robolectricJarDir)
    jvmArgs(
        "--add-opens=java.base/java.lang=ALL-UNNAMED",
        "--add-opens=java.base/java.util=ALL-UNNAMED",
        "--add-opens=java.base/java.io=ALL-UNNAMED",
        "--add-opens=java.base/java.net=ALL-UNNAMED",
        "--add-opens=java.base/java.security=ALL-UNNAMED",
        "--add-opens=java.base/java.text=ALL-UNNAMED",
        "--add-opens=java.base/jdk.internal.access=ALL-UNNAMED",
        "--add-opens=java.desktop/java.awt.font=ALL-UNNAMED",
        "--add-opens=jdk.compiler/com.sun.tools.javac.api=ALL-UNNAMED",
    )
}
roborazzi {
    // Reference images are reviewed and committed; diffs stay in build/outputs/roborazzi.
    outputDir.set(file("src/test/screenshots"))
}
dependencies {
    implementation(project(":core"))
    implementation(libs.androidx.core)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.lifecycle.runtime)
    implementation(libs.coroutines.android)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.material3)
    testImplementation(libs.junit)
    testImplementation(platform(libs.compose.bom))
    testImplementation(libs.compose.ui.test.junit4)
    // Compose test pulls Espresso 3.5.1, which reflects on InputManager.getInstance() that SDK 37 lacks.
    testImplementation(libs.espresso.core)
    testImplementation(libs.robolectric)
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    add(robolectricRuntime.name, libs.robolectric.android.all)
}
