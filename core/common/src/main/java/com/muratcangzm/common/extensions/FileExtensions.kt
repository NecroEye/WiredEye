package com.muratcangzm.common.extensions

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import androidx.core.content.FileProvider
import androidx.core.graphics.createBitmap
import java.io.File


val File.size get() = if (!exists()) 0.0 else length().toDouble()
val File.sizeInKb get() = size / 1024
val File.sizeInMb get() = sizeInKb / 1024
val File.sizeInGb get() = sizeInMb / 1024
val File.sizeInTb get() = sizeInGb / 1024

fun File.sizeStr(): String = size.toString()
fun File.sizeStrInKb(decimals: Int = 0): String = "%.${decimals}f".format(sizeInKb)
fun File.sizeStrInMb(decimals: Int = 0): String = "%.${decimals}f".format(sizeInMb)
fun File.sizeStrInGb(decimals: Int = 0): String = "%.${decimals}f".format(sizeInGb)

fun File.sizeStrWithBytes(): String = sizeStr() + "b"
fun File.sizeStrWithKb(decimals: Int = 0): String = sizeStrInKb(decimals) + " Kb"
fun File.sizeStrWithMb(decimals: Int = 0): String = sizeStrInMb(decimals) + " Mb"
fun File.sizeStrWithGb(decimals: Int = 0): String = sizeStrInGb(decimals) + " Gb"

fun File.toShareUri(context: Context): Uri {
    return FileProvider.getUriForFile(context, "WiredEye.provider", this)
}

fun File.getPdfFirstPageBitmap(): Bitmap? {
    return try {
        val fileDescriptor = ParcelFileDescriptor.open(this, ParcelFileDescriptor.MODE_READ_ONLY)
        val pdfRenderer = PdfRenderer(fileDescriptor)
        val page = pdfRenderer.openPage(0)

        val bitmap = createBitmap(page.width, page.height)

        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
        page.close()
        pdfRenderer.close()
        fileDescriptor.close()

        bitmap
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun Long.kbToMb(): Long {
    return (this / 1024.0).toLong()
}

fun Long.toMB(): Double {
    return this / (1024.0 * 1024.0)
}

fun File.getAspectRatio(): String? {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    BitmapFactory.decodeFile(this.absolutePath, options)

    val width = options.outWidth
    val height = options.outHeight

    if (width <= 0 || height <= 0) return null

    fun gcd(a: Int, b: Int): Int {
        return if (b == 0) a else gcd(b, a % b)
    }

    val divisor = gcd(width, height)
    val ratioWidth = width / divisor
    val ratioHeight = height / divisor

    return "$ratioWidth:$ratioHeight"
}

fun File.getAspectRatioFloat(): Float {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }

    BitmapFactory.decodeFile(this.absolutePath, options)

    val width = options.outWidth
    val height = options.outHeight

    if (width <= 0 || height <= 0) return 1f

    return width.toFloat() / height.toFloat()
}

