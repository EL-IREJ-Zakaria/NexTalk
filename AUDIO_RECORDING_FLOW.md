# 🎙️ Flux d'Enregistrement Audio - Diagramme d'États

## État de l'Interface

```
┌─────────────────────────────────────────────────────────────┐
│                     ÉTAT INITIAL                             │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  [🎤] Bouton Micro visible                           │   │
│  │  [ ] Indicateur d'enregistrement masqué              │   │
│  │  [ ] Texte "Glisser pour annuler" masqué            │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                            │
                            │ ACTION_DOWN (appui long)
                            ▼
┌─────────────────────────────────────────────────────────────┐
│              ENREGISTREMENT EN COURS                         │
│  ┌─────────────────────────────────────────────────────┐   │
│  │  [🎤] Bouton Micro (scale: 0.9x, animé)             │   │
│  │  [📍] Vibration haptique (50ms)                      │   │
│  │                                                       │   │
│  │  ┌───────────────────────────────────────────┐      │   │
│  │  │  🔴 ENREGISTREMENT                         │      │   │
│  │  │  🎤  ▂▅▃▇▅▆▃  0:05                        │      │   │
│  │  │  [Animation: pulsation + ondes]           │      │   │
│  │  └───────────────────────────────────────────┘      │   │
│  │                                                       │   │
│  │  ⬆️ Glisser pour annuler (alpha: 0.8)              │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
                    │                     │
         ACTION_MOVE │                     │ ACTION_UP
         (swipe up)  │                     │ (release)
                     │                     │
         ▼           │                     ▼
┌─────────────┐     │         ┌──────────────────────┐
│ ANNULATION  │◄────┘         │   ENVOI EN COURS     │
└─────────────┘               └──────────────────────┘
      │                                   │
      │                                   │
      ▼                                   ▼
┌─────────────────────┐     ┌──────────────────────────┐
│  Animation sortie    │     │  Animation succès        │
│  Slide up (-100px)   │     │  Scale 1.2x → 1.0x      │
│  Vibration (100ms)   │     │  Upload vers Firebase    │
│  Suppression fichier │     │  Création du message     │
│  Toast "Annulé"      │     │  Toast "Envoyé"          │
└─────────────────────┘     └──────────────────────────┘
      │                                   │
      └───────────┬───────────────────────┘
                  │
                  ▼
        ┌──────────────────┐
        │  RETOUR INITIAL   │
        └──────────────────┘
```

## Détail des Animations

### 🎬 Animation d'Entrée (300ms)
```
recordingIndicatorCard:
  Alpha: 0 → 1
  TranslationY: +100px → 0px
  
tvSlideToCancel:
  Alpha: 0 → 0.8
  Delay: 150ms

btnVoice:
  ScaleX/Y: 1.0 → 0.9
  Duration: 200ms

ivRecordingIcon:
  Alpha: 1.0 → 0.3 → 1.0 (loop)
  Duration: 1000ms per cycle
```

### 🎵 Animation des Ondes (150ms loop)
```
Pour chaque barre (7 barres):
  ScaleY: random(1.0 - 2.0)
  Duration: 150ms
  Repeat: infinite
```

### ⏱️ Timer d'Enregistrement
```
Update: chaque 100ms
Format: M:SS
  Exemple: 0:05, 1:23, 10:45
```

### 🚫 Animation d'Annulation (200ms)
```
recordingIndicatorCard:
  Alpha: current → 0
  TranslationY: 0px → -100px
  
tvSlideToCancel:
  Alpha: current → 0
  
btnVoice:
  ScaleX/Y: 0.9 → 1.0
```

### ✅ Animation de Succès (100ms × 2)
```
btnVoice:
  Phase 1: ScaleX/Y: 0.9 → 1.2 (100ms)
  Phase 2: ScaleX/Y: 1.2 → 1.0 (100ms)
```

## Gestion des Gestes

### 📍 Détection du Swipe Up
```kotlin
ACTION_MOVE:
  deltaY = initialTouchY - event.rawY
  
  if (deltaY > 100px):
    → cancelVoiceRecording()
  
  else if (deltaY > 0):
    → Effet visuel progressif
    → recordingIndicatorCard.alpha = 1 - (deltaY/100) * 0.5
    → tvSlideToCancel.alpha = 0.8 + (deltaY/100) * 0.2
```

## Timeline d'un Enregistrement Complet

```
T=0ms     : ACTION_DOWN
          └─> vibrateDevice(50ms)
          └─> showRecordingIndicator()
          
T=0-300ms : Animation d'entrée
          └─> Card fade in + slide up
          
T=150ms   : Texte "Glisser pour annuler" apparaît
          
T=300ms+  : État stable - enregistrement
          ├─> Pulsation icône (loop 1000ms)
          ├─> Ondes animées (loop 150ms)
          └─> Timer update (loop 100ms)
          
T=3500ms  : USER ACTION_MOVE (swipe up 120px)
          └─> deltaY > 100px → Trigger cancel
          
T=3500ms  : cancelVoiceRecording()
          └─> vibrateDevice(100ms)
          └─> Animation sortie (-100px)
          └─> stopRecording()
          └─> delete file
          
T=3700ms  : Retour état initial
```

## Conditions et Validations

### ✅ Envoi Réussi
```
Conditions:
  ✓ duration > 500ms
  ✓ file exists
  ✓ file.length > 0
  ✓ !isCanceled

Action:
  → Upload vers Firebase Storage
  → Créer message dans Firestore
  → Supprimer fichier local
  → Toast "Message vocal envoyé"
```

### ❌ Enregistrement Trop Court
```
Conditions:
  ✗ duration ≤ 500ms

Action:
  → Supprimer fichier
  → Toast "Message trop court (min 0.5s)"
```

### 🚫 Annulation
```
Conditions:
  ✓ deltaY > 100px (swipe up)
  OR
  ✓ User navigates away

Action:
  → stopRecording()
  → delete file
  → Toast "Enregistrement annulé"
```

## Variables d'État

```kotlin
// État global
private var isRecording: Boolean = false
private var isCanceled: Boolean = false
private var recordingStartTime: Long = 0

// Gestes
private var initialTouchY: Float = 0f

// Jobs asynchrones
private var recordingJob: Job? = null
private var waveAnimationJob: Job? = null
```

## Permissions Requises

```xml
<!-- AndroidManifest.xml -->
<uses-permission android:name="android.permission.RECORD_AUDIO" />
<uses-permission android:name="android.permission.VIBRATE" />
<uses-permission android:name="android.permission.INTERNET" />

<!-- Pour Android 6+ : Runtime permission -->
RECORD_AUDIO : Demandée au premier ACTION_DOWN
```

---

**Note**: Ce flux assure une expérience utilisateur fluide avec des animations cohérentes et un feedback constant.
