# 📂 Structure du Projet NexTalk

## 🌳 Arborescence Complète

```
NexTalk/
│
├── 📄 Documentation
│   ├── CHAT_FEATURES.md              ← Guide complet des fonctionnalités
│   ├── CHAT_MODERNIZATION.md         ← Résumé exécutif
│   ├── CHANGELOG.md                  ← Historique des changements
│   ├── FILES_CHANGED.md              ← Liste des fichiers modifiés
│   ├── GUIDE_UTILISATEUR.md          ← Manuel utilisateur
│   ├── IMPLEMENTATION_SUMMARY.md     ← Détails techniques
│   ├── NOUVELLES_FONCTIONNALITES.md  ← Focus sur nouvelles features
│   ├── README_CHAT_UPDATE.md         ← Vue d'ensemble update
│   ├── STRUCTURE_PROJET.md           ← Ce fichier
│   └── README.md                     ← README principal
│
├── 📁 app/
│   │
│   ├── 📁 src/main/
│   │   │
│   │   ├── 📁 java/com/example/nextalk/
│   │   │   │
│   │   │   ├── 📁 data/
│   │   │   │   ├── 📁 local/
│   │   │   │   │   ├── 📁 dao/
│   │   │   │   │   │   ├── ConversationDao.kt
│   │   │   │   │   │   ├── MessageDao.kt
│   │   │   │   │   │   └── UserDao.kt
│   │   │   │   │   ├── Converters.kt          ✏️ MODIFIÉ
│   │   │   │   │   ├── NexTalkDatabase.kt
│   │   │   │   │   └── ...
│   │   │   │   │
│   │   │   │   ├── 📁 model/
│   │   │   │   │   ├── Conversation.kt
│   │   │   │   │   ├── Message.kt             ✏️ MODIFIÉ
│   │   │   │   │   ├── MessageReaction.kt     🆕 NOUVEAU
│   │   │   │   │   ├── ReplyInfo.kt           🆕 NOUVEAU
│   │   │   │   │   ├── User.kt
│   │   │   │   │   └── ...
│   │   │   │   │
│   │   │   │   ├── 📁 preferences/
│   │   │   │   │   └── PreferencesManager.kt
│   │   │   │   │
│   │   │   │   └── 📁 repository/
│   │   │   │       ├── AuthRepository.kt
│   │   │   │       ├── ChatRepository.kt      ✏️ MODIFIÉ
│   │   │   │       └── UserRepository.kt
│   │   │   │
│   │   │   ├── 📁 ui/
│   │   │   │   │
│   │   │   │   ├── 📁 auth/
│   │   │   │   │   ├── AuthViewModel.kt
│   │   │   │   │   ├── LoginActivity.kt
│   │   │   │   │   ├── RegisterActivity.kt
│   │   │   │   │   └── ...
│   │   │   │   │
│   │   │   │   ├── 📁 chat/
│   │   │   │   │   ├── ChatActivity.kt        ✏️ MODIFIÉ
│   │   │   │   │   ├── MessageAdapter.kt      ✏️ MODIFIÉ
│   │   │   │   │   └── ...
│   │   │   │   │
│   │   │   │   ├── 📁 main/
│   │   │   │   │   ├── MainActivity.kt
│   │   │   │   │   ├── ConversationAdapter.kt
│   │   │   │   │   └── ...
│   │   │   │   │
│   │   │   │   ├── 📁 profile/
│   │   │   │   │   ├── ProfileActivity.kt
│   │   │   │   │   └── ...
│   │   │   │   │
│   │   │   │   ├── 📁 users/
│   │   │   │   │   ├── UsersActivity.kt       ✏️ MODIFIÉ
│   │   │   │   │   ├── UsersAdapter.kt        ✏️ MODIFIÉ
│   │   │   │   │   └── ...
│   │   │   │   │
│   │   │   │   └── 📁 onboarding/
│   │   │   │       └── ...
│   │   │   │
│   │   │   ├── 📁 service/
│   │   │   │   └── ...
│   │   │   │
│   │   │   ├── 📁 util/
│   │   │   │   └── ...
│   │   │   │
│   │   │   ├── 📁 worker/
│   │   │   │   └── ...
│   │   │   │
│   │   │   └── NexTalkApplication.kt
│   │   │
│   │   ├── 📁 res/
│   │   │   │
│   │   │   ├── 📁 drawable/
│   │   │   │   ├── ic_mic.xml                 🆕 NOUVEAU
│   │   │   │   ├── ic_reply.xml               🆕 NOUVEAU
│   │   │   │   ├── ic_add_reaction.xml        🆕 NOUVEAU
│   │   │   │   ├── ic_play.xml                🆕 NOUVEAU
│   │   │   │   ├── ic_pause.xml               🆕 NOUVEAU
│   │   │   │   ├── ic_copy.xml                🆕 NOUVEAU
│   │   │   │   ├── ic_delete.xml              🆕 NOUVEAU
│   │   │   │   ├── bg_reaction_bubble.xml     🆕 NOUVEAU
│   │   │   │   ├── bg_reply_preview.xml       🆕 NOUVEAU
│   │   │   │   ├── bg_voice_message.xml       🆕 NOUVEAU
│   │   │   │   └── ... (autres drawables)
│   │   │   │
│   │   │   ├── 📁 layout/
│   │   │   │   ├── activity_chat.xml          ✏️ MODIFIÉ
│   │   │   │   ├── activity_users.xml         ✏️ MODIFIÉ
│   │   │   │   ├── item_message_sent.xml      ✏️ MODIFIÉ
│   │   │   │   ├── item_message_received.xml  ✏️ MODIFIÉ
│   │   │   │   ├── item_user.xml              ✏️ MODIFIÉ
│   │   │   │   ├── activity_main.xml
│   │   │   │   ├── activity_profile.xml
│   │   │   │   └── ... (autres layouts)
│   │   │   │
│   │   │   ├── 📁 menu/
│   │   │   │   ├── menu_message_options.xml   🆕 NOUVEAU
│   │   │   │   └── menu_main.xml
│   │   │   │
│   │   │   ├── 📁 values/
│   │   │   │   ├── strings.xml                ✏️ MODIFIÉ
│   │   │   │   ├── colors.xml
│   │   │   │   └── themes.xml                 ✏️ MODIFIÉ
│   │   │   │
│   │   │   ├── 📁 values-night/
│   │   │   │   └── themes.xml
│   │   │   │
│   │   │   ├── 📁 mipmap-*/
│   │   │   │   └── (icônes de l'app)
│   │   │   │
│   │   │   └── AndroidManifest.xml
│   │   │
│   │   └── google-services.json
│   │
│   ├── 📄 build.gradle.kts           ✏️ MODIFIÉ
│   ├── 📄 proguard-rules.pro
│   └── ...
│
├── 📁 gradle/
│   ├── 📁 wrapper/
│   └── libs.versions.toml
│
├── 📄 build.gradle.kts
├── 📄 settings.gradle.kts
├── 📄 gradle.properties
├── 📄 gradlew
├── 📄 gradlew.bat
├── 📄 local.properties
├── 📄 README.md
└── ...
```

---

## 📊 Statistiques Détaillées

### Par Dossier

#### `data/`
```
Fichiers modifiés:    3 (Message, Converters, ChatRepository)
Fichiers créés:       2 (MessageReaction, ReplyInfo)
Total lignes ajoutées: ~400
```

#### `ui/chat/`
```
Fichiers modifiés:    2 (ChatActivity, MessageAdapter)
Fichiers créés:       0
Total lignes ajoutées: ~800
```

#### `ui/users/`
```
Fichiers modifiés:    2 (UsersActivity, UsersAdapter)
Fichiers créés:       0
Total lignes ajoutées: ~300
```

#### `res/drawable/`
```
Fichiers créés:       10 (icônes et formes)
Formats:              10 × XML
Total taille:         ~8 KB
```

#### `res/layout/`
```
Fichiers modifiés:    5 (activity_chat, messages, users)
Fichiers créés:       1 (menu_message_options)
Total lignes ajoutées: ~600
```

#### `res/menu/`
```
Fichiers créés:       1 (menu_message_options.xml)
Options:              5 (reply, react, copy, edit, delete)
```

---

## 🔄 Dépendances Entre Fichiers

```
ChatActivity
├── MessageAdapter
│   ├── Message
│   │   ├── MessageReaction
│   │   ├── ReplyInfo
│   │   ├── MessageStatus
│   │   └── MessageType
│   └── Glide
├── ChatRepository
│   ├── Firebase Firestore
│   └── Message
├── UserRepository
│   └── User
└── AuthRepository

UsersActivity
├── UsersAdapter
│   ├── User
│   └── Glide
├── ChatRepository
└── UserRepository

ChatRepository
├── Message
├── MessageReaction
├── ReplyInfo
├── Firebase Firestore
└── Firebase Storage
```

---

## 🎯 Fichiers Clés par Fonctionnalité

### Réactions Emoji
```
Message.kt              (propriété reactions)
MessageReaction.kt      (classe de réaction)
MessageAdapter.kt       (affichage chips)
ChatRepository.kt       (update réactions)
menu_message_options.xml (option ajouter réaction)
```

### Répondre aux Messages
```
Message.kt              (propriété replyTo)
ReplyInfo.kt            (classe de réponse)
MessageAdapter.kt       (affichage réponse)
ChatActivity.kt         (swipe + menu)
activity_chat.xml       (zone réponse)
item_message_*.xml      (aperçu réponse)
```

### Messages Vocaux
```
Message.kt              (voiceUrl, voiceDuration)
MessageAdapter.kt       (affichage lecteur)
ChatActivity.kt         (bouton micro)
ChatRepository.kt       (sendVoiceMessage)
ic_play.xml, ic_pause.xml (contrôles)
```

### Design Premium
```
activity_chat.xml       (layout moderne)
item_message_*.xml      (cartes)
activity_users.xml      (search moderne)
item_user.xml           (cards utilisateurs)
themes.xml              (styles)
Tous les drawables      (icônes)
```

---

## 📋 Checklist de Navigation

### Pour Ajouter une Nouvelle Fonctionnalité
- [ ] Modifier `Message.kt` si besoin de nouvelles données
- [ ] Ajouter des Converters si structures complexes
- [ ] Ajouter des méthodes dans `ChatRepository.kt`
- [ ] Mettre à jour `MessageAdapter.kt` pour l'affichage
- [ ] Ajouter la logique dans `ChatActivity.kt`
- [ ] Créer les drawables nécessaires
- [ ] Ajouter les strings dans `strings.xml`
- [ ] Ajouter les styles dans `themes.xml`
- [ ] Documenter dans les fichiers MD

### Pour Corriger un Bug
- [ ] Identifier le composant affecté (voir dépendances)
- [ ] Vérifier `Message.kt` et les modèles
- [ ] Vérifier `ChatRepository.kt` pour la logique métier
- [ ] Vérifier le code UI correspondant
- [ ] Tester avec les cas limites
- [ ] Documenter la correction

### Pour Optimiser les Performances
- [ ] Profiler avec Android Profiler
- [ ] Vérifier `MessageAdapter.kt` (RecyclerView)
- [ ] Vérifier `ChatRepository.kt` (requêtes Firebase)
- [ ] Vérifier les animations dans les layouts
- [ ] Mesurer l'impact mémoire
- [ ] Documenter les changements

---

## 🧪 Fichiers de Test (À Ajouter)

```
app/src/test/
├── java/com/example/nextalk/
│   ├── data/
│   │   ├── repository/
│   │   │   └── ChatRepositoryTest.kt
│   │   └── model/
│   │       └── MessageTest.kt
│   └── ui/
│       ├── chat/
│       │   └── ChatActivityTest.kt
│       └── users/
│           └── UsersActivityTest.kt
│
└── resources/
    └── (fichiers de test)
```

---

## 🔐 Fichiers Sensibles

### Ne Pas Commiter
```
local.properties         (clés locales)
google-services.json    (clés Firebase)
.gradle/                (cache)
build/                  (artifacts)
.idea/                  (configuration IDE)
*.keystore              (clés signing)
```

### À Protéger en Production
```
app/google-services.json
Clés API Firebase
Secrets en général
```

---

## 📦 Dépendances Externes

### Firebase
```
firebase-auth
firebase-firestore
firebase-storage
firebase-messaging
```

### AndroidX
```
androidx.core
androidx.appcompat
androidx.constraintlayout
androidx.lifecycle
androidx.recyclerview
androidx.navigation
androidx.datastore
androidx.credentials
```

### Material Design
```
com.google.android.material
```

### Autres
```
com.github.bumptech.glide (Glide)
de.hdodenhof.circleimageview
com.google.code.gson (JSON)
```

---

## 🚀 Commandes Utiles

### Compiler
```bash
./gradlew build
```

### Tester
```bash
./gradlew test
```

### Nettoyer
```bash
./gradlew clean
```

### Analyser Lint
```bash
./gradlew lint
```

### Installer APK
```bash
./gradlew installDebug
```

---

## 📱 Fichiers de Configuration Android

### AndroidManifest.xml
```
- Activités déclarées
- Permissions requises
- Services
- Broacast receivers
```

### build.gradle.kts
```
- Dépendances
- Versions SDK
- Plugins
- Signing config
```

### gradle.properties
```
- Versions
- Properties globales
```

---

## 💾 Structure des Données Firebase

### Firestore Collections
```
conversations/
├── {conversationId}
│   ├── users: [user1, user2]
│   ├── lastMessage
│   ├── lastMessageTime
│   └── messages/
│       └── {messageId}
│           ├── text
│           ├── senderId
│           ├── timestamp
│           ├── reactions: [{emoji, userId, timestamp}]
│           ├── replyTo: {messageId, senderId, text}
│           └── ... (autres propriétés)

users/
├── {userId}
│   ├── name
│   ├── email
│   ├── photoUrl
│   ├── isOnline
│   └── lastSeen
```

---

## 🔄 Flux de Données

```
ChatActivity
    ↓
MessageAdapter ← observeMessages() ← ChatRepository
    ↓                                       ↓
Affichage               Firebase Firestore
    ↑                                       ↑
    └─ sendMessage() / updateReaction() ──┘
```

---

## 📚 Documentation par Rôle

### Pour Développeurs
1. Lire `IMPLEMENTATION_SUMMARY.md`
2. Étudier la structure ici
3. Vérifier les dépendances
4. Consulter `CHAT_FEATURES.md`
5. Examiner le code source

### Pour QA
1. Lire `GUIDE_UTILISATEUR.md`
2. Consulter `CHAT_FEATURES.md`
3. Vérifier la matrice de test
4. Rapporter les bugs avec contexte

### Pour PM
1. Lire `CHAT_MODERNIZATION.md`
2. Consulter `NOUVELLES_FONCTIONNALITES.md`
3. Voir l'impact utilisateur
4. Préparer le marketing

---

## 🎯 Points de Vigilance

1. **Firebase Rules**: Vérifier les permissions Firestore
2. **Permissions Android**: RECORD_AUDIO pour vocal
3. **Migrations**: Vérifier la compatibilité Room
4. **Performance**: Tester sur appareils bas de gamme
5. **Batterie**: Impact des animations
6. **Mémoire**: Gestion des images et audio

---

## 🚀 Conclusion

La structure du projet NexTalk est **bien organisée et modulaire**, ce qui facilite:
- ✅ Maintenance
- ✅ Évolution
- ✅ Testing
- ✅ Collaboration

**Bonne navigation ! 🎉**

---

**Dernière mise à jour: Décembre 2025**
