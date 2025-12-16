package com.example.nextalk.data.repository

import android.app.Activity
import com.example.nextalk.data.model.User
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit

class AuthRepository {

    private val auth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val usersCollection = firestore.collection("users")

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    // ==================== EMAIL/PASSWORD AUTH ====================

    suspend fun register(
        email: String,
        password: String,
        name: String,
        birthDate: Long
    ): Result<User> {
        return try {
            val result = auth.createUserWithEmailAndPassword(email, password).await()
            val firebaseUser = result.user ?: throw Exception("Erreur de création de compte")

            val user = User(
                uid = firebaseUser.uid,
                name = name,
                email = email,
                birthDate = birthDate,
                createdAt = System.currentTimeMillis()
            )

            usersCollection.document(firebaseUser.uid).set(user.toMap()).await()
            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun login(email: String, password: String): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithEmailAndPassword(email, password).await()
            val user = result.user ?: throw Exception("Erreur de connexion")

            usersCollection.document(user.uid).update(
                mapOf(
                    "isOnline" to true,
                    "lastSeen" to System.currentTimeMillis()
                )
            ).await()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== GOOGLE AUTH ====================

    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Erreur de connexion Google")

            // Vérifier si l'utilisateur existe déjà
            val userDoc = usersCollection.document(firebaseUser.uid).get().await()

            if (!userDoc.exists()) {
                // Créer un nouvel utilisateur
                val user = User(
                    uid = firebaseUser.uid,
                    name = firebaseUser.displayName ?: "Utilisateur",
                    email = firebaseUser.email ?: "",
                    photoUrl = firebaseUser.photoUrl?.toString() ?: "",
                    createdAt = System.currentTimeMillis()
                )
                usersCollection.document(firebaseUser.uid).set(user.toMap()).await()
            }

            // Mettre à jour le statut en ligne
            usersCollection.document(firebaseUser.uid).update(
                mapOf(
                    "isOnline" to true,
                    "lastSeen" to System.currentTimeMillis()
                )
            ).await()

            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // ==================== PHONE AUTH ====================

    fun sendVerificationCode(
        phoneNumber: String,
        activity: Activity,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    fun resendVerificationCode(
        phoneNumber: String,
        activity: Activity,
        token: PhoneAuthProvider.ForceResendingToken,
        callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks
    ) {
        val options = PhoneAuthOptions.newBuilder(auth)
            .setPhoneNumber(phoneNumber)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(activity)
            .setCallbacks(callbacks)
            .setForceResendingToken(token)
            .build()

        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential): Result<FirebaseUser> {
        return try {
            val result = auth.signInWithCredential(credential).await()
            val firebaseUser = result.user ?: throw Exception("Erreur de connexion par téléphone")

            // Vérifier si l'utilisateur existe déjà
            val userDoc = usersCollection.document(firebaseUser.uid).get().await()

            if (!userDoc.exists()) {
                // Créer un nouvel utilisateur
                val user = User(
                    uid = firebaseUser.uid,
                    name = "Utilisateur",
                    email = "",
                    photoUrl = "",
                    createdAt = System.currentTimeMillis()
                )
                usersCollection.document(firebaseUser.uid).set(user.toMap()).await()
            }

            // Mettre à jour le statut en ligne
            usersCollection.document(firebaseUser.uid).update(
                mapOf(
                    "isOnline" to true,
                    "lastSeen" to System.currentTimeMillis()
                )
            ).await()

            Result.success(firebaseUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getPhoneCredential(verificationId: String, code: String): PhoneAuthCredential {
        return PhoneAuthProvider.getCredential(verificationId, code)
    }

    // ==================== OTHER METHODS ====================

    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            auth.sendPasswordResetEmail(email).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logout(): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid
            if (uid != null) {
                usersCollection.document(uid).update(
                    mapOf(
                        "isOnline" to false,
                        "lastSeen" to System.currentTimeMillis()
                    )
                ).await()
            }
            auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(name: String, photoUrl: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Non connecté")
            val updates = mutableMapOf<String, Any>("name" to name)
            if (photoUrl.isNotEmpty()) {
                updates["photoUrl"] = photoUrl
            }
            usersCollection.document(uid).update(updates).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateFcmToken(token: String): Result<Unit> {
        return try {
            val uid = auth.currentUser?.uid ?: throw Exception("Non connecté")
            usersCollection.document(uid).update("fcmToken", token).await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeAuthState(): Flow<FirebaseUser?> = callbackFlow {
        val listener = FirebaseAuth.AuthStateListener { auth ->
            trySend(auth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }
}
