plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}

// Versioning from stackoverflow and Jake Wharton
// https://plus.google.com/+JakeWharton/posts/6f5TcVPRZij
// http://stackoverflow.com/questions/4616095/how-to-get-the-build-version-number-of-your-android-application
val versionMajor = 1
val versionMinor = 0
val versionPatch = 6
val versionBuild = 6 // bump for dogfood builds, public betas, etc.

android {
    namespace = "com.learnopengles.android"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.learnopengles.android"
        minSdk = 21
        targetSdk = 35

        versionCode = versionMajor * 10000 + versionMinor * 1000 + versionPatch * 100 + versionBuild
        versionName = "${versionMajor}.${versionMinor}.${versionPatch}"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.txt"
            )
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
    // Kotlin
    implementation(libs.kotlin.stdlib)

    // Timber
    implementation(libs.timber)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.hamcrest)
    testImplementation(libs.truth)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.espresso.core)
}
