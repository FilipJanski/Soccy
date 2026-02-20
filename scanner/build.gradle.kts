@file:Suppress("DEPRECATION")

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}


android {
    namespace = "com.example.threedscanner"
    compileSdk = 36

    defaultConfig {
        minSdk = 26
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables { useSupportLibrary = true }
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
    kotlin {
        jvmToolchain(17)
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

}
kotlin {
    jvmToolchain(17)
}


dependencies {
    implementation(libs.androidx.core.ktx.v1170)
    implementation(libs.androidx.lifecycle.runtime.ktx.v2100)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose.v1124)
    implementation(platform(libs.androidx.compose.bom.v20260200))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    
    // ARCore
    //noinspection Aligned16KB
    implementation(libs.core)

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit.v130)
    androidTestImplementation(libs.androidx.espresso.core.v370)
    androidTestImplementation(platform(libs.androidx.compose.bom.v20260200))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    //noinspection UseTomlInstead
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.androidx.camera.extensions)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
