package com.example.nextalk.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Utilitaire pour compresser et optimiser les images
 */
class ImageCompressor(private val context: Context) {

    companion object {
        private const val TAG = "ImageCompressor"
        private const val DEFAULT_MAX_WIDTH = 1920
        private const val DEFAULT_MAX_HEIGHT = 1080
        private const val DEFAULT_QUALITY = 85
        private const val PROFILE_IMAGE_SIZE = 512
        private const val THUMBNAIL_SIZE = 200
    }

    /**
     * Compresser une image pour un message de chat
     */
    suspend fun compressForChat(
        imageUri: Uri,
        maxWidth: Int = DEFAULT_MAX_WIDTH,
        maxHeight: Int = DEFAULT_MAX_HEIGHT,
        quality: Int = DEFAULT_QUALITY
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeBitmapFromUri(imageUri) ?: return@withContext Result.failure(
                Exception("Failed to decode bitmap")
            )

            val rotatedBitmap = rotateBitmapIfNeeded(bitmap, imageUri)
            val resizedBitmap = resizeBitmap(rotatedBitmap, maxWidth, maxHeight)
            val compressedFile = saveBitmapToFile(resizedBitmap, quality, "chat_")

            // Libérer la mémoire
            if (bitmap != rotatedBitmap) bitmap.recycle()
            if (rotatedBitmap != resizedBitmap) rotatedBitmap.recycle()
            resizedBitmap.recycle()

            Result.success(compressedFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing image for chat", e)
            Result.failure(e)
        }
    }

    /**
     * Compresser une image de profil (format carré)
     */
    suspend fun compressForProfile(imageUri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeBitmapFromUri(imageUri) ?: return@withContext Result.failure(
                Exception("Failed to decode bitmap")
            )

            val rotatedBitmap = rotateBitmapIfNeeded(bitmap, imageUri)
            val squareBitmap = cropToSquare(rotatedBitmap)
            val resizedBitmap = resizeBitmap(squareBitmap, PROFILE_IMAGE_SIZE, PROFILE_IMAGE_SIZE)
            val compressedFile = saveBitmapToFile(resizedBitmap, 90, "profile_")

            // Libérer la mémoire
            if (bitmap != rotatedBitmap) bitmap.recycle()
            if (rotatedBitmap != squareBitmap) rotatedBitmap.recycle()
            if (squareBitmap != resizedBitmap) squareBitmap.recycle()
            resizedBitmap.recycle()

            Result.success(compressedFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error compressing profile image", e)
            Result.failure(e)
        }
    }

    /**
     * Créer une miniature d'image
     */
    suspend fun createThumbnail(imageUri: Uri): Result<File> = withContext(Dispatchers.IO) {
        try {
            val bitmap = decodeBitmapFromUri(imageUri) ?: return@withContext Result.failure(
                Exception("Failed to decode bitmap")
            )

            val resizedBitmap = resizeBitmap(bitmap, THUMBNAIL_SIZE, THUMBNAIL_SIZE)
            val thumbnailFile = saveBitmapToFile(resizedBitmap, 80, "thumb_")

            bitmap.recycle()
            resizedBitmap.recycle()

            Result.success(thumbnailFile)
        } catch (e: Exception) {
            Log.e(TAG, "Error creating thumbnail", e)
            Result.failure(e)
        }
    }

    /**
     * Décoder un bitmap depuis une URI avec gestion de la mémoire
     */
    private fun decodeBitmapFromUri(uri: Uri): Bitmap? {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }

            // Calculer le facteur de sous-échantillonnage
            options.inSampleSize = calculateInSampleSize(options, DEFAULT_MAX_WIDTH, DEFAULT_MAX_HEIGHT)
            options.inJustDecodeBounds = false

            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                BitmapFactory.decodeStream(inputStream, null, options)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error decoding bitmap", e)
            null
        }
    }

    /**
     * Calculer le facteur de sous-échantillonnage pour économiser la mémoire
     */
    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight &&
                (halfWidth / inSampleSize) >= reqWidth
            ) {
                inSampleSize *= 2
            }
        }

        return inSampleSize
    }

    /**
     * Redimensionner un bitmap en gardant le ratio
     */
    private fun resizeBitmap(bitmap: Bitmap, maxWidth: Int, maxHeight: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height

        if (width <= maxWidth && height <= maxHeight) {
            return bitmap
        }

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int

        if (width > height) {
            newWidth = maxWidth
            newHeight = (maxWidth / ratio).toInt()
        } else {
            newHeight = maxHeight
            newWidth = (maxHeight * ratio).toInt()
        }

        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Recadrer l'image en carré
     */
    private fun cropToSquare(bitmap: Bitmap): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        val size = minOf(width, height)

        val x = (width - size) / 2
        val y = (height - size) / 2

        return Bitmap.createBitmap(bitmap, x, y, size, size)
    }

    /**
     * Corriger l'orientation de l'image selon les données EXIF
     */
    private fun rotateBitmapIfNeeded(bitmap: Bitmap, uri: Uri): Bitmap {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri)
            val exif = inputStream?.use { ExifInterface(it) }
            val orientation = exif?.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            ) ?: ExifInterface.ORIENTATION_NORMAL

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.postScale(-1f, 1f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> matrix.postScale(1f, -1f)
                else -> return bitmap
            }

            val rotatedBitmap = Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true
            )

            if (rotatedBitmap != bitmap) {
                bitmap.recycle()
            }

            rotatedBitmap
        } catch (e: IOException) {
            Log.e(TAG, "Error rotating bitmap", e)
            bitmap
        }
    }

    /**
     * Sauvegarder un bitmap dans un fichier
     */
    private fun saveBitmapToFile(bitmap: Bitmap, quality: Int, prefix: String): File {
        val cacheDir = context.cacheDir
        val fileName = "$prefix${System.currentTimeMillis()}.jpg"
        val file = File(cacheDir, fileName)

        FileOutputStream(file).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
        }

        Log.d(TAG, "Saved compressed image: ${file.absolutePath} (${file.length() / 1024}KB)")
        return file
    }

    /**
     * Obtenir la taille d'un bitmap en bytes
     */
    fun getBitmapSize(bitmap: Bitmap): Int {
        val stream = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream)
        return stream.toByteArray().size
    }

    /**
     * Calculer le ratio de compression
     */
    suspend fun getCompressionRatio(originalUri: Uri, compressedFile: File): Float {
        return withContext(Dispatchers.IO) {
            try {
                val originalSize = context.contentResolver.openInputStream(originalUri)?.use {
                    it.available()
                } ?: 0

                val compressedSize = compressedFile.length()
                
                if (originalSize > 0) {
                    (1 - compressedSize.toFloat() / originalSize) * 100
                } else {
                    0f
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error calculating compression ratio", e)
                0f
            }
        }
    }
}
