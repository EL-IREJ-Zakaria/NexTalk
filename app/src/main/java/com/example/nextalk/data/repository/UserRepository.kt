package com.example.nextalk.data.repository

import android.net.Uri
import android.util.Log
import com.example.nextalk.data.local.dao.UserDao
import com.example.nextalk.data.model.User
import com.example.nextalk.service.MediaService
import com.example.nextalk.util.withRetry
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

class UserRepository(
    private val userDao: UserDao,
    private val mediaService: MediaService = MediaService()
) {

    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    companion object {
        private const val TAG = "UserRepository"
    }

    // Obtenir tous les utilisateurs en temps réel
    fun getAllUsersFlow(): Flow<List<User>> = callbackFlow {
        val listener = usersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error getting users", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            val users = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(User::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing user", e)
                    null
                }
            } ?: emptyList()

            trySend(users)
        }

        awaitClose { listener.remove() }
    }.catch { e ->
        Log.e(TAG, "Flow error", e)
        emit(emptyList())
    }

    // Obtenir tous les utilisateurs (sauf l'utilisateur courant)
    fun getAllUsersExcept(currentUserId: String): Flow<List<User>> = callbackFlow {
        val listener = usersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error getting users except", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            val users = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(User::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing user", e)
                    null
                }
            }?.filter { it.uid != currentUserId } ?: emptyList()

            trySend(users)
        }

        awaitClose { listener.remove() }
    }.catch { e ->
        Log.e(TAG, "Flow error", e)
        emit(emptyList())
    }

    // Obtenir un utilisateur par ID
    suspend fun getUserById(userId: String): User? {
        return try {
            val doc = usersCollection.document(userId).get().await()
            doc.toObject(User::class.java)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user by id from Firestore", e)
            try {
                userDao.getUserById(userId)
            } catch (e2: Exception) {
                Log.e(TAG, "Error getting user by id from local", e2)
                null
            }
        }
    }

    // Observer un utilisateur en temps réel
    fun observeUser(userId: String): Flow<User?> = callbackFlow {
        val listener = usersCollection.document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing user", error)
                trySend(null)
                return@addSnapshotListener
            }

            val user = try {
                snapshot?.toObject(User::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing user", e)
                null
            }
            trySend(user)
        }

        awaitClose { listener.remove() }
    }.catch { e ->
        Log.e(TAG, "Flow error", e)
        emit(null)
    }

    // Rechercher des utilisateurs
    fun searchUsers(query: String, currentUserId: String): Flow<List<User>> = callbackFlow {
        val listener = usersCollection.addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error searching users", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            val users = snapshot?.documents?.mapNotNull { doc ->
                try {
                    doc.toObject(User::class.java)
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing user", e)
                    null
                }
            }?.filter {
                it.uid != currentUserId &&
                        (it.name.contains(query, ignoreCase = true) ||
                                it.email.contains(query, ignoreCase = true))
            } ?: emptyList()

            trySend(users)
        }

        awaitClose { listener.remove() }
    }.catch { e ->
        Log.e(TAG, "Flow error", e)
        emit(emptyList())
    }

    // Mettre à jour le statut en ligne
    suspend fun updateOnlineStatus(userId: String, isOnline: Boolean) {
        try {
            usersCollection.document(userId).update(
                mapOf(
                    "isOnline" to isOnline,
                    "lastSeen" to System.currentTimeMillis()
                )
            ).await()
        } catch (e: Exception) {
            Log.e(TAG, "Error updating online status", e)
        }
    }

    // Sauvegarder dans la base locale
    suspend fun saveUserLocally(user: User) {
        try {
            userDao.insertUser(user)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving user locally", e)
        }
    }

    suspend fun saveUsersLocally(users: List<User>) {
        try {
            userDao.insertUsers(users)
        } catch (e: Exception) {
            Log.e(TAG, "Error saving users locally", e)
        }
    }

    // Obtenir depuis la base locale
    fun getLocalUsers(): Flow<List<User>> = userDao.getAllUsers()

    suspend fun getLocalUserById(userId: String): User? {
        return try {
            userDao.getUserById(userId)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting local user", e)
            null
        }
    }

    /**
     * Mettre à jour la photo de profil avec retry
     */
    suspend fun updateProfilePhoto(userId: String, photoUri: Uri): Result<String> = withRetry(maxRetries = 2) {
        // Upload la photo
        val uploadResult = mediaService.uploadProfileImage(photoUri, userId)
        
        if (uploadResult.isFailure) {
            throw uploadResult.exceptionOrNull() ?: Exception("Failed to upload photo")
        }

        val photoUrl = uploadResult.getOrNull() ?: throw Exception("No photo URL")

        // Mettre à jour dans Firestore
        usersCollection.document(userId).update("photoUrl", photoUrl).await()

        Log.d(TAG, "Profile photo updated successfully")
        photoUrl
    }

    /**
     * Mettre à jour le profil utilisateur
     */
    suspend fun updateUserProfile(
        userId: String,
        updates: Map<String, Any>
    ): Result<Unit> = withRetry(maxRetries = 2) {
        usersCollection.document(userId).update(updates).await()
        Log.d(TAG, "User profile updated: $updates")
    }

    /**
     * Mettre à jour le statut bio
     */
    suspend fun updateBio(userId: String, bio: String): Result<Unit> {
        return try {
            usersCollection.document(userId).update("bio", bio).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating bio", e)
            Result.failure(e)
        }
    }

    /**
     * Bloquer un utilisateur
     */
    suspend fun blockUser(currentUserId: String, blockedUserId: String): Result<Unit> {
        return try {
            val userDoc = usersCollection.document(currentUserId).get().await()
            val blockedUsers = (userDoc.get("blockedUsers") as? List<*>)
                ?.filterIsInstance<String>()
                ?.toMutableList() ?: mutableListOf()

            if (!blockedUsers.contains(blockedUserId)) {
                blockedUsers.add(blockedUserId)
                usersCollection.document(currentUserId)
                    .update("blockedUsers", blockedUsers)
                    .await()
            }

            Log.d(TAG, "User blocked: $blockedUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error blocking user", e)
            Result.failure(e)
        }
    }

    /**
     * Débloquer un utilisateur
     */
    suspend fun unblockUser(currentUserId: String, blockedUserId: String): Result<Unit> {
        return try {
            val userDoc = usersCollection.document(currentUserId).get().await()
            val blockedUsers = (userDoc.get("blockedUsers") as? List<*>)
                ?.filterIsInstance<String>()
                ?.toMutableList() ?: mutableListOf()

            blockedUsers.remove(blockedUserId)
            usersCollection.document(currentUserId)
                .update("blockedUsers", blockedUsers)
                .await()

            Log.d(TAG, "User unblocked: $blockedUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error unblocking user", e)
            Result.failure(e)
        }
    }

    /**
     * Obtenir la liste des utilisateurs bloqués
     */
    suspend fun getBlockedUsers(userId: String): List<String> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            (userDoc.get("blockedUsers") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Error getting blocked users", e)
            emptyList()
        }
    }

    /**
     * Vérifier si un utilisateur est bloqué
     */
    suspend fun isUserBlocked(currentUserId: String, otherUserId: String): Boolean {
        val blockedUsers = getBlockedUsers(currentUserId)
        return blockedUsers.contains(otherUserId)
    }

    /**
     * Synchroniser les utilisateurs depuis Firebase
     */
    suspend fun syncUsersFromFirebase(): Result<Unit> = withRetry(maxRetries = 2) {
        val snapshot = usersCollection.get().await()
        val users = snapshot.documents.mapNotNull { doc ->
            try {
                doc.toObject(User::class.java)
            } catch (e: Exception) {
                Log.e(TAG, "Error parsing user", e)
                null
            }
        }

        userDao.insertUsers(users)
        Log.d(TAG, "Synced ${users.size} users from Firebase")
    }

    /**
     * Mettre à jour le FCM token
     */
    suspend fun updateFcmToken(userId: String, token: String): Result<Unit> {
        return try {
            usersCollection.document(userId).update("fcmToken", token).await()
            Log.d(TAG, "FCM token updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating FCM token", e)
            Result.failure(e)
        }
    }

    /**
     * Obtenir les utilisateurs en ligne
     */
    fun getOnlineUsers(): Flow<List<User>> = callbackFlow {
        val listener = usersCollection
            .whereEqualTo("isOnline", true)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    Log.e(TAG, "Error getting online users", error)
                    trySend(emptyList())
                    return@addSnapshotListener
                }

                val users = snapshot?.documents?.mapNotNull { doc ->
                    try {
                        doc.toObject(User::class.java)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing user", e)
                        null
                    }
                } ?: emptyList()

                trySend(users)
            }

        awaitClose { listener.remove() }
    }.catch { e ->
        Log.e(TAG, "Flow error", e)
        emit(emptyList())
    }

    /**
     * Recherche avancée d'utilisateurs (nom, email, téléphone)
     */
    suspend fun searchUsersAdvanced(query: String, currentUserId: String): Result<List<User>> {
        return try {
            val snapshot = usersCollection.get().await()
            val users = snapshot.documents.mapNotNull { doc ->
                try {
                    doc.toObject(User::class.java)
                } catch (e: Exception) {
                    null
                }
            }.filter { user ->
                user.uid != currentUserId &&
                (user.name.contains(query, ignoreCase = true) ||
                 user.email.contains(query, ignoreCase = true))
            }

            Result.success(users)
        } catch (e: Exception) {
            Log.e(TAG, "Error searching users", e)
            Result.failure(e)
        }
    }

    /**
     * Obtenir les statistiques utilisateur
     */
    suspend fun getUserStats(userId: String): Map<String, Int> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            mapOf(
                "messagesSent" to (userDoc.getLong("messagesSent")?.toInt() ?: 0),
                "callsMade" to (userDoc.getLong("callsMade")?.toInt() ?: 0),
                "statusesPosted" to (userDoc.getLong("statusesPosted")?.toInt() ?: 0)
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting user stats", e)
            emptyMap()
        }
    }

    // ============= NOUVELLES FONCTIONNALITÉS AVANCÉES =============

    /**
     * ⭐ Ajouter un utilisateur aux favoris
     */
    suspend fun addToFavorites(currentUserId: String, favoriteUserId: String): Result<Unit> {
        return try {
            val userDoc = usersCollection.document(currentUserId).get().await()
            val favorites = (userDoc.get("favorites") as? List<*>)
                ?.filterIsInstance<String>()
                ?.toMutableList() ?: mutableListOf()

            if (!favorites.contains(favoriteUserId)) {
                favorites.add(favoriteUserId)
                usersCollection.document(currentUserId)
                    .update(mapOf(
                        "favorites" to favorites,
                        "favoritesUpdatedAt" to System.currentTimeMillis()
                    ))
                    .await()
                Log.d(TAG, "User added to favorites: $favoriteUserId")
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding to favorites", e)
            Result.failure(e)
        }
    }

    /**
     * ⭐ Retirer un utilisateur des favoris
     */
    suspend fun removeFromFavorites(currentUserId: String, favoriteUserId: String): Result<Unit> {
        return try {
            val userDoc = usersCollection.document(currentUserId).get().await()
            val favorites = (userDoc.get("favorites") as? List<*>)
                ?.filterIsInstance<String>()
                ?.toMutableList() ?: mutableListOf()

            favorites.remove(favoriteUserId)
            usersCollection.document(currentUserId)
                .update(mapOf(
                    "favorites" to favorites,
                    "favoritesUpdatedAt" to System.currentTimeMillis()
                ))
                .await()

            Log.d(TAG, "User removed from favorites: $favoriteUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error removing from favorites", e)
            Result.failure(e)
        }
    }

    /**
     * 📋 Obtenir la liste des favoris
     */
    suspend fun getFavorites(userId: String): Result<List<String>> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val favorites = (userDoc.get("favorites") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()

            Result.success(favorites)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting favorites", e)
            Result.failure(e)
        }
    }

    /**
     * 👥 Obtenir les utilisateurs favoris avec leurs infos
     */
    fun getFavoriteUsers(userId: String): Flow<List<User>> = callbackFlow {
        val listener = usersCollection.document(userId).addSnapshotListener { snapshot, error ->
            if (error != null) {
                Log.e(TAG, "Error observing favorites", error)
                trySend(emptyList())
                return@addSnapshotListener
            }

            val favoriteIds = (snapshot?.get("favorites") as? List<*>)
                ?.filterIsInstance<String>() ?: emptyList()

            if (favoriteIds.isEmpty()) {
                trySend(emptyList())
                return@addSnapshotListener
            }

            // Récupérer les infos des favoris (en batches de 10)
            val batches = favoriteIds.chunked(10)
            val favoriteUsers = mutableListOf<User>()

            for (batch in batches) {
                usersCollection.whereIn("uid", batch).get()
                    .addOnSuccessListener { querySnapshot ->
                        val users = querySnapshot.documents.mapNotNull { doc ->
                            try {
                                doc.toObject(User::class.java)
                            } catch (e: Exception) {
                                null
                            }
                        }
                        favoriteUsers.addAll(users)
                        trySend(favoriteUsers.toList())
                    }
            }
        }

        awaitClose { listener.remove() }
    }.catch { e ->
        Log.e(TAG, "Flow error", e)
        emit(emptyList())
    }

    /**
     * 🔔 Configurer les préférences de notification pour un utilisateur
     */
    suspend fun setNotificationPreferences(
        currentUserId: String,
        otherUserId: String,
        preferences: Map<String, Boolean>
    ): Result<Unit> {
        return try {
            val userDoc = usersCollection.document(currentUserId).get().await()
            val notifPrefs = (userDoc.get("notificationPreferences") as? Map<*, *>)
                ?.mapKeys { it.key.toString() }
                ?.mapValues { it.value as? Map<*, *> }
                ?.toMutableMap() ?: mutableMapOf()

            notifPrefs[otherUserId] = preferences

            usersCollection.document(currentUserId)
                .update("notificationPreferences", notifPrefs)
                .await()

            Log.d(TAG, "Notification preferences updated for $otherUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting notification preferences", e)
            Result.failure(e)
        }
    }

    /**
     * 🔕 Désactiver les notifications pour un utilisateur
     */
    suspend fun muteUser(currentUserId: String, mutedUserId: String, muteUntil: Long): Result<Unit> {
        return try {
            val userDoc = usersCollection.document(currentUserId).get().await()
            val mutedUsers = (userDoc.get("mutedUsers") as? Map<*, *>)
                ?.mapKeys { it.key.toString() }
                ?.mapValues { (it.value as? Number)?.toLong() ?: 0L }
                ?.toMutableMap() ?: mutableMapOf()

            mutedUsers[mutedUserId] = muteUntil

            usersCollection.document(currentUserId)
                .update("mutedUsers", mutedUsers)
                .await()

            Log.d(TAG, "User muted until $muteUntil: $mutedUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error muting user", e)
            Result.failure(e)
        }
    }

    /**
     * 🔔 Réactiver les notifications pour un utilisateur
     */
    suspend fun unmuteUser(currentUserId: String, mutedUserId: String): Result<Unit> {
        return try {
            val userDoc = usersCollection.document(currentUserId).get().await()
            val mutedUsers = (userDoc.get("mutedUsers") as? Map<*, *>)
                ?.mapKeys { it.key.toString() }
                ?.mapValues { (it.value as? Number)?.toLong() ?: 0L }
                ?.toMutableMap() ?: mutableMapOf()

            mutedUsers.remove(mutedUserId)

            usersCollection.document(currentUserId)
                .update("mutedUsers", mutedUsers)
                .await()

            Log.d(TAG, "User unmuted: $mutedUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error unmuting user", e)
            Result.failure(e)
        }
    }

    /**
     * 🔍 Vérifier si un utilisateur est muté
     */
    suspend fun isUserMuted(currentUserId: String, otherUserId: String): Boolean {
        return try {
            val userDoc = usersCollection.document(currentUserId).get().await()
            val mutedUsers = (userDoc.get("mutedUsers") as? Map<*, *>)
                ?.mapKeys { it.key.toString() }
                ?.mapValues { (it.value as? Number)?.toLong() ?: 0L } ?: emptyMap()

            val muteUntil = mutedUsers[otherUserId] ?: 0L
            muteUntil > System.currentTimeMillis()
        } catch (e: Exception) {
            Log.e(TAG, "Error checking if user is muted", e)
            false
        }
    }

    /**
     * 📊 Obtenir les statistiques détaillées d'un utilisateur
     */
    suspend fun getUserDetailedStats(userId: String): Map<String, Any> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            mapOf(
                "totalMessages" to (userDoc.getLong("messagesSent")?.toInt() ?: 0),
                "totalCalls" to (userDoc.getLong("callsMade")?.toInt() ?: 0),
                "totalStatuses" to (userDoc.getLong("statusesPosted")?.toInt() ?: 0),
                "accountCreated" to (userDoc.getLong("createdAt") ?: 0L),
                "lastSeen" to (userDoc.getLong("lastSeen") ?: 0L),
                "isOnline" to (userDoc.getBoolean("isOnline") ?: false),
                "favorites" to ((userDoc.get("favorites") as? List<*>)?.size ?: 0),
                "blockedUsers" to ((userDoc.get("blockedUsers") as? List<*>)?.size ?: 0),
                "bio" to (userDoc.getString("bio") ?: ""),
                "photoUrl" to (userDoc.getString("photoUrl") ?: "")
            )
        } catch (e: Exception) {
            Log.e(TAG, "Error getting detailed stats", e)
            emptyMap()
        }
    }

    /**
     * 🎯 Marquer l'activité récente d'un utilisateur
     */
    suspend fun trackUserActivity(userId: String, activityType: String, metadata: Map<String, Any> = emptyMap()): Result<Unit> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val recentActivity = (userDoc.get("recentActivity") as? List<*>)
                ?.mapNotNull { it as? Map<*, *> }
                ?.toMutableList() ?: mutableListOf()

            // Ajouter la nouvelle activité
            recentActivity.add(0, mapOf(
                "type" to activityType,
                "timestamp" to System.currentTimeMillis(),
                "metadata" to metadata
            ))

            // Garder seulement les 50 dernières activités
            val trimmedActivity = recentActivity.take(50)

            usersCollection.document(userId)
                .update("recentActivity", trimmedActivity)
                .await()

            Log.d(TAG, "User activity tracked: $activityType")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error tracking activity", e)
            Result.failure(e)
        }
    }

    /**
     * 📈 Obtenir l'activité récente d'un utilisateur
     */
    suspend fun getUserRecentActivity(userId: String, limit: Int = 20): Result<List<Map<String, Any>>> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val recentActivity = (userDoc.get("recentActivity") as? List<*>)
                ?.mapNotNull { it as? Map<*, *> }
                ?.take(limit)
                ?.map { it.mapKeys { entry -> entry.key.toString() }.mapValues { entry -> entry.value ?: "" } }
                ?: emptyList()

            Result.success(recentActivity)
        } catch (e: Exception) {
            Log.e(TAG, "Error getting recent activity", e)
            Result.failure(e)
        }
    }

    /**
     * 🏷️ Ajouter un tag/label personnalisé à un utilisateur
     */
    suspend fun addUserTag(currentUserId: String, otherUserId: String, tag: String): Result<Unit> {
        return try {
            val userDoc = usersCollection.document(currentUserId).get().await()
            val userTags = (userDoc.get("userTags") as? Map<*, *>)
                ?.mapKeys { it.key.toString() }
                ?.mapValues { (it.value as? List<*>)?.filterIsInstance<String>() ?: emptyList() }
                ?.toMutableMap() ?: mutableMapOf()

            val tags = userTags[otherUserId]?.toMutableList() ?: mutableListOf()
            if (!tags.contains(tag)) {
                tags.add(tag)
                userTags[otherUserId] = tags
            }

            usersCollection.document(currentUserId)
                .update("userTags", userTags)
                .await()

            Log.d(TAG, "Tag added to user $otherUserId: $tag")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error adding user tag", e)
            Result.failure(e)
        }
    }

    /**
     * 🌍 Mettre à jour la localisation de l'utilisateur
     */
    suspend fun updateUserLocation(
        userId: String,
        latitude: Double,
        longitude: Double,
        city: String = "",
        country: String = ""
    ): Result<Unit> {
        return try {
            val locationData = mapOf(
                "latitude" to latitude,
                "longitude" to longitude,
                "city" to city,
                "country" to country,
                "updatedAt" to System.currentTimeMillis()
            )

            usersCollection.document(userId)
                .update("location", locationData)
                .await()

            Log.d(TAG, "User location updated")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error updating location", e)
            Result.failure(e)
        }
    }

    /**
     * 🎨 Personnaliser le thème de chat pour un utilisateur
     */
    suspend fun setChatTheme(
        currentUserId: String,
        otherUserId: String,
        backgroundColor: String,
        bubbleColor: String,
        textColor: String
    ): Result<Unit> {
        return try {
            val userDoc = usersCollection.document(currentUserId).get().await()
            val chatThemes = (userDoc.get("chatThemes") as? Map<*, *>)
                ?.mapKeys { it.key.toString() }
                ?.toMutableMap() ?: mutableMapOf()

            chatThemes[otherUserId] = mapOf(
                "backgroundColor" to backgroundColor,
                "bubbleColor" to bubbleColor,
                "textColor" to textColor,
                "updatedAt" to System.currentTimeMillis()
            )

            usersCollection.document(currentUserId)
                .update("chatThemes", chatThemes)
                .await()

            Log.d(TAG, "Chat theme set for $otherUserId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error setting chat theme", e)
            Result.failure(e)
        }
    }

    /**
     * 📞 Incrémenter le compteur d'appels
     */
    suspend fun incrementCallCount(userId: String): Result<Unit> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val callsMade = (userDoc.getLong("callsMade") ?: 0) + 1

            usersCollection.document(userId)
                .update("callsMade", callsMade)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing call count", e)
            Result.failure(e)
        }
    }

    /**
     * 💬 Incrémenter le compteur de messages
     */
    suspend fun incrementMessageCount(userId: String): Result<Unit> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val messagesSent = (userDoc.getLong("messagesSent") ?: 0) + 1

            usersCollection.document(userId)
                .update("messagesSent", messagesSent)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing message count", e)
            Result.failure(e)
        }
    }

    /**
     * 📸 Incrémenter le compteur de statuts
     */
    suspend fun incrementStatusCount(userId: String): Result<Unit> {
        return try {
            val userDoc = usersCollection.document(userId).get().await()
            val statusesPosted = (userDoc.getLong("statusesPosted") ?: 0) + 1

            usersCollection.document(userId)
                .update("statusesPosted", statusesPosted)
                .await()

            Result.success(Unit)
        } catch (e: Exception) {
            Log.e(TAG, "Error incrementing status count", e)
            Result.failure(e)
        }
    }
}
