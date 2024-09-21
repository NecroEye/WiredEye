buildscript {
    repositories {
        google()
        mavenCentral()
        maven { url = uri("https://www.jitpack.io") }
    }
    dependencies{
        classpath("com.google.dagger:hilt-android-gradle-plugin:${libs.versions.hilt}")
        classpath("com.google.gms:google-services:${libs.versions.gms}")
        classpath("com.google.firebase:firebase-crashlytics-gradle:${libs.versions.firebase.crashlytics}")
        classpath("com.google.devtools.ksp:com.google.devtools.ksp.gradle.plugin:${libs.versions.ksp}")
    }
}

plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.gms) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.library) apply false
}

task("clean", type = Delete::class) {
    delete(rootProject.buildDir)
}
