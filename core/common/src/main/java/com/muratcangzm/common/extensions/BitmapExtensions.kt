package com.muratcangzm.common.extensions

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.core.graphics.createBitmap
import androidx.core.graphics.scale
import com.muratcangzm.tryOrLog
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.util.UUID

fun Bitmap.resize(newWidth: Int, newHeight: Int): Bitmap = this.scale(newWidth, newHeight)

fun Bitmap.saveCacheDirectory(
    context: Context,
    bitmapFormat: BitmapSaveFormat,
    isUniqueFileName: Boolean = false,
): File {
    val fileName = if (isUniqueFileName) {
        val unique = UUID.randomUUID().toString()
        "${unique}_${System.currentTimeMillis()}.${bitmapFormat.suffix}"
    } else {
        "${"WiredEye"}.${bitmapFormat.suffix}"
    }

    val bitmapFile = File(context.applicationContext.cacheDir, fileName)

    tryOrLog {
        FileOutputStream(bitmapFile).use { imageFos ->
            this.compress(Bitmap.CompressFormat.valueOf(bitmapFormat.name), 100, imageFos)
        }
    }

    return bitmapFile
}

fun Bitmap.saveCacheDirectoryOverride(context: Context, bitmapFormat: BitmapSaveFormat): File {
    val fileName = "${"WiredEye"}.${bitmapFormat.suffix}"
    val bitmapFile = File(context.applicationContext.cacheDir, fileName)
    tryOrLog {
        val imageFos = FileOutputStream(bitmapFile)
        this.compress(Bitmap.CompressFormat.valueOf(bitmapFormat.name), 100, imageFos)
        imageFos.close()
    }
    return bitmapFile
}

fun Bitmap.saveImageToGallery(context: Context): Boolean {
    try {
        val unique = UUID.randomUUID().toString()
        val fileName = "${unique}_${System.currentTimeMillis()}.jpeg"
        var fos: OutputStream? = null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver?.also { resolver ->
                val contentValues = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
                    put(
                        MediaStore.MediaColumns.RELATIVE_PATH,
                        Environment.DIRECTORY_PICTURES + File.separator + "WiredEye"
                    )
                }
                val imageUri: Uri? = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
                fos = imageUri?.let { uri -> resolver.openOutputStream(uri) }
            }
        } else {
            val imagesDir = File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES).toString() +
                        File.separator + "WiredEye"
            )
            if (imagesDir.exists().not()) {
                imagesDir.mkdir()
            }
            val image = File(imagesDir, fileName)
            fos = FileOutputStream(image)
        }
        fos?.use { os ->
            this.compress(Bitmap.CompressFormat.JPEG, 100, os)
            return true
        }
    } catch (exception: Exception) {
        return false
    }
    return true
}

fun Bitmap.isSquare() = this.height == this.width
fun Bitmap.isPortrait() = this.height > this.width

fun Bitmap.mergeWithTattooFitted(
    tattoo: Bitmap,
    offsetXPx: Float,
    offsetYPx: Float,
    scale: Float,
    rotationDeg: Float,
    containerWidthPx: Float,
    containerHeightPx: Float,
    displayDensity: Float,
): Bitmap {
    val baseW = width.toFloat()
    val baseH = height.toFloat()

    val fitScale = kotlin.math.min(containerWidthPx / baseW, containerHeightPx / baseH)
    val drawnW = baseW * fitScale
    val drawnH = baseH * fitScale
    val left = (containerWidthPx - drawnW) / 2f
    val top = (containerHeightPx - drawnH) / 2f

    val cxScreen = containerWidthPx / 2f + offsetXPx
    val cyScreen = containerHeightPx / 2f + offsetYPx

    val cxBase = (cxScreen - left) / fitScale
    val cyBase = (cyScreen - top) / fitScale

    val out = createBitmap(width, height, config ?: Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    canvas.drawBitmap(this, 0f, 0f, null)

    val userScale = scale.coerceAtLeast(0.01f)
    val scaleOnBase = ((userScale / fitScale) / displayDensity) * 1.34f

    val matrix = Matrix().apply {
        postTranslate(cxBase - tattoo.width / 2f, cyBase - tattoo.height / 2f)
        postScale(scaleOnBase, scaleOnBase, cxBase, cyBase)
        postRotate(rotationDeg, cxBase, cyBase)
    }
    canvas.drawBitmap(tattoo, matrix, null)
    return out
}

fun Bitmap.downscaleIfTooLarge(maxSide: Int = 2048): Bitmap {
    val largest = maxOf(width, height)
    if (largest <= maxSide) return this
    val ratio = maxSide.toFloat() / largest.toFloat()
    val w = (width * ratio).toInt().coerceAtLeast(1)
    val h = (height * ratio).toInt().coerceAtLeast(1)
    return this.scale(w, h)
}

enum class BitmapSaveFormat(val suffix: String) {
    PNG(suffix = "png"),
    JPEG(suffix = "jpeg")
}