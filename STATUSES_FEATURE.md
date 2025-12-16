# 📱 Statuts - NexTalk

## 🎯 Vue d'Ensemble

La **fonctionnalité Statuts** permet de partager des contenus temporaires (texte, photo, vidéo) qui **disparaissent après 24 heures**, comme WhatsApp ou Instagram.

---

## ✨ Types de Statuts

### 1. **Statuts Texte** 📝
- Texte avec couleur personnalisée
- Fond coloré configurable
- Émoticônes et formatage

**Exemple:**
```
┌─────────────────────┐
│                     │
│   Bonne journée ! 😊 │
│                     │
│   (Fond bleu-vert)  │
└─────────────────────┘
```

### 2. **Statuts Photo** 📷
- Photos haute résolution
- Duplication après 5 secondes
- Marquage des vues

### 3. **Statuts Vidéo** 🎥
- Vidéos courtes (jusqu'à 30s)
- Lecture automatique
- Durée configurée

---

## 🎬 Interface des Statuts

### Écran Principal
```
┌────────────────────────┐
│ ← Statuts             │ ← Toolbar
├────────────────────────┤
│                        │
│ 👤 Jean Dupont         │
│    3 statuts           │ ← Utilisateur + nombre
│    Il y a 2 heures     │
│    Vues: 42 👁️        │
│                        │
│ 👤 Marie Curie         │
│    5 statuts           │
│    Il y a 10 min       │
│    Vues: 156 👁️       │
│                        │
│ 👤 Votre statut        │
│    (Mon statut)        │
│    À expirer dans 18h  │
│                        │
└────────────────────────┘
         [+] ← FAB
```

---

## 🎨 Création de Statut

### Dialog de Création
```
┌────────────────────────┐
│ Créer un statut        │
├────────────────────────┤
│                        │
│ [📝] Texte             │
│ [📷] Photo             │
│ [🎥] Vidéo             │
│                        │
│      [Annuler]         │
└────────────────────────┘
```

### Statut Texte - Éditeur
```
┌────────────────────────┐
│ Écrire un statut       │
├────────────────────────┤
│                        │
│  [Écrivez ici...]      │
│                        │
│  Couleurs:             │
│  🔴 🟠 🟡 🟢 🔵 🟣     │
│                        │
│ [Annuler] [Partager]   │
└────────────────────────┘
```

---

## 👁️ Visualisation d'un Statut

### Écran de Lecture
```
┌────────────────────────┐
│ [Image/Texte du statut]│
│                        │
│ 👤 Jean Dupont         │
│    il y a 2 heures     │
│                        │
│ [← Précédent] [→ Suivant]
│                        │
│ [❤️] [😂] [😮] ...     │ ← Réactions rapides
│                        │
│ [💬] Répondre          │
└────────────────────────┘
```

---

## 📊 Informations par Statut

Chaque statut affiche:
- 👤 **Avatar** de l'utilisateur
- **Nom** du créateur
- **Nombre de statuts** de cet utilisateur
- **Heure** (formatée intelligemment)
- **Nombre de vues** 👁️
- **Temps avant expiration**

---

## ⏱️ Durée de Vie

### Timeline des Statuts
```
Création
   ↓
[0-24h] Visible pour tous
   ↓
24h Expiration
   ↓
Suppression automatique
```

### Affichage des Durées
- "À l'instant"
- "Il y a 5 min"
- "Il y a 1 heure"
- "Hier"
- "Il y a 2 jours"

---

## 👁️ Système de Vues

### Marquage des Vues
```
Utilisateur ouvre statut
        ↓
Statut marqué comme "vu"
        ↓
Compteur incrémenté
        ↓
Créateur voit le nombre
```

### Liste des Vues
```
┌────────────────────────┐
│ Vues: 156              │
├────────────────────────┤
│ 👤 Jean              ✓  │
│ 👤 Marie             ✓  │
│ 👤 Luc               ✓  │
│ 👤 Emma              ✓  │
│ ... +152 autres       │
└────────────────────────┘
```

---

## 💬 Réponses aux Statuts

### Système de Réponses
```
Utilisateur regarde statut
        ↓
Clique "Répondre"
        ↓
Envoie un message privé
        ↓
Créateur reçoit réponse
```

### Types de Réponses
- **Message privé** à l'utilisateur
- **Réaction emoji** directe
- **Mention** dans le chat

---

## 🗂️ Architecture

### Modèle Status
```kotlin
data class Status(
    val id: String,
    val userId: String,
    val userName: String,
    val userPhotoUrl: String,
    val content: String,              // Texte ou URL image/vidéo
    val type: StatusType,             // TEXT, IMAGE, VIDEO
    val backgroundColor: String,      // Pour texte
    val textColor: String,
    val createdAt: Long,
    val expiresAt: Long,              // 24h après création
    val duration: Long,               // Durée d'affichage
    val viewedBy: List<String>,       // Utilisateurs qui ont vu
    val replies: List<StatusReply>    // Réponses reçues
)
```

### Components
```
StatusesActivity
    ↓
    ├─ StatusesAdapter (affichage)
    ├─ StatusViewerActivity (lecture)
    └─ CreateStatusActivity (création)
        ↓
        StatusRepository (données)
```

---

## 🔐 Confidentialité

### Contrôle des Vues
- ✅ Créateur voit qui a vu
- ✅ Statuts visibles 24h
- ✅ Suppression automatique
- ✅ Pas d'archivage

---

## 📱 Cas d'Usage

### Scenario 1: Créer un statut texte
```
1. Cliquer [+] Ajouter statut
2. Sélectionner "Texte"
3. Écrire le texte
4. Choisir les couleurs
5. Cliquer "Partager"
6. Statut visible 24h
```

### Scenario 2: Voir un statut
```
1. Ouvrir Statuts
2. Cliquer sur utilisateur
3. Statut affiche en plein écran
4. Balayer pour suivant/précédent
5. Cliquer réactions ou répondre
```

### Scenario 3: Répondre à un statut
```
1. Regarder le statut
2. Cliquer "Répondre"
3. Envoyer message privé
4. Message arrive au créateur
```

### Scenario 4: Voir les vues
```
1. Cliquer "Vues: 42" sur un statut
2. Voir la liste des personnes qui ont vu
3. Parfois voir les temps d'accès
```

---

## 🚀 Prochaines Améliorations

### Court Terme
- [ ] Filtrage par amis/famille
- [ ] Réactions emoji avancées
- [ ] Partage sur d'autres statuts

### Moyen Terme
- [ ] Archivage des statuts
- [ ] Partage en groupe
- [ ] Autorisations personnalisées

### Long Terme
- [ ] Statuts collaboratifs
- [ ] Analytics avancées
- [ ] Filtres d'effets

---

## 📊 Statistiques

### Données Collectées
```
Statut créé
├─ Type (texte/photo/vidéo)
├─ Heure de création
├─ Heure d'expiration
├─ Nombres de vues
├─ Personnes qui ont vu
├─ Réactions reçues
└─ Réponses reçues
```

---

## 🎯 Notifications

### Types de Notifications
- ✅ Ami a créé un statut
- ✅ Quelqu'un a réagi à votre statut
- ✅ Réponse reçue à votre statut
- ✅ Vous avez regardé le statut de X
- ✅ Votre statut expire bientôt

---

## 💡 Conseils d'Utilisation

1. **Créativité**: Utilisez les couleurs et textes amusants
2. **Fréquence**: Postez régulièrement (pas trop)
3. **Qualité**: Photos et vidéos claires
4. **Interactions**: Répondez aux messages
5. **Temps**: Postez quand les amis sont actifs

---

## ❓ FAQ

**Q: Les statuts sont-ils privés?**
A: Les statuts sont visibles par tous vos contacts, mais vous voyez qui les regarde.

**Q: Puis-je récupérer un statut expiré?**
A: Non, les statuts disparaissent après 24h. Archivez avant!

**Q: Puis-je voir les statuts sans être vu?**
A: Non, le créateur voit toujours qui a vu.

**Q: Combien de statuts puis-je poster?**
A: Illimité, mais pratiquez la modération.

---

## 📞 Support

Pour toute question:
- 📧 support@nextalk.com
- 🐛 Rapportez les bugs
- 💬 Suggestions bienvenues

---

**Les Statuts NexTalk vous permettent de partager votre vie de manière temporaire ! 📱✨**
