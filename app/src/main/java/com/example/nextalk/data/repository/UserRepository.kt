package com.example.nextalk.data.repository

import android.util.Log
import com.example.nextalk.data.local.dao.UserDao
import com.example.nextalk.data.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.tasks.await

class UserRepository(private val userDao: UserDao) {

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
}
