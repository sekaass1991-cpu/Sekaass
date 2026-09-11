plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.owner.assistant"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.owner.assistant"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "0.2"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
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
        viewBinding = false
    }

    packaging {
        resources.excludes.add("META-INF/*")
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.12.0")
    implementation("androidx.lifecycle:lifecycle-service:2.8.4")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.4")

    // Reliable alarm/reminder + periodic background work (DND schedule, battery scan)
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // Encrypted local storage for the voice profile, emergency contact, blocklist prefs
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Camera capture on voice command
    implementation("androidx.camera:camera-core:1.3.4")
    implementation("androidx.camera:camera-camera2:1.3.4")
    implementation("androidx.camera:camera-lifecycle:1.3.4")
    implementation("androidx.camera:camera-video:1.3.4")

    // On-device translation + language ID (Translator feature).
    // Models download on first use over Wi-Fi; see README for offline model management.
    implementation("com.google.mlkit:translate:17.0.3")
    implementation("com.google.mlkit:language-id:17.0.6")
}
