package com.muratcangzm.common.extensions


import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.OpenableColumns
import com.muratcangzm.tryOrNull
import java.io.File
import java.io.InputStream

private const val URI_CONTENT_SCHEMA = "content"

fun Uri.toBitmap(context: Context): Bitmap? {
    val sizeStream = tryOrNull { context.contentResolver.openInputStream(this) } ?: return null
    val (width, height) = calculateBitmapSizeBeforeDecode(sizeStream)
    val (requestedWidth, requestedHeight) = calculateRequestedWidthHeight(width, height)
    sizeStream.close()

    val bitmapStream = context.contentResolver.openInputStream(this)
    val sampleSize = calculateInSampleSize(width = width, height = height, reqWidth = requestedWidth, reqHeight = requestedHeight)
    val bitmap = BitmapFactory.decodeStream(bitmapStream, null, BitmapFactory.Options().apply { inSampleSize = sampleSize })
    bitmapStream?.close()
    return bitmap
}

private fun calculateRequestedWidthHeight(width: Int, height: Int): Pair<Int, Int> {
    return when {
        width == height -> Pair(1024, 1024)
        width > height -> {
            val scaleRate = width.toFloat() / 1024f
            val newHeight = (height / scaleRate).toInt()
            Pair(1024, newHeight)
        }

        else -> {
            val scaleRate = height.toFloat() / 1024f
            val newWidth = (width / scaleRate).toInt()
            Pair(newWidth, 1024)
        }
    }
}

private fun calculateBitmapSizeBeforeDecode(stream: InputStream?): Pair<Int, Int> {
    val options = BitmapFactory.Options().apply {
        inJustDecodeBounds = true
    }
    BitmapFactory.decodeStream(stream, null, options)
    val imageHeight: Int = options.outHeight
    val imageWidth: Int = options.outWidth
    return Pair(imageWidth, imageHeight)
}

private fun calculateInSampleSize(width: Int, height: Int, reqWidth: Int, reqHeight: Int): Int {
    var inSampleSize = 1
    if (height > reqHeight || width > reqWidth) {

        val halfHeight: Int = height / 2
        val halfWidth: Int = width / 2

        while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
            inSampleSize *= 2
        }
    }
    return inSampleSize
}

fun Uri.toFile(context: Context, fileName: String): File? {
    return try {
        val inputStream = context.contentResolver.openInputStream(this) ?: return null
        val safeFileName = fileName.replace(Regex("[^a-zA-Z0-9_.-]"), "_")
        val file = File(context.cacheDir, safeFileName)

        inputStream.use { input ->
            file.outputStream().use { output ->
                input.copyTo(output)
            }
        }

        if (file.exists() && file.length() > 0) file else null
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }
}

fun Uri.getFileSize(context: Context): Long {
    return if (scheme == "content") {
        context.contentResolver.query(this, null, null, null, null)?.use { cursor ->
            val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
            if (sizeIndex != -1 && cursor.moveToFirst()) {
                cursor.getLong(sizeIndex)
            } else {
                0L
            }
        } ?: 0L
    } else {
        val file = File(path ?: return 0L)
        file.length()
    }
}

fun Uri.isContentSchema() = this.scheme == URI_CONTENT_SCHEMA