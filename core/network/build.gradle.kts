plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.muratcangzm.network"
    compileSdk = ProjectConfig.compileSdk

    defaultConfig {
        minSdk = ProjectConfig.minSdk

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlinOptions {
        compileOptions.sourceCompatibility = JavaVersion.VERSION_21
    }
    kotlin{
        jvmToolchain(21)
    }
}

dependencies {

    implementation(project(":core:data"))
    implementation(project(":core:common"))

    // --- Serialization & Okio ---
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.okio)
    implementation(libs.okhttp)

    implementation(libs.koin.android)
    implementation(libs.koin.androidx.workmanager)
    implementation(libs.androidx.work.runtime.ktx)

    implementation(libs.bundles.advancedDecryption)
    implementation(libs.bundles.advancedNetworkAnalysis)

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}