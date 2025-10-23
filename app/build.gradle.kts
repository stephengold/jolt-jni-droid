plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.github.stephengold.joltjni.droid"
    compileSdk = 35

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
    implementation("com.github.stephengold", "jolt-jni-Android", "3.4.0", "", "SpDebug", "aar")
}