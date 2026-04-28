import java.util.Properties
import org.gradle.api.JavaVersion

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlinx.serialization)
    id("kotlin-parcelize")
}

val versionCode = 1
val versionName = "1.0"

android {
    namespace = "io.github.kowx712.mmuautoqr"
    compileSdk = 36

    val keystoreProperties = Properties().apply {
        load(rootProject.file("keystore.properties").inputStream())
    }

    defaultConfig {
        applicationId = "io.github.kowx712.mmuautoqr"
        minSdk = 26
        targetSdk = 36
        versionCode = versionCode
        versionName = versionName
    }

    signingConfigs {
        create("release") {
            storeFile = file(keystoreProperties["KEY_STORE_FILE"] as String)
            storePassword = keystoreProperties["KEY_STORE_PASSWORD"] as String
            keyAlias = keystoreProperties["KEY_ALIAS"] as String
            keyPassword = keystoreProperties["KEY_PASSWORD"] as String
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    buildFeatures {
        compose = true
    }

    compileSdkMinor = 1
}

base {
    archivesName.set("Attendance_Pro_max_$versionName-$versionCode")
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.webkit)
    implementation(libs.guava)
    implementation(libs.androidx.swiperefreshlayout)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    debugImplementation(libs.androidx.ui.tooling)

    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.play.services.mlkit.barcode.scanning)

    implementation(libs.kotlinx.serialization.json)
}
