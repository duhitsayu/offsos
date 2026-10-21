package com.example.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream

object TacticalMediaManager {

    fun processAndSaveImage(context: Context, imageUri: Uri): String? {
        return try {
            val imagesDir = File(context.filesDir, "field_photos").apply { mkdirs() }
            val outputFile = File(imagesDir, "photo_${System.currentTimeMillis()}.jpg")

            // Decode dimensions first
            var inputStream: InputStream? = context.contentResolver.openInputStream(imageUri)
            val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            BitmapFactory.decodeStream(inputStream, null, options)
            inputStream?.close()

            // Calculate sample size for max 1280x1280 tactical resolution
            val maxDimension = 1280
            var sampleSize = 1
            var w = options.outWidth
            var h = options.outHeight

            while (w / 2 >= maxDimension || h / 2 >= maxDimension) {
                w /= 2
                h /= 2
                sampleSize *= 2
            }

            // Decode actual bitmap
            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565
            }
            inputStream = context.contentResolver.openInputStream(imageUri)
            val originalBitmap = BitmapFactory.decodeStream(inputStream, null, decodeOptions)
            inputStream?.close()

            if (originalBitmap == null) return null

            // Handle orientation if needed
            val rotatedBitmap = try {
                context.contentResolver.openInputStream(imageUri)?.use { stream ->
                    val exif = ExifInterface(stream)
                    val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
                    val matrix = Matrix()
                    val needsRotation = when (orientation) {
                        ExifInterface.ORIENTATION_ROTATE_90 -> { matrix.postRotate(90f); true }
                        ExifInterface.ORIENTATION_ROTATE_180 -> { matrix.postRotate(180f); true }
                        ExifInterface.ORIENTATION_ROTATE_270 -> { matrix.postRotate(270f); true }
                        else -> false
                    }
                    if (needsRotation) {
                        Bitmap.createBitmap(originalBitmap, 0, 0, originalBitmap.width, originalBitmap.height, matrix, true)
                    } else {
                        originalBitmap
                    }
                } ?: originalBitmap
            } catch (_: Exception) {
                originalBitmap
            }

            // Save compressed JPEG (80% quality, compact for mesh)
            FileOutputStream(outputFile).use { fos ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 80, fos)
                fos.flush()
            }

            outputFile.absolutePath
        } catch (e: Exception) {
            Log.e("TacticalMediaManager", "Error saving picked image", e)
            null
        }
    }
}
