package com.muratcangzm.common.extensions

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

fun Context.isLocationPermissionGranted(): Boolean {
    val granted = this.checkCallingOrSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
    return granted == PackageManager.PERMISSION_GRANTED
}

fun Context.isStoragePermissionGranted(): Boolean {
    return if (Build.VERSION.SDK_INT < 33) {
        val readExternalStorageGranted = this.checkCallingOrSelfPermission(android.Manifest.permission.READ_EXTERNAL_STORAGE)
        val writeExternalStorageGranted = this.checkCallingOrSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
        readExternalStorageGranted == PackageManager.PERMISSION_GRANTED && writeExternalStorageGranted == PackageManager.PERMISSION_GRANTED
    } else {
        val readMediaImagesGranted = this.checkCallingOrSelfPermission(android.Manifest.permission.READ_MEDIA_IMAGES)
        readMediaImagesGranted == PackageManager.PERMISSION_GRANTED
    }
}

fun Context.isNotificationPermissionGranted(): Boolean {
    if (Build.VERSION.SDK_INT < 33) {
        return true
    }
    val granted = this.checkCallingOrSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS)
    return granted == PackageManager.PERMISSION_GRANTED
}

fun Context.isCameraPermissionGranted(): Boolean {
    val cameraPermission = this.checkCallingOrSelfPermission(android.Manifest.permission.CAMERA)
    return cameraPermission == PackageManager.PERMISSION_GRANTED
}

fun Context.isRecordAudioPermissionGranted(): Boolean {
    val recordAudioPermission = this.checkCallingOrSelfPermission(android.Manifest.permission.RECORD_AUDIO)
    return recordAudioPermission == PackageManager.PERMISSION_GRANTED
}