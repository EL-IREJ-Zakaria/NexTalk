# 🎙️ Guide Visuel - Enregistrement Audio NexTalk

## 📱 Interface Utilisateur

### État 1️⃣ : Interface Normale
```
┌───────────────────────────────────────┐
│  👤 Contact Name         📞 📹        │
│  ────────────────────────────────     │
│                                        │
│  ┌──────────────────────────────┐    │
│  │ 👤 Bonjour! Comment vas-tu?  │    │
│  │    14:32 ✓✓                   │    │
│  └──────────────────────────────┘    │
│                                        │
│         ┌──────────────────────┐      │
│         │ Très bien merci!     │ 👤   │
│         │           ✓✓ 14:35   │      │
│         └──────────────────────┘      │
│                                        │
│                                        │
│  ┌────────────────────────────────┐  │
│  │ 📎   [Écrire un message...] 😊 │  │
│  └────────────────────────────────┘  │
│                         [🎤]          │
└───────────────────────────────────────┘
        ↑
    Bouton Micro
```

---

### État 2️⃣ : Enregistrement Actif
```
┌───────────────────────────────────────┐
│  👤 Contact Name         📞 📹        │
│  ────────────────────────────────     │
│                                        │
│  ┌──────────────────────────────┐    │
│  │ 👤 Bonjour! Comment vas-tu?  │    │
│  │    14:32 ✓✓                   │    │
│  └──────────────────────────────┘    │
│                                        │
│         ┌──────────────────────┐      │
│         │ Très bien merci!     │ 👤   │
│         │           ✓✓ 14:35   │      │
│         └──────────────────────┘      │
│                                        │
│      ⬆️ Glisser pour annuler          │
│                                        │
│  ┌────────────────────────────────┐  │
│  │ 🔴 ENREGISTREMENT              │  │
│  │ 🎤  ▂▅▃▇▅▆▃  0:05             │  │
│  └────────────────────────────────┘  │
│                                        │
│  ┌────────────────────────────────┐  │
│  │ 📎   [Écrire un message...] 😊 │  │
│  └────────────────────────────────┘  │
│                      [🎤]             │
│                    (plus petit)       │
└───────────────────────────────────────┘
```

---

### État 3️⃣ : Annulation en Cours (Swipe Up)
```
┌───────────────────────────────────────┐
│  👤 Contact Name         📞 📹        │
│  ────────────────────────────────     │
│                                        │
│  ┌──────────────────────────────┐    │
│  │ 👤 Bonjour! Comment vas-tu?  │    │
│  │    14:32 ✓✓                   │    │
│  └──────────────────────────────┘    │
│                                        │
│      ⬆️ Glisser pour annuler          │
│        (Plus visible)                 │
│  ┌────────────────────────────────┐  │
│  │ 🔴 ENREGISTREMENT              │  │
│  │ 🎤  ▂▅▃▇▅▆▃  0:03             │  │
│  └────────────────────────────────┘  │
│         (translucide, monte)          │
│                                        │
│         ┌──────────────────────┐      │
│         │ Très bien merci!     │ 👤   │
│         │           ✓✓ 14:35   │      │
│         └──────────────────────┘      │
│                                        │
│  ┌────────────────────────────────┐  │
│  │ 📎   [Écrire un message...] 😊 │  │
│  └────────────────────────────────┘  │
│                      [🎤]             │
└───────────────────────────────────────┘
            👆 Swipe UP
```

---

## 🎬 Séquence d'Animation

### Animation d'Entrée (300ms)
```
Frame 0ms    Frame 100ms   Frame 200ms   Frame 300ms
═════════    ═══════════   ═══════════   ═══════════

                            ⬆️ Glisser
                               (fade in)
                                          ⬆️ Glisser
                                          (alpha 0.8)

             ┌─────────┐   ┌─────────┐   ┌─────────┐
             │ 🔴 REC  │   │ 🔴 REC  │   │ 🔴 REC  │
             │ 🎤 ▂▃   │   │ 🎤 ▂▃▅  │   │ 🎤 ▂▅▃▇ │
             │   0:00  │   │   0:00  │   │   0:00  │
             └─────────┘   └─────────┘   └─────────┘
             (alpha 0.3)   (alpha 0.6)   (alpha 1.0)
             (y: +60px)    (y: +30px)    (y: 0px)
```

### Animation des Ondes (Boucle Continue)
```
T=0ms        T=150ms      T=300ms      T=450ms
═════        ═══════      ═══════      ═══════

 ▂▅▃▇▅▆▃  →  ▃▂▆▃▇▅▄  →  ▅▄▃▆▄▇▂  →  ▂▅▃▇▅▆▃
 
(Hauteurs aléatoires, animation fluide)
```

### Pulsation de l'Icône (1000ms)
```
T=0ms   T=250ms  T=500ms  T=750ms  T=1000ms
═════   ═══════  ═══════  ═══════  ════════

 🎤  →   🎤   →   🎤   →   🎤   →   🎤
100%     70%      40%      70%     100%
(alpha) (alpha)  (alpha)  (alpha)  (alpha)
```

---

## 🎨 Spécifications de Design

### Carte d'Enregistrement
```
┌─────────────────────────────────────┐
│  Background: #F44336 (Rouge)        │
│  Corner Radius: 16dp                │
│  Elevation: 8dp                     │
│  Padding: 16dp                      │
│                                     │
│  [🎤 40x40dp] [▂▅▃▇▅▆▃] [0:05]    │
│   White        White     White      │
│   Pulsante     Animé     Bold 16sp  │
└─────────────────────────────────────┘
```

### Barres d'Onde
```
Barre 1: Width 4dp, Base Height 12dp, Radius 2dp
Barre 2: Width 4dp, Base Height 20dp, Radius 2dp
Barre 3: Width 4dp, Base Height 16dp, Radius 2dp
Barre 4: Width 4dp, Base Height 24dp, Radius 2dp
Barre 5: Width 4dp, Base Height 18dp, Radius 2dp
Barre 6: Width 4dp, Base Height 22dp, Radius 2dp
Barre 7: Width 4dp, Base Height 14dp, Radius 2dp

Spacing: 2dp entre chaque barre
Color: #FFFFFF (Blanc)
```

### Texte "Glisser pour annuler"
```
Text: "⬆️ Glisser pour annuler"
Size: 13sp
Color: @color/textSecondary
Alpha: 0.8
Position: Au-dessus de la carte, centré
Margin Bottom: 8dp
```

---

## 🎯 Zones de Touch

### Bouton Micro (ACTION_DOWN)
```
┌────────────────┐
│                │
│       🎤       │  ← Appui long pour démarrer
│                │
└────────────────┘
   56dp x 56dp
   (Touch target)
```

### Swipe Up pour Annuler
```
        ⬆️
        │
        │  > 100px = Annulation
        │
        │
  ┌─────┴─────┐
  │     🎤    │  ← Position initiale
  └───────────┘
```

---

## 🔔 Feedback Utilisateur

### Vibrations
```
Événement          Durée    Pattern
─────────────────────────────────────
Début enreg.       50ms     ●
Annulation        100ms     ●●
```

### Toast Messages
```
Situation                Message
────────────────────────────────────────────────
Enreg. trop court       "Message trop court (min 0.5s)"
Annulation              "Enregistrement annulé"
Envoi réussi            "Message vocal envoyé"
Permission refusée      "Permission requise pour enregistrer"
Erreur                  "Erreur lors de l'enregistrement"
```

---

## 🎮 Interactions Utilisateur

### Scénario 1 : Enregistrement Réussi
```
1. 👆 Appui long sur 🎤
   └─> 📳 Vibration (50ms)
   └─> 🎬 Animation d'entrée
   └─> ⏱️  Timer démarre (0:00)
   
2. 🗣️  Parler (durée > 0.5s)
   └─> 🌊 Ondes animées
   └─> 💫 Icône pulsante
   └─> ⏱️  Timer progresse (0:05)
   
3. 👆 Relâcher le bouton
   └─> 🎬 Animation de succès
   └─> 📤 Upload vers Firebase
   └─> ✅ "Message vocal envoyé"
   └─> 🎬 Animation de sortie
```

### Scénario 2 : Annulation
```
1. 👆 Appui long sur 🎤
   └─> 📳 Vibration (50ms)
   └─> 🎬 Animation d'entrée
   
2. 🗣️  Parler...
   
3. ⬆️ Glisser vers le haut > 100px
   └─> 📳 Vibration (100ms)
   └─> 🎬 Animation slide up
   └─> 🗑️  Suppression fichier
   └─> ⚠️  "Enregistrement annulé"
```

### Scénario 3 : Trop Court
```
1. 👆 Appui long sur 🎤
   
2. 👆 Relâcher immédiatement (< 0.5s)
   └─> 🎬 Animation de sortie rapide
   └─> 🗑️  Suppression fichier
   └─> ⚠️  "Message trop court (min 0.5s)"
```

---

## 📊 Métriques de Performance

### Temps de Réponse
```
Action                  Temps Cible    Temps Réel
──────────────────────────────────────────────────
Affichage indicateur    < 50ms         ~30ms
Animation entrée        300ms          300ms
Mise à jour timer       100ms          100ms
Animation ondes         150ms          150ms
Détection swipe up      < 100ms        ~50ms
Vibration               50-100ms       50-100ms
```

### Utilisation Ressources
```
Ressource          Utilisation    Acceptable
─────────────────────────────────────────────
CPU (animations)   5-10%          < 15%
RAM                +2MB           < 5MB
Battery            Négligeable    < 1%/min
```

---

## ✅ Checklist de Test Visuel

### Animations
- [ ] Animation d'entrée fluide (fade + slide)
- [ ] Ondes animées en continu
- [ ] Pulsation de l'icône visible
- [ ] Timer mis à jour régulièrement
- [ ] Animation de sortie fluide
- [ ] Swipe up smooth avec effet progressif

### Couleurs & Contraste
- [ ] Carte rouge bien visible sur fond clair
- [ ] Texte blanc lisible sur fond rouge
- [ ] Indicateur "Glisser" visible mais discret
- [ ] Icônes bien contrastées

### Responsive
- [ ] Adapté aux petits écrans (< 5")
- [ ] Adapté aux grands écrans (> 6")
- [ ] Rotation d'écran gérée
- [ ] Pas de débordement de texte

---

## 🎨 Thème Sombre (À Implémenter)

### Proposition de Design
```
┌─────────────────────────────────────┐
│  Background: #D32F2F (Rouge foncé)  │
│  Text: #FFFFFF (Blanc pur)          │
│  Waves: #FFFFFF 90% opacity         │
│  Indicator: #BBBBBB 80% opacity     │
└─────────────────────────────────────┘
```

---

## 🏆 Points Forts du Design

### ✨ Modernité
- Design Material Design 3
- Animations fluides et naturelles
- Micro-interactions soignées

### 👆 Intuitivité
- Gestes standards (hold, swipe)
- Feedback immédiat et clair
- Indicateurs explicites

### 🎯 Efficacité
- Une seule action pour enregistrer
- Annulation rapide et facile
- Pas de dialogues intrusifs

### 💎 Polissage
- Vibrations haptiques
- Animations coordonnées
- Timer précis et lisible

---

**Design créé avec soin pour NexTalk 🚀**
