package com.example.nextalk.util

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Gestionnaire de cache pour optimiser le stockage local
 */
class CacheManager(private val context: Context) {

    companion object {
        private const val TAG = "CacheManager"
        private const val MAX_CACHE_SIZE_MB = 100L
        private const val MAX_CACHE_AGE_DAYS = 7L
        private const val IMAGE_CACHE_DIR = "image_cache"
        private const val VIDEO_CACHE_DIR = "video_cache"
        private const val AUDIO_CACHE_DIR = "audio_cache"
    }

    /**
     * Obtenir la taille totale du cache en MB
     */
    suspend fun getCacheSize(): Long = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            calculateDirectorySize(cacheDir) / (1024 * 1024) // Convertir en MB
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating cache size", e)
            0L
        }
    }

    /**
     * Nettoyer le cache si nécessaire
     */
    suspend fun cleanCacheIfNeeded(): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val cacheSize = getCacheSize()
            
            if (cacheSize > MAX_CACHE_SIZE_MB) {
                Log.d(TAG, "Cache size ($cacheSize MB) exceeds limit, cleaning...")
                val freedSpace = cleanOldFiles()
                Result.success(freedSpace)
            } else {
                Log.d(TAG, "Cache size ($cacheSize MB) within limit")
                Result.success(0L)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning cache", e)
            Result.failure(e)
        }
    }

    /**
     * Supprimer les fichiers anciens du cache
     */
    suspend fun cleanOldFiles(): Long = withContext(Dispatchers.IO) {
        var freedSpace = 0L
        val maxAge = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(MAX_CACHE_AGE_DAYS)
        
        try {
            val cacheDir = context.cacheDir
            cacheDir.listFiles()?.forEach { file ->
                if (file.lastModified() < maxAge) {
                    val size = file.length()
                    if (file.delete()) {
                        freedSpace += size
                        Log.d(TAG, "Deleted old file: ${file.name}")
                    }
                }
            }
            
            Log.d(TAG, "Freed ${freedSpace / (1024 * 1024)} MB")
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning old files", e)
        }
        
        freedSpace / (1024 * 1024) // Retourner en MB
    }

    /**
     * Nettoyer tout le cache
     */
    suspend fun clearAllCache(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            deleteDirectory(cacheDir)
            Log.d(TAG, "All cache cleared")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all cache", e)
            Result.failure(e)
        }
    }

    /**
     * Nettoyer le cache des images
     */
    suspend fun clearImageCache(): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val imageDir = File(context.cacheDir, IMAGE_CACHE_DIR)
            if (imageDir.exists()) {
                deleteDirectory(imageDir)
                Log.d(TAG, "Image cache cleared")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing image cache", e)
            Result.failure(e)
        }
    }

    /**
     * Obtenir le répertoire de cache pour les images
     */
    fun getImageCacheDir(): File {
        val dir = File(context.cacheDir, IMAGE_CACHE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Obtenir le répertoire de cache pour les vidéos
     */
    fun getVideoCacheDir(): File {
        val dir = File(context.cacheDir, VIDEO_CACHE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Obtenir le répertoire de cache pour les audios
     */
    fun getAudioCacheDir(): File {
        val dir = File(context.cacheDir, AUDIO_CACHE_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Calculer la taille d'un répertoire récursivement
     */
    private fun calculateDirectorySize(directory: File): Long {
        var size = 0L
        
        if (directory.exists()) {
            directory.listFiles()?.forEach { file ->
                size += if (file.isDirectory) {
                    calculateDirectorySize(file)
                } else {
                    file.length()
                }
            }
        }
        
        return size
    }

    /**
     * Supprimer un répertoire récursivement
     */
    private fun deleteDirectory(directory: File): Boolean {
        if (directory.exists()) {
            directory.listFiles()?.forEach { file ->
                if (file.isDirectory) {
                    deleteDirectory(file)
                } else {
                    file.delete()
                }
            }
        }
        return directory.delete()
    }

    /**
     * Obtenir les statistiques du cache
     */
    suspend fun getCacheStats(): CacheStats = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            val totalSize = calculateDirectorySize(cacheDir)
            val fileCount = countFiles(cacheDir)
            
            val imageSize = File(cacheDir, IMAGE_CACHE_DIR).let {
                if (it.exists()) calculateDirectorySize(it) else 0L
            }
            
            val videoSize = File(cacheDir, VIDEO_CACHE_DIR).let {
                if (it.exists()) calculateDirectorySize(it) else 0L
            }
            
            val audioSize = File(cacheDir, AUDIO_CACHE_DIR).let {
                if (it.exists()) calculateDirectorySize(it) else 0L
            }
            
            CacheStats(
                totalSizeMB = totalSize / (1024 * 1024),
                fileCount = fileCount,
                imageSizeMB = imageSize / (1024 * 1024),
                videoSizeMB = videoSize / (1024 * 1024),
                audioSizeMB = audioSize / (1024 * 1024)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cache stats", e)
            CacheStats()
        }
    }

    /**
     * Compter les fichiers dans un répertoire récursivement
     */
    private fun countFiles(directory: File): Int {
        var count = 0
        
        if (directory.exists()) {
            directory.listFiles()?.forEach { file ->
                count += if (file.isDirectory) {
                    countFiles(file)
                } else {
                    1
                }
            }
        }
        
        return count
    }

    /**
     * Vérifier si le cache a besoin d'être nettoyé
     */
    suspend fun needsCleaning(): Boolean {
        val size = getCacheSize()
        return size > MAX_CACHE_SIZE_MB
    }

    /**
     * Obtenir l'âge du fichier le plus ancien en jours
     */
    suspend fun getOldestFileAge(): Long = withContext(Dispatchers.IO) {
        try {
            val cacheDir = context.cacheDir
            var oldestTime = System.currentTimeMillis()
            
            cacheDir.listFiles()?.forEach { file ->
                if (file.lastModified() < oldestTime) {
                    oldestTime = file.lastModified()
                }
            }
            
            TimeUnit.MILLISECONDS.toDays(System.currentTimeMillis() - oldestTime)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting oldest file age", e)
            0L
        }
    }
}

/**
 * Statistiques du cache
 */
data class CacheStats(
    val totalSizeMB: Long = 0,
    val fileCount: Int = 0,
    val imageSizeMB: Long = 0,
    val videoSizeMB: Long = 0,
    val audioSizeMB: Long = 0
) {
    fun toReadableString(): String {
        return """
            Taille totale: ${totalSizeMB}MB
            Nombre de fichiers: $fileCount
            Images: ${imageSizeMB}MB
            Vidéos: ${videoSizeMB}MB
            Audios: ${audioSizeMB}MB
        """.trimIndent()
    }
}
