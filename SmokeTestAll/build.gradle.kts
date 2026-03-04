// Gradle script to build the SmokeTestAll test app for Android

plugins {
    alias(libs.plugins.android.application) // to build Android libraries
}

android {
    buildFeatures {
        viewBinding = true
    }
    buildTypes {
        debug {
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    compileSdk = 36
    defaultConfig {
        applicationId = "com.github.stephengold.joltjni.droidsta"
        minSdk = 33 // in order to use Jolt JNI
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }
    namespace = "com.github.stephengold.joltjni.droidsta"
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation("com.github.stephengold", "jolt-jni-Android",
            libs.versions.joltjni.get(), "", "SpDebug", "aar"
    )
}
