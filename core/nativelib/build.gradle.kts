plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.muratcangzm.core.nativelib"
    compileSdk {
        version = release(ProjectConfig.compileSdk)
    }

    defaultConfig {
        minSdk = ProjectConfig.minSdk

        externalNativeBuild {
            cmake {
                cppFlags += listOf("-std=c++20", "-O2", "-fvisibility=hidden")
            }
        }
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
        consumerProguardFiles("consumer-rules.pro")
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = libs.versions.cmake.get()
        }
    }

    ndkVersion = libs.versions.ndk.get()

    buildTypes {
        debug { ndk { debugSymbolLevel = "FULL" } }
        release { ndk { debugSymbolLevel = "FULL" } }
    }

    packaging {
        jniLibs { useLegacyPackaging = false }
        resources {
            excludes += setOf(
                "/META-INF/{AL2.0,LGPL2.1}",
                "/META-INF/LICENSE*",
                "/META-INF/NOTICE*"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin { jvmToolchain(21) }
}

dependencies {

    implementation(project(":core:shared"))

    // Koin
    implementation(libs.koin.android)

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.serialization.core)
}