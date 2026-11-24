plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "tv.nani.launcher"
    compileSdk = 34

    defaultConfig {
        applicationId = "tv.nani.launcher"
        minSdk = 23
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Static config — GitHub repo ka raw JSON. Edit + commit = list update.
        buildConfigField("String", "CONFIG_URL", "\"https://raw.githubusercontent.com/VishalPhirkoj/nani-tv-config/refs/heads/main/config.json\"")
    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        release {
            // Sirf ghar ke TV pe sideload hota hai — Play Store nahi. Debug key se
            // sign karne se `adb install -r` debug build ke upar bhi chal jata hai.
            signingConfig = signingConfigs.getByName("debug")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
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
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("androidx.activity:activity-ktx:1.9.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.3")
    implementation("io.coil-kt:coil:2.6.0")
}
