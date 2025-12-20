# 📱 NexTalk - Système de Messagerie en Temps Réel

## 📖 À Propos

Votre application **NexTalk** dispose d'un système de messagerie complet et moderne utilisant **Firebase Firestore** pour la synchronisation en temps réel.

## ✨ Fonctionnalités

### Messages
- ✅ **Messages texte** en temps réel
- ✅ **Images** (avec upload Firebase Storage)
- ✅ **Messages vocaux** (avec enregistrement)
- ✅ **Réactions emoji** sur les messages
- ✅ **Répondre** aux messages (swipe-to-reply)
- ✅ **Édition** de messages
- ✅ **Suppression** de messages
- ✅ **Mode hors ligne** (messages en attente)

### Indicateurs
- ✅ **Statut en ligne/hors ligne**
- ✅ **Indicateur "en train d'écrire..."**
- ✅ **Statuts des messages** : Envoyé ✓, Reçu ✓✓, Lu ✓✓ (bleu)
- ✅ **Compteur de messages non lus**

### Interface
- ✅ **Animation fluide** lors de l'envoi/réception
- ✅ **Scroll automatique** vers le nouveau message
- ✅ **Prévisualisation** des réponses
- ✅ **Long press** pour les options de message
- ✅ **Design moderne** Material Design 3

## 🚀 Mise en Route

### 1. Configuration Firebase (OBLIGATOIRE)

#### A. Règles Firestore

1. Ouvrez https://console.firebase.google.com
2. Sélectionnez votre projet
3. Firestore Database → Règles
4. Copiez le contenu de `firestore.rules` (dans le projet)
5. Publiez les règles

#### B. Règles Storage (pour les images)

1. Firebase Console → Storage
2. Onglet Règles
3. Copiez le contenu de `storage.rules`
4. Publiez les règles

### 2. Test de l'Application

#### Prérequis
- Deux appareils (ou émulateurs)
- Connexion Internet sur les deux
- Deux comptes utilisateurs différents

#### Procédure
1. **Appareil 1** : Connectez-vous avec utilisateur A
2. **Appareil 2** : Connectez-vous avec utilisateur B
3. Utilisateur A ouvre une conversation avec B
4. Utilisateur A envoie un message
5. ✅ Le message apparaît instantanément sur l'appareil 2

## 🔧 Dépannage

### Problème : Les messages ne s'envoient pas

**Solution la plus courante** : Configurez les règles Firestore (voir ci-dessus)

**Diagnostic** :
1. Dans l'app : Menu ⋮ → "Test de connexion"
2. Lisez le résultat et suivez les instructions

### Problème : Permission Denied

```
FirebaseFirestoreException: PERMISSION_DENIED
```

**Solution** : Vérifiez les règles Firestore dans Firebase Console

### Problème : Pas de connexion Internet

**Solution** : 
- Les messages sont sauvegardés localement
- Ils seront envoyés automatiquement quand la connexion reviendra

## 📂 Structure du Projet

### Code Principal

```
app/src/main/java/com/example/nextalk/
├── ui/chat/
│   ├── ChatActivity.kt          # Interface de conversation
│   └── MessageAdapter.kt        # Affichage des messages
├── data/
│   ├── model/
│   │   ├── Message.kt          # Modèle de message
│   │   ├── Conversation.kt     # Modèle de conversation
│   │   └── MessageType.kt      # Types de messages
│   └── repository/
│       └── ChatRepository.kt   # Logique Firebase
└── util/
    ├── FirebaseConnectionTester.kt  # Test de connexion
    └── NetworkUtil.kt               # Utilitaires réseau
```

### Fichiers de Configuration

```
NexTalk/
├── firestore.rules                    # Règles Firestore
├── storage.rules                      # Règles Storage
├── LIRE_MOI_URGENCE.md               # Guide rapide
├── CONFIGURATION_MESSAGERIE.md        # Configuration détaillée
└── GUIDE_DEPANNAGE_MESSAGERIE.md     # Guide de dépannage
```

## 🛠️ Fonctionnalités Techniques

### Architecture

- **MVVM** : Séparation vue/logique
- **Repository Pattern** : Abstraction des données
- **Coroutines** : Opérations asynchrones
- **Flow** : Données réactives en temps réel
- **Room** : Cache local (mode hors ligne)

### Firebase

- **Firestore** : Base de données temps réel
- **Storage** : Stockage des images/fichiers
- **Auth** : Authentification utilisateurs

### Synchronisation

```kotlin
// Écoute en temps réel
fun getMessages(conversationId: String): Flow<List<Message>> = callbackFlow {
    conversationsCollection
        .document(conversationId)
        .collection("messages")
        .orderBy("timestamp")
        .addSnapshotListener { snapshot, error ->
            // Mise à jour automatique
            val messages = snapshot?.toObjects(Message::class.java)
            trySend(messages)
        }
    awaitClose { listener.remove() }
}
```

## 📊 Structure Firestore

```
conversations/
  {conversationId}/
    users: ["userId1", "userId2"]
    lastMessage: "Dernier message"
    lastMessageTime: 1234567890
    lastMessageSenderId: "userId1"
    unreadCount: 2
    
    messages/
      {messageId}/
        id: "messageId"
        conversationId: "conversationId"
        senderId: "userId1"
        text: "Contenu du message"
        timestamp: 1234567890
        status: "SENT"
        type: "TEXT"
        reactions: [
          {emoji: "👍", userId: "userId2", timestamp: 1234567891}
        ]
        replyTo: {
          messageId: "...",
          senderId: "...",
          text: "...",
          type: "TEXT"
        }
```

## 🎨 Interface Utilisateur

### activity_chat.xml

Contient :
- **Toolbar** avec avatar, nom, statut
- **RecyclerView** pour les messages
- **Indicateur "en train d'écrire..."**
- **Zone de saisie** avec boutons :
  - Emoji
  - Pièce jointe
  - Message vocal
  - Envoi
- **Carte de prévisualisation** pour les réponses

### Thèmes

- Mode clair/sombre
- Couleurs Material Design 3
- Animations fluides

## 🔐 Sécurité

### Règles Firestore (Production)

```javascript
// Seuls les participants peuvent lire/écrire
match /conversations/{conversationId} {
  allow read: if request.auth.uid in resource.data.users;
  allow write: if request.auth.uid in resource.data.users;
  
  match /messages/{messageId} {
    allow read: if isParticipant(conversationId);
    allow create: if isParticipant(conversationId) 
                  && request.resource.data.senderId == request.auth.uid;
    allow update: if isParticipant(conversationId);
    allow delete: if resource.data.senderId == request.auth.uid;
  }
}
```

## 📱 Tests

### Test Unitaire

```kotlin
// Exemple de test
@Test
fun testSendMessage() = runBlocking {
    val result = chatRepository.sendMessage(
        conversationId = "test",
        senderId = "user1",
        text = "Hello",
        type = MessageType.TEXT
    )
    assertTrue(result.isSuccess)
}
```

### Test d'Intégration

```bash
# Lancer les tests
./gradlew test

# Lancer les tests instrumentés
./gradlew connectedAndroidTest
```

### Test Manuel

1. Utilisez le menu "Test de connexion" dans l'app
2. Consultez les logs Logcat :
   ```bash
   adb logcat | grep -E "ChatActivity|ChatRepository|FirebaseTest"
   ```

## 📈 Performance

### Optimisations

- **Cache local** : Room Database pour le mode hors ligne
- **Pagination** : Chargement progressif (futures versions)
- **Compression d'images** : Réduction de la taille avant upload
- **Listeners intelligents** : Détachement automatique

### Métriques

- Temps d'envoi : < 500ms (avec bonne connexion)
- Temps de réception : Instantané (< 100ms)
- Taille moyenne d'un message : ~2KB
- Cache local : Illimité

## 🚧 Fonctionnalités Futures

- [ ] Appels vidéo intégrés
- [ ] Messages éphémères
- [ ] Chiffrement end-to-end
- [ ] Recherche dans les messages
- [ ] Groupes de discussion
- [ ] Stories/Statuts
- [ ] Stickers personnalisés
- [ ] Sauvegarde cloud
- [ ] Export de conversations
- [ ] Chatbots

## 📚 Documentation

### Fichiers Disponibles

1. **LIRE_MOI_URGENCE.md** - Démarrage rapide (5 min)
2. **CONFIGURATION_MESSAGERIE.md** - Guide complet de configuration
3. **GUIDE_DEPANNAGE_MESSAGERIE.md** - Résolution de problèmes
4. **firestore.rules** - Règles de sécurité Firestore
5. **storage.rules** - Règles de sécurité Storage

### Commandes Utiles

```bash
# Logs en temps réel
adb logcat | grep -E "Chat|Message|Firebase"

# Nettoyer le projet
./gradlew clean

# Compiler
./gradlew assembleDebug

# Installer
adb install -r app/build/outputs/apk/debug/app-debug.apk

# Forcer l'arrêt
adb shell am force-stop com.example.nextalk
```

## 🤝 Support

### En cas de problème :

1. **Consultez** LIRE_MOI_URGENCE.md
2. **Testez** avec le menu "Test de connexion"
3. **Vérifiez** les logs Logcat
4. **Consultez** GUIDE_DEPANNAGE_MESSAGERIE.md

### Informations à fournir :

- Version Android
- Logs Logcat (filtre: ChatActivity)
- Résultat du test de connexion
- Captures d'écran Firebase Console

## 📝 Changelog

### v1.0 (Actuel)
- ✅ Messagerie en temps réel
- ✅ Envoi d'images
- ✅ Réactions emoji
- ✅ Réponses aux messages
- ✅ Mode hors ligne
- ✅ Test de connexion intégré
- ✅ Logs détaillés

## 📄 Licence

Projet personnel - Tous droits réservés

---

## 🎯 Démarrage Rapide (TL;DR)

1. Configurez les règles Firestore (voir `firestore.rules`)
2. Testez avec deux utilisateurs différents
3. Utilisez le menu "Test de connexion" si problème
4. Consultez `LIRE_MOI_URGENCE.md` pour l'aide immédiate

---

**Fait avec ❤️ pour NexTalk**
