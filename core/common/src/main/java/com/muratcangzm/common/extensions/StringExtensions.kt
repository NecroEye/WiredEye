package com.muratcangzm.common.extensions

import android.net.Uri
import android.util.Patterns
import com.muratcangzm.tryOrNull
import com.muratcangzm.utils.StringUtils
import java.io.File
import java.util.Locale
import java.util.regex.Pattern

fun String?.asUri(): Uri? = tryOrNull<Uri?> { return Uri.parse(this) }

fun String.removeWhiteSpaces(): String {
    return this.replace("\\s".toRegex(), "")
}

fun String.lenEqOrGt(length: Int): Boolean = this.length >= length

fun String.isEmailValid() = this.isNotEmpty() && Patterns.EMAIL_ADDRESS.matcher(this).matches()

fun String.toVersionNumber(): Int = this.replace(".", "").toInt()

fun String?.withMarkdownLineBreaks(): String? = this?.replace(StringUtils.SOFT_BREAK, StringUtils.HARD_BREAK)

fun String.capitalized(): String {
    return this.replaceFirstChar {
        if (it.isLowerCase())
            it.titlecase(Locale.getDefault())
        else it.toString()
    }
}

fun String.toAspectRatio(): Float? {
    val parts = this.split(":")
    return if (parts.size == 2) {
        val width = parts[0].toFloatOrNull()
        val height = parts[1].toFloatOrNull()
        if (width != null && height != null && height != 0f) {
            width / height
        } else null
    } else null
}

fun String.getProductPrice(): String {
    val nonDigitCount = this.count { it.isDigit().not() }
    var currentNonDigitCount = 0
    val priceBuilder = StringBuilder()
    for (i in this.indices) {
        if (this[i].isDigit()) {
            priceBuilder.append(this[i])
        } else {
            currentNonDigitCount++
            if (currentNonDigitCount == nonDigitCount) {
                if (this[i] == '.' || this[i] == ',') {
                    priceBuilder.append('.')
                }
            }
        }
    }
    return priceBuilder.toString()
}

fun String.isValidYoutubeUrl() = Pattern.compile("^(http(s)?://)?((w){3}.)?youtu(be|.be)?(.com)?/.+").matcher(this).matches()

fun String.isValidUrl() = Patterns.WEB_URL.matcher(this).matches()

fun String.removeMarkdown(): String {
    try {
        val markdownRegex = Regex("""(\*{1,2}|_{1,2}|-|>|#\s*)""")
        return this.replace(markdownRegex, "").trim()
    } catch (_: Exception) {
        return this
    }
}


fun String.safeSubstring(startIndex: Int, endIndex: Int): String {
    return try {
        this.substring(startIndex, endIndex)
    } catch (_: IndexOutOfBoundsException) {
        ""
    }
}

fun String.safeSubstring(startIndex: Int): String {
    return try {
        this.substring(startIndex)
    } catch (_: IndexOutOfBoundsException) {
        ""
    }
}

fun String.createFile(): File? {
    val tempFile = File(this)
    return if (tempFile.exists()) tempFile else null
}