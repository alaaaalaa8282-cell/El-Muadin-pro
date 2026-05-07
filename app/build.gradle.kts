import java.util.Properties
import java.io.FileInputStream

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // the Google services Gradle plugin
    //id("com.google.gms.google-services")

    // Compose plugin (Kotlin 2.0.21)
    alias(libs.plugins.kotlin.compose)

    // Hilt plugin
    alias(libs.plugins.hilt)

    // kapt
    // kapt
    id("org.jetbrains.kotlin.kapt")
    
    // Serialization
    // Use the same version as Kotlin
    id("org.jetbrains.kotlin.plugin.serialization") version "2.0.21"
}


android {
    namespace = "com.AbuAlaa"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.AbuAlaa"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val keystoreFile = rootProject.file("keystore.properties")
            if (keystoreFile.exists()) {
                val props = Properties()
                props.load(FileInputStream(keystoreFile))
                val storeFilePath = props["storeFile"] as String?
                if (storeFilePath != null && file(storeFilePath).exists()) {
                    storeFile = file(storeFilePath)
                    storePassword = props["storePassword"] as String
                    keyAlias = props["keyAlias"] as String
                    keyPassword = props["keyPassword"] as String
                }
            }
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    // Compose
    buildFeatures {
        compose = true
    }

    // If you removed org.jetbrains.kotlin.plugin.compose, uncomment this block:
    // composeOptions {
    //     kotlinCompilerExtensionVersion = "1.5.14"
    // }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    // Compose BOM
    implementation(platform("androidx.compose:compose-bom:2024.10.00"))
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.runtime:runtime-saveable")
    implementation("androidx.compose.animation:animation")
    implementation("androidx.compose.foundation:foundation")
    debugImplementation("androidx.compose.ui:ui-tooling")
    implementation("androidx.compose.material3:material3")
    implementation(libs.material)
    
      // Import the Firebase BoM
 // implementation(platform("com.google.firebase:firebase-bom:34.8.0"))
  // TODO: Add the dependencies for Firebase products you want to use
  // When using the BoM, don't specify versions in Firebase dependencies
// implementation("com.google.firebase:firebase-analytics")
 // implementation("com.google.firebase:firebase-auth")
  // implementation("com.google.firebase:firebase-database")
  // Add the dependencies for any other desired Firebase products
  // https://firebase.google.com/docs/android/setup#available-libraries

    // Navigation Compose
    implementation("androidx.navigation:navigation-compose:2.8.4")

    // Lifecycle (optional but recommended)
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.8.7")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.52")
    kapt("com.google.dagger:hilt-compiler:2.52")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")


    // Core
    implementation("androidx.core:core-ktx:1.15.0")

    // Material Icons
    implementation("androidx.compose.material:material-icons-extended")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.1")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.1")

    // Networking (Retrofit + Moshi)
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.11.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    kapt("com.squareup.moshi:moshi-kotlin-codegen:1.15.1") // Keep using kapt for Moshi
    implementation("com.squareup.okhttp3:okhttp-sse:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // WorkManager (optional but great for background refresh)
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    // Room Database (for offline caching)
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
}
