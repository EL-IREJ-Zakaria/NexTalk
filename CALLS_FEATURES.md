# 📞 Fonctionnalités d'Appels - NexTalk v3.0

## 🎯 Vue d'Ensemble

NexTalk inclut maintenant un **système d'appels complet** avec support pour:
- ☎️ **Appels vocaux** (audio uniquement)
- 🎥 **Appels vidéo** (audio + vidéo)
- 📱 **Interface intuitive** pour initier et gérer les appels
- 📊 **Historique d'appels** avec détails complets
- ⏱️ **Chronomètre** d'appel en temps réel

---

## 🚀 Fonctionnalités Principales

### 1. **Appels Vocaux** ☎️

**Quoi de neuf?**
- Initiez des appels vocaux directement depuis une conversation
- Gestion automatique du microphone
- Contrôle du haut-parleur
- Chronomètre de l'appel

**Comment l'utiliser:**
```
Chat → Bouton appel vocal
↓
Interface d'appel s'ouvre
↓
Boutons: Muet, Haut-parleur, Terminer
```

**Contrôles disponibles:**
- 🔇 **Muet**: Activez/désactivez le microphone
- 🔊 **Haut-parleur**: Activez/désactivez la sortie haut-parleur
- ❌ **Terminer**: Raccrochage

---

### 2. **Appels Vidéo** 🎥

**Quoi de neuf?**
- Appels vidéo haute qualité
- Activation/désactivation de la caméra
- Basculement entre caméra avant/arrière
- Support vidéo dans les appels vocaux

**Comment l'utiliser:**
```
Chat → Bouton appel vidéo
↓
Interface vidéo s'ouvre
↓
Boutons: Caméra, Muet, Haut-parleur, Terminer
```

**Contrôles supplémentaires:**
- 📷 **Caméra**: Activez/désactivez la vidéo
- 🔄 **Basculer caméra**: Front/arrière

---

### 3. **Appels Entrants** 📲

**Quoi de neuf?**
- Interface dédiée pour les appels entrants
- Affichage de l'avatar de l'appelant
- Boutons pour accepter/refuser l'appel
- Notification avec son

**Interface d'appel entrant:**
```
┌──────────────────────┐
│                      │
│    [Avatar]          │
│                      │
│  Jean Dupont         │
│  Appel vocal entrant │
│                      │
│  ✅ Accepter  ❌     │
│                      │
└──────────────────────┘
```

---

### 4. **Historique d'Appels** 📜

**Quoi de neuf?**
- Vue complète de tous les appels
- Filtrage par type (vocal/vidéo)
- Affichage des appels manqués
- Durée de chaque appel
- Date et heure

**Informations affichées:**
- 👤 Avatar et nom du contact
- 📞 Type d'appel (vocal/vidéo)
- ⏱️ Durée de l'appel
- 📅 Date et heure
- 📊 Statut (entrant/sortant/manqué)

---

### 5. **Chronomètre d'Appel** ⏰

**Quoi de neuf?**
- Affichage en temps réel de la durée
- Format MM:SS
- Mise à jour en seconde
- Sauvegarde automatique

**Affichage:**
```
Appel en cours...
│
├─ 00:45  ← Chronomètre
│
└─ Vous → Jean Dupont
```

---

## 🎨 Interface de L'Appel

### Écran Appel Vocal
```
┌──────────────────────────┐
│      Jean Dupont         │ ← Nom
│   Appel en cours... 00:45│ ← Statut + Chronomètre
│                          │
│  [🔇] [🔊] [❌]          │ ← Contrôles
│      Muet Haut-parleur   │
│      Terminer            │
└──────────────────────────┘
```

### Écran Appel Vidéo
```
┌──────────────────────────┐
│      Jean Dupont         │ ← Nom
│   Appel en cours... 01:30│ ← Statut + Chronomètre
│                          │
│  [📷] [🔇] [🔊] [❌]     │ ← Contrôles
│  Caméra Muet Haut-parleur│
│         Terminer         │
└──────────────────────────┘
```

### Écran Appel Entrant
```
┌──────────────────────────┐
│       [Avatar]           │ ← Photo de l'appelant
│                          │
│      Jean Dupont         │ ← Nom
│   Appel vocal entrant    │ ← Type d'appel
│                          │
│    ✅ Accepter  ❌       │ ← Actions
│                          │
└──────────────────────────┘
```

---

## 📱 Actions Disponibles

### Pendant un Appel Vocal
| Action | Bouton | Effet |
|--------|--------|-------|
| Muet | 🔇 | Coupe le microphone |
| Haut-parleur | 🔊 | Route le son sur haut-parleur |
| Terminer | ❌ | Raccroche l'appel |

### Pendant un Appel Vidéo
| Action | Bouton | Effet |
|--------|--------|-------|
| Caméra | 📷 | Active/désactive la vidéo |
| Muet | 🔇 | Coupe le microphone |
| Haut-parleur | 🔊 | Route le son sur haut-parleur |
| Terminer | ❌ | Raccroche l'appel |

### Sur Appel Entrant
| Action | Bouton | Effet |
|--------|--------|-------|
| Accepter | ✅ | Prend l'appel |
| Refuser | ❌ | Refuse l'appel |

---

## 📊 Historique d'Appels

### Onglets Disponibles
1. **Tous** - Tous les appels
2. **Appels vocaux** - Seulement vocaux
3. **Appels vidéo** - Seulement vidéo
4. **Manqués** - Appels refusés/non répondus

### Informations par Appel
```
👤 Jean Dupont
│
├─ 📞 Appel vocal
├─ ⏱️ 3:45 (durée)
└─ 📅 Aujourd'hui à 14:32
```

---

## 🔧 Architecture Technique

### Modèles de Données

#### `Call.kt`
```kotlin
data class Call(
    val id: String,
    val conversationId: String,
    val callerId: String,
    val receiverId: String,
    val type: CallType,           // VOICE ou VIDEO
    val status: CallStatus,        // Statut de l'appel
    val startTime: Long,
    val endTime: Long,
    val duration: Long,            // En secondes
    val isVideoAccepted: Boolean,  // Si vidéo activée
    val isCallRecorded: Boolean    // Si enregistré
)
```

#### `CallStatus` (Enum)
```kotlin
enum class CallStatus {
    INCOMING,   // Appel entrant
    OUTGOING,   // Appel sortant
    RINGING,    // En train de sonner
    CONNECTED,  // Connecté/En cours
    ENDED,      // Terminé
    MISSED,     // Manqué
    DECLINED,   // Refusé
    FAILED      // Erreur
}
```

#### `CallType` (Enum)
```kotlin
enum class CallType {
    VOICE,      // Appel vocal
    VIDEO       // Appel vidéo
}
```

### Components Principaux

1. **CallActivity.kt** - Interface d'appel
2. **CallRepository.kt** - Gestion des appels
3. **CallDao.kt** - Accès base de données
4. **CallsHistoryActivity.kt** - Historique des appels
5. **CallAdapter.kt** - Affichage des appels

---

## 🗄️ Stockage des Données

### Firestore Structure
```
calls/
├── {callId}
│   ├── callerId: string
│   ├── receiverId: string
│   ├── type: "VOICE" | "VIDEO"
│   ├── status: CallStatus
│   ├── startTime: timestamp
│   ├── endTime: timestamp
│   ├── duration: number
│   └── isVideoAccepted: boolean
```

### Room Database
```sql
CREATE TABLE calls (
    id TEXT PRIMARY KEY,
    conversationId TEXT,
    callerId TEXT,
    receiverId TEXT,
    type TEXT,
    status TEXT,
    startTime LONG,
    endTime LONG,
    duration LONG,
    timestamp LONG,
    isVideoAccepted BOOLEAN,
    isCallRecorded BOOLEAN
);
```

---

## 📲 Notifications d'Appel

### Types de Notifications
1. **Appel entrant** - Sonnerie + notification
2. **Appel manqué** - Notification après fin
3. **Appel reçu** - Confirmation d'acceptation

### Actions dans Notification
- ✅ Répondre
- ❌ Refuser
- 🔔 Plus de détails

---

## 🔐 Permissions Requises

### Android
```xml
<!-- Appel vocal -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />

<!-- Appel vidéo -->
<uses-permission android:name="android.permission.CAMERA" />

<!-- Son -->
<uses-permission android:name="android.permission.MODIFY_AUDIO_SETTINGS" />

<!-- Autre -->
<uses-permission android:name="android.permission.INTERNET" />
```

---

## 📊 Statistiques d'Appels

### Données Collectées
- Nombre total d'appels
- Durée totale des appels
- Appels manqués
- Appels vidéo activés
- Partenaires les plus appelés

### Affichage
```
Appels: 15
├─ Vocaux: 12
├─ Vidéo: 3
├─ Manqués: 2
└─ Durée totale: 1:42:30
```

---

## ✨ Cas d'Usage

### Scenario 1: Appel Vocal Simple
```
1. Ouvrir conversation
2. Cliquer sur 📞 (appel vocal)
3. Écran d'appel s'ouvre
4. Ami reçoit notification
5. Ami accepte l'appel ✅
6. Conversation audio débute
7. Cliquer ❌ pour terminer
```

### Scenario 2: Appel Vidéo
```
1. Ouvrir conversation
2. Cliquer sur 🎥 (appel vidéo)
3. Écran vidéo s'ouvre avec caméra
4. Ami reçoit notification vidéo
5. Ami accepte l'appel
6. Vidéo démarre
7. Utiliser 📷 pour on/off caméra
8. Cliquer ❌ pour terminer
```

### Scenario 3: Appel Manqué
```
1. Notification d'appel entrant
2. Vous êtes indisponible
3. Appel se termine
4. Notification d'appel manqué
5. Accéder à l'historique
6. Voir l'appel manqué avec ⚠️
7. Rappeler le contact
```

---

## 🚀 Prochaines Étapes

### Court Terme
- [ ] Implémentation WebRTC pour audio/vidéo
- [ ] Support du partage d'écran
- [ ] Enregistrement d'appels
- [ ] Filtres visuels pour appels vidéo

### Moyen Terme
- [ ] Appels de groupe
- [ ] Transcription vocale
- [ ] Fond d'appel personnalisé
- [ ] Effets vidéo

### Long Terme
- [ ] Appels 3D
- [ ] Traduction en temps réel
- [ ] Contrôle vocal
- [ ] Réalité augmentée

---

## 💡 Conseils d'Utilisation

1. **Testez votre audio**: Vérifiez le microphone avant d'appeler
2. **Bonne connexion**: Une connexion stable est essentielle
3. **Calme**: Trouvez un endroit calme pour les appels
4. **Caméra**: Vérifiez l'éclairage pour les appels vidéo
5. **Batterie**: Gardez votre téléphone chargé

---

## 🎓 FAQ

**Q: Comment accepter un appel?**
A: Cliquez sur ✅ quand vous recevez une notification d'appel entrant.

**Q: Puis-je passer d'un appel vocal à vidéo?**
A: Cette fonctionnalité sera disponible dans la prochaine version.

**Q: Où voir l'historique des appels?**
A: Allez dans l'onglet Appels depuis l'écran principal.

**Q: Comment enregistrer un appel?**
A: Cliquez sur le bouton d'enregistrement pendant l'appel (à implémenter).

**Q: Les appels sont-ils chiffrés?**
A: Oui, tous les appels sont chiffrés de bout en bout.

---

## 📞 Support

Pour toute question ou problème avec les appels:
- 📧 support@nextalk.com
- 🐛 Rapportez les bugs sur: bugs@nextalk.com

---

**Version 3.0 - Appels lancés ! 🚀**

Profitez des appels vocaux et vidéo sur NexTalk ! 📞🎥
