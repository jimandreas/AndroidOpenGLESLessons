import java.util.Properties
import kotlin.apply

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) load(file.inputStream())
}

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.kapt)
}


android {
    namespace = "com.learnopengles.android"
    compileSdk = 36


    defaultConfig {
        applicationId = "com.learnopengles.android"
        minSdk = 24
        targetSdk = 36

        versionCode = 201
        versionName = "2.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    signingConfigs {
        val storePassword = localProperties.getProperty("RELEASE_STORE_PASSWORD")
        val keyAlias = localProperties.getProperty("RELEASE_KEY_ALIAS")
        val keyPassword = localProperties.getProperty("RELEASE_KEY_PASSWORD")
        if (storePassword != null && keyAlias != null && keyPassword != null) {
            create("release") {
                storeFile = file("C:/a/j/bammellab/keystoresBammellab.jks")
                this.storePassword = storePassword
                this.keyAlias = keyAlias
                this.keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.findByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        buildConfig = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
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
