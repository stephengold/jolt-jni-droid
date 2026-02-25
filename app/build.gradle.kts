// Gradle script to build the JoltJNI-on-Android test app

plugins {
    alias(libs.plugins.android.application) // to build Android libraries
}

android {
    namespace = "com.github.stephengold.joltjni.droid"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.github.stephengold.joltjni.droid"
        minSdk = 33
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
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
    buildFeatures {
        viewBinding = true
    }
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
