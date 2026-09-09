plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}
android {
    namespace = "com.aviad.sidur"
    compileSdk = 34
    defaultConfig {
        applicationId = "com.aviad.sidur"
        minSdk = 26
        targetSdk = 34
        val run = (System.getenv("GITHUB_RUN_NUMBER") ?: "1").toInt()
        versionCode = run
        versionName = "1.$run"
    }
    signingConfigs {
        create("sidur") {
            storeFile = file("sidur.jks")
            storePassword = "sidur2026"
            keyAlias = "sidur"
            keyPassword = "sidur2026"
        }
    }
    buildTypes {
        release { isMinifyEnabled = false; signingConfig = signingConfigs.getByName("sidur") }
        debug { signingConfig = signingConfigs.getByName("sidur") }
    }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies {
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.webkit:webkit:1.11.0")
    implementation("androidx.core:core-ktx:1.13.1")
}
