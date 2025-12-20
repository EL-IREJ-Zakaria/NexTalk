# 🚀 Configuration de la Messagerie en Temps Réel

## ✅ État Actuel
Votre application **NexTalk** utilise Firebase Firestore pour la messagerie en temps réel. Le code est déjà implémenté et fonctionnel !

## 📋 Comment ça fonctionne

### 1. Envoi de messages
Quand vous envoyez un message dans `ChatActivity` :
```kotlin
// Dans ChatActivity.kt ligne 560-604
private fun sendMessage(text: String) {
    // Le message est envoyé à Firebase Firestore
    chatRepository.sendMessage(
        conversationId = conversationId,
        senderId = currentUserId,
        text = text,
        type = MessageType.TEXT
    )
}
```

### 2. Réception en temps réel
Les messages sont reçus instantanément grâce aux listeners Firebase :
```kotlin
// Dans ChatRepository.kt ligne 113-141
fun getMessages(conversationId: String): Flow<List<Message>> {
    // Écoute en temps réel des nouveaux messages
    conversationsCollection
        .document(conversationId)
        .collection("messages")
        .orderBy("timestamp")
        .addSnapshotListener { ... }
}
```

## 🔧 Configuration Requise

### Étape 1 : Règles Firestore
**IMPORTANT** : Vous DEVEZ configurer les règles de sécurité Firestore pour permettre la lecture/écriture.

#### 🌐 Accédez à la Console Firebase
1. Allez sur https://console.firebase.google.com
2. Sélectionnez votre projet **NexTalk**
3. Dans le menu de gauche, cliquez sur **Firestore Database**
4. Allez dans l'onglet **Règles** (Rules)

#### 📝 Règles Recommandées

**Pour le développement (permissif) :**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    // Permet à tous les utilisateurs authentifiés de lire/écrire
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

**Pour la production (plus sécurisé) :**
```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    
    // Collection des utilisateurs
    match /users/{userId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null && request.auth.uid == userId;
    }
    
    // Collection des conversations
    match /conversations/{conversationId} {
      // Un utilisateur peut lire une conversation s'il en fait partie
      allow read: if request.auth != null && 
                    request.auth.uid in resource.data.users;
      
      // Un utilisateur peut créer/modifier une conversation s'il en fait partie
      allow write: if request.auth != null && 
                     request.auth.uid in resource.data.users;
      
      // Messages dans une conversation
      match /messages/{messageId} {
        // Peut lire si fait partie de la conversation
        allow read: if request.auth != null && 
                      request.auth.uid in get(/databases/$(database)/documents/conversations/$(conversationId)).data.users;
        
        // Peut écrire si fait partie de la conversation
        allow create: if request.auth != null && 
                        request.auth.uid in get(/databases/$(database)/documents/conversations/$(conversationId)).data.users;
        
        // Peut modifier/supprimer uniquement ses propres messages
        allow update, delete: if request.auth != null && 
                                 request.auth.uid == resource.data.senderId;
      }
    }
    
    // Collection des statuts
    match /statuses/{statusId} {
      allow read: if request.auth != null;
      allow write: if request.auth != null;
    }
    
    // Collection des appels
    match /calls/{callId} {
      allow read, write: if request.auth != null;
    }
  }
}
```

**⚠️ Important** : Copiez ces règles et publiez-les dans votre Console Firebase.

### Étape 2 : Règles Firebase Storage
Pour l'envoi d'images et de messages vocaux :

1. Dans Firebase Console, allez dans **Storage**
2. Onglet **Rules**
3. Utilisez ces règles :

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      // Permet le téléchargement si authentifié
      allow read: if request.auth != null;
      allow write: if request.auth != null && 
                     request.resource.size < 10 * 1024 * 1024; // Max 10 MB
    }
  }
}
```

### Étape 3 : Vérification de la connexion

#### Test de connexion Firebase
Ajoutez ce code temporaire dans `ChatActivity.onCreate()` pour vérifier :

```kotlin
// Test de connexion Firestore
lifecycleScope.launch {
    try {
        firestore.collection("test").document("test").set(mapOf("test" to true)).await()
        Log.d(TAG, "✅ Firestore connecté avec succès !")
    } catch (e: Exception) {
        Log.e(TAG, "❌ Erreur Firestore : ${e.message}")
    }
}
```

## 🐛 Résolution des Problèmes

### Problème 1 : Les messages ne s'affichent pas
**Causes possibles :**
- ✅ Vérifiez que les deux utilisateurs sont connectés au même `conversationId`
- ✅ Vérifiez les règles Firestore (voir ci-dessus)
- ✅ Vérifiez la connexion Internet
- ✅ Vérifiez les logs Logcat pour les erreurs

**Solution :**
```bash
# Dans Logcat, filtrez par "ChatRepository" ou "ChatActivity"
# Cherchez les erreurs de permission ou de connexion
```

### Problème 2 : "Permission Denied"
**Cause** : Règles Firestore trop restrictives

**Solution** : Utilisez les règles de développement ci-dessus (temporairement)

### Problème 3 : Les messages s'envoient mais ne s'affichent pas
**Cause** : Problème d'écoute des messages

**Vérification :**
1. Ouvrez Firebase Console
2. Allez dans Firestore Database
3. Vérifiez que la structure est : `conversations/{conversationId}/messages/{messageId}`
4. Vérifiez que les messages sont bien enregistrés

## 📊 Structure Firestore Attendue

```
firestore/
├── conversations/
│   ├── {conversationId}/
│   │   ├── users: ["userId1", "userId2"]
│   │   ├── lastMessage: "Dernier message"
│   │   ├── lastMessageTime: 1234567890
│   │   └── messages/
│   │       ├── {messageId}/
│   │       │   ├── id: "messageId"
│   │       │   ├── senderId: "userId1"
│   │       │   ├── text: "Contenu du message"
│   │       │   ├── timestamp: 1234567890
│   │       │   ├── status: "SENT"
│   │       │   └── type: "TEXT"
│   │       └── ...
│   └── ...
├── users/
│   ├── {userId}/
│   │   ├── id: "userId"
│   │   ├── name: "Nom"
│   │   ├── email: "email@example.com"
│   │   ├── photoUrl: "https://..."
│   │   └── isOnline: true
│   └── ...
└── ...
```

## 🧪 Test Complet

### Test 1 : Envoi de message
1. Connectez-vous avec l'utilisateur A sur un appareil/émulateur
2. Connectez-vous avec l'utilisateur B sur un autre appareil/émulateur
3. Utilisateur A ouvre une conversation avec B
4. Utilisateur A envoie un message "Hello"
5. ✅ Le message devrait apparaître instantanément chez B

### Test 2 : Vérification dans Firebase Console
1. Ouvrez Firebase Console
2. Allez dans Firestore Database
3. Naviguez vers `conversations/{conversationId}/messages`
4. ✅ Vous devriez voir tous les messages avec leurs données

### Test 3 : Logs
Dans Logcat, filtrez par "ChatRepository" et cherchez :
```
✅ Message envoyé avec succès
✅ Nouveaux messages reçus : [...]
❌ Error sending message (si erreur)
```

## 🎯 Fonctionnalités Déjà Implémentées

Votre application supporte déjà :
- ✅ Messages texte en temps réel
- ✅ Messages images
- ✅ Réactions emoji
- ✅ Réponses aux messages (swipe-to-reply)
- ✅ Indicateur "en train d'écrire"
- ✅ Status en ligne/hors ligne
- ✅ Marquage des messages comme lus
- ✅ Mode hors ligne (les messages seront envoyés plus tard)

## 📱 Prochaines Étapes

1. **Configurez les règles Firestore** (URGENT - voir Étape 1)
2. **Testez avec deux appareils** différents
3. **Vérifiez les logs** pour identifier les erreurs
4. **Testez la connexion Internet** sur les deux appareils
5. **Vérifiez Firebase Console** pour voir si les messages sont enregistrés

## 🆘 Besoin d'aide ?

Si après avoir suivi ce guide les messages ne fonctionnent toujours pas :

1. **Partagez les logs Logcat** avec le filtre "ChatRepository"
2. **Vérifiez Firebase Console** - allez dans Firestore et regardez si les données sont créées
3. **Vérifiez l'authentification** - assurez-vous que les deux utilisateurs sont connectés
4. **Testez la connexion** avec le code de test fourni ci-dessus

## 🔥 Commandes Utiles

```bash
# Voir les logs en temps réel
adb logcat | grep -i "Chat\|Message\|Firebase"

# Nettoyer et reconstruire
./gradlew clean
./gradlew assembleDebug

# Installer sur l'appareil
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

---

**Note** : Le code de messagerie est déjà complet et fonctionnel. Le problème vient probablement des règles Firestore qui bloquent l'accès. Suivez l'**Étape 1** en priorité ! 🚀
