# NexTalk - Application de Messagerie Android

Application mobile Android de messagerie instantanée développée en Kotlin, inspirée de WhatsApp.

## Fonctionnalités

### 1. Authentification

- Inscription avec email et mot de passe
- Connexion
- Mot de passe oublié
- Vérification de l'âge (≥ 18 ans)
- Photo de profil

### 2. Gestion des contacts

- Liste de tous les utilisateurs inscrits
- Recherche d'utilisateurs
- Lancement d'une conversation en un clic

### 3. Messagerie en temps réel

- Envoi/réception de messages texte
- Support des emojis
- Envoi de photos
- Mise à jour en temps réel via Firestore
- Interface avec bulles de conversation (gauche/droite)
- Horodatage des messages
- Statuts: envoyé, reçu, vu
- Indicateur en ligne/hors ligne

### 4. Mode hors-ligne

- Messages stockés localement avec Room
- WorkManager pour synchronisation automatique

### 5. Notifications Push (FCM)

- Notifications pour nouveaux messages
- Ouverture directe de la conversation

### 6. Paramètres utilisateur

- Modifier nom et photo de profil
- Mode sombre
- Activer/désactiver notifications
- Déconnexion

## Technologies utilisées

- **Kotlin** - Langage de programmation
- **Architecture MVVM** - Pattern architectural
- **Firebase Authentication** - Authentification
- **Firebase Firestore** - Base de données temps réel
- **Firebase Storage** - Stockage des médias
- **Firebase Cloud Messaging** - Notifications push
- **Room** - Base de données locale
- **Coroutines & Flow** - Programmation asynchrone
- **ViewModel & LiveData** - Gestion de l'état
- **WorkManager** - Tâches en arrière-plan
- **Glide** - Chargement d'images
- **Material Design 3** - Interface utilisateur

## Configuration Firebase

Pour faire fonctionner l'application, vous devez configurer Firebase :

### 1. Créer un projet Firebase

1. Accédez à [Firebase Console](https://console.firebase.google.com/)
2. Créez un nouveau projet
3. Ajoutez une application Android avec le package `com.example.nextalk`

### 2. Télécharger google-services.json

1. Dans les paramètres du projet Firebase, téléchargez `google-services.json`
2. Placez-le dans le dossier `app/`

### 3. Activer les services Firebase

- **Authentication** : Activer l'authentification par email/mot de passe
- **Firestore Database** : Créer une base de données
- **Storage** : Activer le stockage
- **Cloud Messaging** : Automatiquement activé

### 4. Règles Firestore (développement)

```javascript
rules_version = '2';
service cloud.firestore {
  match /databases/{database}/documents {
    match /{document=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

### 5. Règles Storage (développement)

```javascript
rules_version = '2';
service firebase.storage {
  match /b/{bucket}/o {
    match /{allPaths=**} {
      allow read, write: if request.auth != null;
    }
  }
}
```

## Structure du projet

```
app/src/main/java/com/example/nextalk/
├── data/
│   ├── local/
│   │   ├── dao/           # Data Access Objects
│   │   ├── Converters.kt  # Type converters Room
│   │   └── NexTalkDatabase.kt
│   ├── model/             # Modèles de données
│   │   ├── User.kt
│   │   ├── Conversation.kt
│   │   └── Message.kt
│   ├── preferences/       # DataStore
│   │   └── PreferencesManager.kt
│   └── repository/        # Repositories
│       ├── AuthRepository.kt
│       ├── ChatRepository.kt
│       └── UserRepository.kt
├── service/
│   └── NexTalkFirebaseMessagingService.kt
├── ui/
│   ├── auth/              # Écrans d'authentification
│   ├── chat/              # Écran de conversation
│   ├── main/              # Écran principal
│   ├── onboarding/        # Écran d'onboarding
│   ├── profile/           # Écran profil
│   ├── splash/            # Écran de démarrage
│   └── users/             # Liste des utilisateurs
├── util/
│   └── NetworkUtil.kt
├── worker/
│   ├── SendMessageWorker.kt
│   └── WorkManagerHelper.kt
└── NexTalkApplication.kt
```

## Structure Firestore

```
/users/{userId}
  - uid
  - name
  - email
  - photoUrl
  - isOnline
  - lastSeen
  - fcmToken
  - birthDate
  - createdAt

/conversations/{conversationId}
  - id
  - users: [uid1, uid2]
  - lastMessage
  - lastMessageTime
  - lastMessageSenderId
  - unreadCount

/conversations/{conversationId}/messages/{messageId}
  - id
  - conversationId
  - senderId
  - text
  - imageUrl
  - timestamp
  - status (PENDING/SENT/RECEIVED/SEEN)
  - type (TEXT/IMAGE/EMOJI)
```

## Écrans

1. **SplashScreen** - Logo avec redirection automatique
2. **Onboarding** - 3 pages de présentation
3. **Login** - Connexion
4. **Register** - Inscription
5. **ForgotPassword** - Réinitialisation mot de passe
6. **Home** - Liste des conversations
7. **Users** - Liste des utilisateurs
8. **Chat** - Conversation en temps réel
9. **Profile** - Profil et paramètres

## Compilation

1. Clonez le repository
2. Ajoutez `google-services.json` dans `app/`
3. Synchronisez Gradle
4. Exécutez l'application

## Auteur

Projet développé dans le cadre du Module M205 - Composant Android
Filière : Développement Mobile
Année 2024/2025
