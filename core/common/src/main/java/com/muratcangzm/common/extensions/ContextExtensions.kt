package com.muratcangzm.common.extensions


import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.telephony.TelephonyManager
import android.view.inputmethod.InputMethodManager
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.vectorResource
import androidx.core.content.FileProvider
import androidx.core.location.LocationManagerCompat
import com.muratcangzm.tryOrLog
import java.io.File
import java.util.Locale

fun Context.openNotificationSettings() {
    val intent = Intent()
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
            intent.action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
            intent.putExtra(Settings.EXTRA_APP_PACKAGE, this.packageName)
        }

        else -> {
            intent.action = "android.settings.APP_NOTIFICATION_SETTINGS"
            intent.putExtra("app_package", this.packageName)
            intent.putExtra("app_uid", this.applicationInfo.uid)
        }
    }
    tryOrLog { this.startActivity(intent) }
}

fun Context.openSettings() {
    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
    val uri = Uri.fromParts("package", packageName, null)
    intent.data = uri
    tryOrLog { startActivity(intent) }
}

fun Context.openGooglePlay() {
    val appPackageName = packageName
    try {
        this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
    } catch (_: ActivityNotFoundException) {
        this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
    }
}

fun Context.shareApp(onSuccess: () -> Unit) {
    val sendIntent = Intent()
    sendIntent.action = Intent.ACTION_SEND
    sendIntent.putExtra(Intent.EXTRA_TEXT, "https://play.google.com/store/apps/details?id=$packageName")
    sendIntent.type = "text/plain"
    tryOrLog {
        this.startActivity(sendIntent)
        onSuccess()
    }
}

fun Context.openGooglePlay(appPackageName: String) {
    try {
        this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$appPackageName")))
    } catch (_: ActivityNotFoundException) {
        this.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$appPackageName")))
    }
}

fun Context.openBrowser(url: String) {
    val intent = Intent(Intent.ACTION_VIEW)
    intent.data = Uri.parse(url)
    tryOrLog { this.startActivity(intent) }
}

fun Context.sendEmail(title: String, subject: String? = null, body: String? = null, address: String) {
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:")
        putExtra(Intent.EXTRA_EMAIL, arrayOf(address))
        subject?.let { putExtra(Intent.EXTRA_SUBJECT, it) }
        body?.let { putExtra(Intent.EXTRA_TEXT, it) }
    }
    tryOrLog { startActivity(Intent.createChooser(intent, title)) }
}

fun Context.isLocationEnabled(): Boolean {
    val locationManager = this.getSystemService(Context.LOCATION_SERVICE) as LocationManager
    return LocationManagerCompat.isLocationEnabled(locationManager)
}

fun Context.getVideoUriFromRaw(resId: Int): Uri = Uri.parse("android.resource://$packageName/$resId");

fun Context.getUserCountry(): String {
    try {
        val tm: TelephonyManager = this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var countryIso: String = tm.simCountryIso

        if (countryIso.length == 2) { // SIM country code is available
            countryIso.lowercase(Locale.US)
        } else if (tm.phoneType != TelephonyManager.PHONE_TYPE_CDMA) { // device is not 3G (would be unreliable)
            val networkCountry: String = tm.networkCountryIso
            if (networkCountry.length == 2) { // network country code is available
                countryIso = networkCountry.lowercase(Locale.US)
            }
        }
        for (countryCode in countryIso) {
            val locale = Locale("", countryIso)
            val countryName = locale.displayCountry
            return countryName.substring(0, 1).uppercase() + countryName.substring(1).lowercase()
        }
    } catch (e: Exception) {
        return Locale.getDefault().displayCountry
    }
    return Locale.getDefault().displayCountry
}

fun Context.getUserCountryIso(): String {
    return try {
        val tm: TelephonyManager = this.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        var countryIso: String = tm.simCountryIso
        if (countryIso.length == 2) {
            countryIso.lowercase(Locale.US)
        } else if (tm.phoneType != TelephonyManager.PHONE_TYPE_CDMA) { // device is not 3G (would be unreliable)
            val networkCountry: String = tm.networkCountryIso
            if (networkCountry.length == 2) {
                countryIso = networkCountry.lowercase(Locale.US)
            }
        }
        if (countryIso.isEmpty()) {
            countryIso = Locale.getDefault().country.lowercase(Locale.US)
        }
        countryIso
    } catch (_: Exception) {
        Locale.getDefault().country.lowercase(Locale.US)
    }
}

fun Context.createTempPictureUri(
    provider: String = "WiredEye.provider",
    fileName: String = "picture_${System.currentTimeMillis()}",
    fileExtension: String = ".jpg",
    directoryName: String? = null,
): Uri {
    val directoryFile = directoryName?.let { File(cacheDir, directoryName) } ?: cacheDir
    if (directoryFile.exists().not()) directoryFile.mkdir()
    val tempFile = File.createTempFile(fileName, fileExtension, directoryFile).apply { createNewFile() }
    return FileProvider.getUriForFile(applicationContext, provider, tempFile)
}

fun Context.openLanguageSettings() {
    try {
        val intent = Intent(Settings.ACTION_LOCALE_SETTINGS)
        startActivity(intent)
    } catch (_: ActivityNotFoundException) {
        val fallbackIntent = Intent(Settings.ACTION_SETTINGS)
        startActivity(fallbackIntent)
    }
}

fun Context.vectorResource(@DrawableRes id: Int): ImageVector? {
    val theme = this.theme
    return try {
        ImageVector.vectorResource(theme = theme, resId = id, res = this.resources)
    } catch (_: Exception) {
        null
    }
}

fun Context.openInputMethodSettings() {
    val intent = Intent(Settings.ACTION_INPUT_METHOD_SETTINGS)
        .apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
    tryOrLog { this.startActivity(intent) }
}

fun Context.showInputMethodPicker() {
    val imeManager = this.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
    imeManager.showInputMethodPicker()
}

fun Context.isInputMethodEnabled(): Boolean {
    try {
        val imm = getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        val enabledInputMethods = imm.enabledInputMethodList
        return enabledInputMethods.any { it.packageName == this.packageName }
    } catch (_: Exception) {
        return false
    }
}

fun Context.isInputMethodSelected(): Boolean {
    val defaultMethod = Settings.Secure.getString(
        this.contentResolver,
        Settings.Secure.DEFAULT_INPUT_METHOD
    )
    return defaultMethod?.contains(this.packageName) == true
}


@Suppress("NOTHING_TO_INLINE")
@Composable
inline fun vectorResource(@DrawableRes id: Int): ImageVector? {
    val context = LocalContext.current
    return context.vectorResource(id)
}

