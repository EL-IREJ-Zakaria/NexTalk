# 🔧 Guide de Dépannage - Messagerie NexTalk

## ⚡ Solution Rapide (90% des cas)

### Le problème le plus courant : Règles Firestore

**SYMPTÔME** : Les messages ne s'envoient pas ou ne s'affichent pas chez l'autre utilisateur.

**CAUSE** : Les règles de sécurité Firestore bloquent l'accès.

**SOLUTION EN 5 MINUTES** :

1. **Ouvrez Firebase Console** : https://console.firebase.google.com
2. **Sélectionnez votre projet** "NexTalk"
3. **Allez dans** : Firestore Database → Règles (onglet)
4. **Remplacez les règles par ceci** (pour le développement) :

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

5. **Cliquez sur "Publier"** (bouton bleu en haut)
6. **Testez à nouveau** l'envoi de messages

**✅ Vérification** : Si ça fonctionne maintenant, le problème venait bien des règles !

---

## 🔍 Diagnostic Étape par Étape

### Étape 1 : Vérifier l'authentification

**Test** : Les deux utilisateurs sont-ils connectés ?

```
1. Ouvrez l'application sur les deux appareils
2. Vérifiez que chaque utilisateur voit son profil
3. Vérifiez qu'ils ont une connexion Internet
```

**Dans Logcat** :
```
Cherchez : "Firebase Auth : Connecté"
Si vous voyez "Aucun utilisateur connecté" → Connectez-vous d'abord
```

### Étape 2 : Vérifier Firestore

**Test intégré** :
```
1. Ouvrez une conversation dans l'app
2. Appuyez sur ⋮ (menu) en haut à droite
3. Sélectionnez "Test de connexion"
4. Lisez le résultat
```

**Dans Logcat** :
```
Filtre : "FirebaseTest"

✅ Bon signe :
   "✅ Firestore : Écriture réussie"
   "✅ Firestore : Lecture réussie"

❌ Problème :
   "❌ ERREUR CRITIQUE : Permission refusée"
   → Solution : Configurez les règles (voir ci-dessus)
```

### Étape 3 : Vérifier l'envoi de messages

**Test** :
```
1. Utilisateur A envoie un message "Test 123"
2. Regardez Logcat de l'utilisateur A
```

**Dans Logcat (filtre: "ChatActivity")** :
```
✅ Succès :
   "📤 Envoi de message..."
   "✅ Connexion Internet disponible"
   "✅ Message envoyé avec succès !"
   "Le message devrait apparaître chez l'autre utilisateur instantanément"

❌ Échec :
   "❌ Échec de l'envoi du message"
   "❌ ERREUR CRITIQUE : Permission refusée"
   → Solution : Règles Firestore (voir Solution Rapide ci-dessus)
```

### Étape 4 : Vérifier la réception

**Test** :
```
1. Utilisateur B devrait voir le message apparaître
2. Regardez Logcat de l'utilisateur B
```

**Dans Logcat (utilisateur B)** :
```
✅ Succès :
   "👂 Démarrage de l'écoute des messages..."
   "📨 Messages reçus : X messages"
   "✨ Nouveau(x) message(s) : 1"
   "Dernier message : Test 123"

❌ Problème :
   "❌ Erreur lors de l'écoute des messages"
   "❌ ERREUR : Permission refusée"
   → Solution : Règles Firestore
```

### Étape 5 : Vérifier dans Firebase Console

**Navigation** :
```
console.firebase.google.com
→ Votre projet
→ Firestore Database
→ conversations (collection)
→ [conversationId] (document)
→ messages (sous-collection)
```

**Vérification** :
- ✅ Les messages apparaissent-ils dans la console ?
  - OUI → Le problème vient de la réception côté app
  - NON → Le problème vient de l'envoi

---

## 🐛 Problèmes Spécifiques

### Problème 1 : "Permission Denied"

**Erreur dans Logcat** :
```
com.google.firebase.firestore.FirebaseFirestoreException: 
PERMISSION_DENIED: Missing or insufficient permissions.
```

**Solution** :
1. Ouvrez Firebase Console
2. Firestore Database → Règles
3. Pour le développement, utilisez :
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
4. Publiez les règles
5. Attendez 1-2 minutes
6. Retestez

### Problème 2 : Pas de connexion Internet

**Symptôme** :
```
Toast : "Pas de connexion Internet"
Message dans Logcat : "⚠️ Pas de connexion Internet - Mode hors ligne"
```

**Solution** :
1. Vérifiez le WiFi/données mobiles
2. Les messages seront envoyés automatiquement quand la connexion reviendra
3. Vérifiez les permissions réseau dans AndroidManifest.xml

### Problème 3 : Les messages ne s'affichent pas

**Si les messages sont dans Firebase mais pas dans l'app** :

**Cause possible** : Problème de listener

**Solution** :
1. Fermez complètement l'app (force stop)
2. Rouvrez l'app
3. Ouvrez à nouveau la conversation
4. Vérifiez Logcat pour "👂 Démarrage de l'écoute des messages..."

### Problème 4 : ConversationId incorrect

**Symptôme** :
```
Les deux utilisateurs ne voient pas les mêmes messages
```

**Vérification dans Logcat** :
```
Utilisateur A : "ConversationId: abc123"
Utilisateur B : "ConversationId: xyz789"
```

**Si différents** → Problème !

**Solution** :
1. Les deux utilisateurs doivent ouvrir la conversation depuis la liste
2. Ne pas créer de nouvelle conversation manuellement

### Problème 5 : Firestore non initialisé

**Erreur** :
```
FirebaseException: Firestore database not found
```

**Solution** :
1. Allez dans Firebase Console
2. Firestore Database
3. Cliquez sur "Créer une base de données"
4. Choisissez "Mode test" (développement) ou "Mode production"
5. Choisissez la région (ex: europe-west1)
6. Créez la base

---

## 📋 Checklist Complète

Avant de demander de l'aide, vérifiez :

- [ ] Firebase est bien configuré (google-services.json présent)
- [ ] Les deux utilisateurs sont authentifiés
- [ ] Les deux utilisateurs ont une connexion Internet
- [ ] Les règles Firestore permettent l'accès
- [ ] La base Firestore existe dans Firebase Console
- [ ] Les deux utilisateurs ouvrent la même conversation (même conversationId)
- [ ] J'ai testé avec le menu "Test de connexion"
- [ ] J'ai consulté les logs Logcat avec les filtres appropriés

---

## 🔧 Commandes Utiles

### Voir les logs en temps réel

**Windows PowerShell** :
```powershell
adb logcat | Select-String "ChatActivity|ChatRepository|FirebaseTest"
```

**Windows CMD** :
```cmd
adb logcat | findstr "ChatActivity ChatRepository FirebaseTest"
```

**Linux/Mac** :
```bash
adb logcat | grep -E "ChatActivity|ChatRepository|FirebaseTest"
```

### Nettoyer et reconstruire

```bash
# Nettoyer
./gradlew clean

# Reconstruire
./gradlew assembleDebug

# Installer
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Forcer l'arrêt de l'app

```bash
adb shell am force-stop com.example.nextalk
```

### Voir les erreurs uniquement

```bash
adb logcat *:E
```

---

## 📱 Test Complet (Deux Appareils)

### Scénario de test :

1. **Appareil A** :
   - Connectez-vous avec utilisateur_a@example.com
   - Allez dans la liste des conversations
   - Ouvrez la conversation avec utilisateur_b

2. **Appareil B** :
   - Connectez-vous avec utilisateur_b@example.com
   - Allez dans la liste des conversations
   - Ouvrez la conversation avec utilisateur_a

3. **Test d'envoi** :
   - Appareil A : Envoyez "Bonjour depuis A"
   - Appareil B : Devrait voir le message apparaître INSTANTANÉMENT
   - Appareil B : Répond "Bonjour depuis B"
   - Appareil A : Devrait voir la réponse INSTANTANÉMENT

4. **Vérification Logcat** :
   - Sur les deux appareils, vérifiez les logs
   - Cherchez "✅ Message envoyé avec succès"
   - Cherchez "✨ Nouveau(x) message(s)"

---

## 🆘 Toujours bloqué ?

Si après avoir suivi ce guide le problème persiste :

### Partagez ces informations :

1. **Logs Logcat** :
```bash
adb logcat -d > logs.txt
# Envoyez le fichier logs.txt
```

2. **Version Android** des deux appareils

3. **Captures d'écran** :
   - Firebase Console → Firestore → Structure des données
   - Firebase Console → Firestore → Règles
   - Logcat avec les erreurs

4. **Résultat du test de connexion** (menu dans l'app)

5. **Les messages apparaissent-ils dans Firebase Console ?**
   - OUI → Problème de réception
   - NON → Problème d'envoi

---

## 🎯 Points Importants

1. **Firebase est en temps réel** : Si configuré correctement, les messages apparaissent INSTANTANÉMENT (< 1 seconde)

2. **Deux types de problèmes** :
   - Configuration (règles Firestore) → Solution rapide
   - Code (bugs) → Logs nécessaires

3. **90% des problèmes** viennent des règles Firestore

4. **Le code est déjà correct** : Votre application a tout le code nécessaire pour la messagerie en temps réel

5. **Test intégré** : Utilisez le menu "Test de connexion" dans l'app

---

**Bon courage ! 🚀**

La messagerie devrait fonctionner après avoir configuré les règles Firestore.
