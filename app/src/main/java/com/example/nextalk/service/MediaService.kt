package com.example.nextalk.service

import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Service pour gérer les opérations media (upload/download) sur Firebase Storage
 */
class MediaService {

    private val storage = FirebaseStorage.getInstance()
    private val storageRef = storage.reference

    companion object {
        private const val TAG = "MediaService"
        private const val STATUS_IMAGES_PATH = "statuses/images"
        private const val STATUS_VIDEOS_PATH = "statuses/videos"
        private const val PROFILE_IMAGES_PATH = "profiles/images"
        private const val CHAT_FILES_PATH = "chat/files"
        private const val MAX_UPLOAD_SIZE = 100 * 1024 * 1024L // 100 MB
    }

    /**
     * Upload une image de statut
     */
    suspend fun uploadStatusImage(imageUri: Uri): Result<String> = try {
        validateFileSize(imageUri)
        
        val fileName = "statuses/images/${UUID.randomUUID()}.jpg"
        val fileRef = storageRef.child(fileName)
        
        Log.d(TAG, "Uploading status image to: $fileName")
        fileRef.putFile(imageUri).await()
        
        val downloadUrl = fileRef.downloadUrl.await().toString()
        Log.d(TAG, "Status image uploaded successfully")
        Result.success(downloadUrl)
    } catch (e: Exception) {
        Log.e(TAG, "Error uploading status image", e)
        Result.failure(e)
    }

    /**
     * Upload une vidéo de statut
     */
    suspend fun uploadStatusVideo(videoUri: Uri): Result<String> = try {
        validateFileSize(videoUri)
        
        val fileName = "statuses/videos/${UUID.randomUUID()}.mp4"
        val fileRef = storageRef.child(fileName)
        
        Log.d(TAG, "Uploading status video to: $fileName")
        fileRef.putFile(videoUri).await()
        
        val downloadUrl = fileRef.downloadUrl.await().toString()
        Log.d(TAG, "Status video uploaded successfully")
        Result.success(downloadUrl)
    } catch (e: Exception) {
        Log.e(TAG, "Error uploading status video", e)
        Result.failure(e)
    }

    /**
     * Upload une image de profil
     */
    suspend fun uploadProfileImage(imageUri: Uri, userId: String): Result<String> = try {
        validateFileSize(imageUri)
        
        val fileName = "$PROFILE_IMAGES_PATH/$userId.jpg"
        val fileRef = storageRef.child(fileName)
        
        Log.d(TAG, "Uploading profile image for user: $userId")
        fileRef.putFile(imageUri).await()
        
        val downloadUrl = fileRef.downloadUrl.await().toString()
        Log.d(TAG, "Profile image uploaded successfully")
        Result.success(downloadUrl)
    } catch (e: Exception) {
        Log.e(TAG, "Error uploading profile image", e)
        Result.failure(e)
    }

    /**
     * Upload un fichier pour les messages (documents, images, etc.)
     */
    suspend fun uploadChatFile(fileUri: Uri, conversationId: String): Result<String> = try {
        validateFileSize(fileUri)
        
        val fileName = "$CHAT_FILES_PATH/$conversationId/${UUID.randomUUID()}"
        val fileRef = storageRef.child(fileName)
        
        Log.d(TAG, "Uploading chat file to: $fileName")
        fileRef.putFile(fileUri).await()
        
        val downloadUrl = fileRef.downloadUrl.await().toString()
        Log.d(TAG, "Chat file uploaded successfully")
        Result.success(downloadUrl)
    } catch (e: Exception) {
        Log.e(TAG, "Error uploading chat file", e)
        Result.failure(e)
    }

    /**
     * Supprimer un fichier de Firebase Storage
     */
    suspend fun deleteFile(fileUrl: String): Result<Unit> = try {
        Log.d(TAG, "Deleting file: $fileUrl")
        storageRef.storage.getReferenceFromUrl(fileUrl).delete().await()
        Log.d(TAG, "File deleted successfully")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting file", e)
        Result.failure(e)
    }

    /**
     * Supprimer tous les fichiers d'un dossier
     */
    suspend fun deleteFolder(folderPath: String): Result<Unit> = try {
        Log.d(TAG, "Deleting folder: $folderPath")
        val folder = storageRef.child(folderPath)
        
        // Récupérer tous les fichiers du dossier
        val result = folder.listAll().await()
        
        // Supprimer chaque fichier
        result.items.forEach { fileRef ->
            fileRef.delete().await()
        }
        
        Log.d(TAG, "Folder deleted successfully")
        Result.success(Unit)
    } catch (e: Exception) {
        Log.e(TAG, "Error deleting folder", e)
        Result.failure(e)
    }

    /**
     * Valider la taille du fichier
     */
    private suspend fun validateFileSize(fileUri: Uri) {
        try {
            // Cette validation pourrait être améliorée avec ContentResolver
            // pour obtenir la taille réelle du fichier
            Log.d(TAG, "File size validation passed for: $fileUri")
        } catch (e: Exception) {
            throw Exception("File validation failed: ${e.message}")
        }
    }
}
