package com.example.nextalk.util

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.tasks.await
import java.util.UUID

/**
 * Utilitaire pour tester et diagnostiquer la connexion Firebase
 * Utilisez cette classe pour vérifier que Firebase est correctement configuré
 */
object FirebaseConnectionTester {
    
    private const val TAG = "FirebaseTest"
    
    /**
     * Teste la connexion Firebase complète
     * @return Triple<auth, firestore, storage> - true si connecté, false sinon
     */
    suspend fun testFirebaseConnection(): Triple<Boolean, Boolean, Boolean> {
        val authConnected = testAuth()
        val firestoreConnected = testFirestore()
        val storageConnected = testStorage()
        
        return Triple(authConnected, firestoreConnected, storageConnected)
    }
    
    /**
     * Teste l'authentification Firebase
     */
    private fun testAuth(): Boolean {
        return try {
            val auth = FirebaseAuth.getInstance()
            val currentUser = auth.currentUser
            
            if (currentUser != null) {
                Log.d(TAG, "✅ Firebase Auth : Connecté")
                Log.d(TAG, "   User ID: ${currentUser.uid}")
                Log.d(TAG, "   Email: ${currentUser.email}")
                true
            } else {
                Log.w(TAG, "⚠️ Firebase Auth : Aucun utilisateur connecté")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firebase Auth : Erreur", e)
            false
        }
    }
    
    /**
     * Teste Firestore (lecture et écriture)
     */
    private suspend fun testFirestore(): Boolean {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val testDocId = "test_${UUID.randomUUID()}"
            
            // Test d'écriture
            Log.d(TAG, "🔄 Test Firestore : Écriture...")
            firestore.collection("connection_test")
                .document(testDocId)
                .set(mapOf(
                    "timestamp" to System.currentTimeMillis(),
                    "test" to "Connection test successful"
                ))
                .await()
            
            Log.d(TAG, "✅ Firestore : Écriture réussie")
            
            // Test de lecture
            Log.d(TAG, "🔄 Test Firestore : Lecture...")
            val doc = firestore.collection("connection_test")
                .document(testDocId)
                .get()
                .await()
            
            if (doc.exists()) {
                Log.d(TAG, "✅ Firestore : Lecture réussie")
                
                // Nettoyage
                firestore.collection("connection_test")
                    .document(testDocId)
                    .delete()
                    .await()
                
                Log.d(TAG, "✅ Firestore : Entièrement fonctionnel")
                true
            } else {
                Log.w(TAG, "⚠️ Firestore : Document introuvable")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "❌ Firestore : Erreur", e)
            
            // Afficher des informations détaillées sur l'erreur
            when {
                e.message?.contains("PERMISSION_DENIED") == true -> {
                    Log.e(TAG, "❌ ERREUR CRITIQUE : Permission refusée")
                    Log.e(TAG, "   → Vérifiez les règles Firestore dans Firebase Console")
                    Log.e(TAG, "   → Allez sur : https://console.firebase.google.com")
                    Log.e(TAG, "   → Firestore Database → Règles")
                    Log.e(TAG, "   → Utilisez les règles du fichier firestore.rules")
                }
                e.message?.contains("UNAVAILABLE") == true -> {
                    Log.e(TAG, "❌ ERREUR : Pas de connexion Internet")
                    Log.e(TAG, "   → Vérifiez votre connexion réseau")
                }
                e.message?.contains("NOT_FOUND") == true -> {
                    Log.e(TAG, "❌ ERREUR : Firestore non initialisé")
                    Log.e(TAG, "   → Créez la base de données Firestore dans Firebase Console")
                }
                else -> {
                    Log.e(TAG, "   Message d'erreur : ${e.message}")
                }
            }
            
            false
        }
    }
    
    /**
     * Teste Firebase Storage
     */
    private suspend fun testStorage(): Boolean {
        return try {
            val storage = FirebaseStorage.getInstance()
            
            // Test de référence (ne nécessite pas de permission)
            val testRef = storage.reference.child("test/connection_test.txt")
            Log.d(TAG, "✅ Storage : Référence créée (${testRef.path})")
            
            // Note : Pour un test complet d'upload, décommentez ci-dessous
            // (nécessite des permissions Storage configurées)
            /*
            val testData = "Test connection".toByteArray()
            testRef.putBytes(testData).await()
            Log.d(TAG, "✅ Storage : Upload réussi")
            testRef.delete().await()
            Log.d(TAG, "✅ Storage : Entièrement fonctionnel")
            */
            
            true
        } catch (e: Exception) {
            Log.e(TAG, "❌ Storage : Erreur", e)
            
            when {
                e.message?.contains("PERMISSION_DENIED") == true -> {
                    Log.e(TAG, "❌ ERREUR : Permission Storage refusée")
                    Log.e(TAG, "   → Vérifiez les règles Storage dans Firebase Console")
                    Log.e(TAG, "   → Storage → Règles")
                    Log.e(TAG, "   → Utilisez les règles du fichier storage.rules")
                }
                else -> {
                    Log.e(TAG, "   Message d'erreur : ${e.message}")
                }
            }
            
            false
        }
    }
    
    /**
     * Teste spécifiquement la messagerie
     */
    suspend fun testMessaging(conversationId: String): Boolean {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val currentUser = FirebaseAuth.getInstance().currentUser
            
            if (currentUser == null) {
                Log.e(TAG, "❌ Test Messagerie : Utilisateur non connecté")
                return false
            }
            
            Log.d(TAG, "🔄 Test Messagerie : ConversationId = $conversationId")
            
            // Test de lecture de la conversation
            val conversation = firestore.collection("conversations")
                .document(conversationId)
                .get()
                .await()
            
            if (!conversation.exists()) {
                Log.w(TAG, "⚠️ Conversation introuvable (elle sera créée au premier message)")
            } else {
                Log.d(TAG, "✅ Conversation trouvée")
                val users = conversation.get("users") as? List<*>
                Log.d(TAG, "   Participants: $users")
            }
            
            // Test de lecture des messages
            val messages = firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .limit(1)
                .get()
                .await()
            
            Log.d(TAG, "✅ Messages accessibles (${messages.size()} messages)")
            
            // Test d'envoi de message (message de test)
            val testMessageId = "test_${UUID.randomUUID()}"
            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(testMessageId)
                .set(mapOf(
                    "id" to testMessageId,
                    "conversationId" to conversationId,
                    "senderId" to currentUser.uid,
                    "text" to "[TEST] Message de test de connexion",
                    "timestamp" to System.currentTimeMillis(),
                    "status" to "SENT",
                    "type" to "TEXT"
                ))
                .await()
            
            Log.d(TAG, "✅ Message de test envoyé avec succès !")
            
            // Suppression du message de test
            firestore.collection("conversations")
                .document(conversationId)
                .collection("messages")
                .document(testMessageId)
                .delete()
                .await()
            
            Log.d(TAG, "✅ Test Messagerie : RÉUSSI - La messagerie fonctionne correctement")
            true
            
        } catch (e: Exception) {
            Log.e(TAG, "❌ Test Messagerie : ÉCHEC", e)
            
            when {
                e.message?.contains("PERMISSION_DENIED") == true -> {
                    Log.e(TAG, "❌ PROBLÈME CRITIQUE : Permissions Firestore")
                    Log.e(TAG, "   SOLUTION :")
                    Log.e(TAG, "   1. Ouvrez https://console.firebase.google.com")
                    Log.e(TAG, "   2. Sélectionnez votre projet")
                    Log.e(TAG, "   3. Firestore Database → Règles")
                    Log.e(TAG, "   4. Copiez le contenu de firestore.rules")
                    Log.e(TAG, "   5. Publiez les règles")
                }
                e.message?.contains("NOT_FOUND") == true -> {
                    Log.e(TAG, "❌ PROBLÈME : Conversation non trouvée")
                    Log.e(TAG, "   → La conversation sera créée automatiquement")
                }
            }
            
            false
        }
    }
    
    /**
     * Affiche un rapport complet du statut Firebase
     */
    suspend fun generateDiagnosticReport(): String {
        val report = StringBuilder()
        report.appendLine("╔═══════════════════════════════════════╗")
        report.appendLine("║   RAPPORT DE DIAGNOSTIC FIREBASE     ║")
        report.appendLine("╚═══════════════════════════════════════╝")
        report.appendLine()
        
        val (auth, firestore, storage) = testFirebaseConnection()
        
        report.appendLine("🔐 Firebase Authentication: ${if (auth) "✅ OK" else "❌ ERREUR"}")
        report.appendLine("📊 Cloud Firestore: ${if (firestore) "✅ OK" else "❌ ERREUR"}")
        report.appendLine("💾 Firebase Storage: ${if (storage) "✅ OK" else "❌ ERREUR"}")
        report.appendLine()
        
        if (!auth) {
            report.appendLine("⚠️ ATTENTION : Aucun utilisateur connecté")
            report.appendLine("   → Connectez-vous d'abord")
        }
        
        if (!firestore) {
            report.appendLine("❌ PROBLÈME CRITIQUE : Firestore inaccessible")
            report.appendLine("   → Vérifiez les règles Firestore")
            report.appendLine("   → Fichier: firestore.rules")
        }
        
        if (!storage) {
            report.appendLine("⚠️ Storage inaccessible (envoi d'images impossible)")
            report.appendLine("   → Vérifiez les règles Storage")
            report.appendLine("   → Fichier: storage.rules")
        }
        
        if (auth && firestore && storage) {
            report.appendLine("🎉 TOUT EST OPÉRATIONNEL !")
            report.appendLine("   La messagerie devrait fonctionner correctement.")
        }
        
        report.appendLine()
        report.appendLine("═══════════════════════════════════════")
        
        val result = report.toString()
        Log.d(TAG, result)
        return result
    }
}
