package com.example.nextalk.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.example.nextalk.data.local.Converters
import com.google.firebase.firestore.PropertyName

/**
 * Types d'appels supportés
 */
enum class CallType {
    VOICE,  // Appel vocal
    VIDEO   // Appel vidéo
}

/**
 * États d'un appel
 */
enum class CallStatus {
    INCOMING,   // Appel entrant
    OUTGOING,   // Appel sortant
    RINGING,    // En train de sonner
    CONNECTED,  // Connecté/En cours
    ENDED,      // Terminé
    MISSED,     // Manqué
    DECLINED,   // Refusé
    FAILED,     // Erreur
    SCHEDULED   // Appel planifié
}

/**
 * Modèle d'appel
 */
@Entity(tableName = "calls")
@TypeConverters(Converters::class) // Ajouter les TypeConverters si des listes sont utilisées
data class Call(
    @PrimaryKey
    val id: String = "",
    val conversationId: String = "",
    val callerId: String = "",
    val callerName: String = "",
    val callerPhotoUrl: String = "",
    val receiverId: String = "",
    val receiverName: String = "",
    val receiverPhotoUrl: String = "",
    val type: CallType = CallType.VOICE,
    val status: CallStatus = CallStatus.INCOMING,
    val startTime: Long = 0L,
    val endTime: Long = 0L,
    val duration: Long = 0L,  // Durée en secondes
    val timestamp: Long = System.currentTimeMillis(),
    val isVideoAccepted: Boolean = false,  // Si la vidéo a été activée durant l'appel
    val isCallRecorded: Boolean = false,    // Si l'appel a été enregistré

    // Nouvelles propriétés pour les fonctionnalités avancées
    @get:PropertyName("isGroupCall")
    val isGroupCall: Boolean = false,
    val groupParticipants: List<String> = emptyList(),
    val groupParticipantNames: List<String> = emptyList(),
    val groupParticipantPhotos: List<String> = emptyList(),

    val note: String = "",
    val noteAddedAt: Long = 0L,

    val qualityRating: Int = 0, // Note de qualité de l'appel (1-5)
    val qualityFeedback: String = "",
    val ratedAt: Long = 0L,

    @get:PropertyName("isMuted")
    val isMuted: Boolean = false,
    val mutedAt: Long = 0L,

    @get:PropertyName("isCameraOn")
    val isCameraOn: Boolean = false,
    val cameraToggledAt: Long = 0L,

    @get:PropertyName("isSpeakerOn")
    val isSpeakerOn: Boolean = false,
    val speakerToggledAt: Long = 0L,

    val videoUpgradedAt: Long = 0L // Timestamp quand l'appel a été mis à niveau en vidéo
) {
    // Constructeur sans argument pour Firebase et Room
    constructor() : this("")

    /**
     * Retourne la durée formatée (mm:ss)
     */
    fun getFormattedDuration(): String {
        val minutes = duration / 60
        val seconds = duration % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    /**
     * Convertit en map pour Firebase
     */
    fun toMap(): Map<String, Any?> = mapOf(
        "id" to id,
        "conversationId" to conversationId,
        "callerId" to callerId,
        "callerName" to callerName,
        "callerPhotoUrl" to callerPhotoUrl,
        "receiverId" to receiverId,
        "receiverName" to receiverName,
        "receiverPhotoUrl" to receiverPhotoUrl,
        "type" to type.name,
        "status" to status.name,
        "startTime" to startTime,
        "endTime" to endTime,
        "duration" to duration,
        "timestamp" to timestamp,
        "isVideoAccepted" to isVideoAccepted,
        "isCallRecorded" to isCallRecorded,
        "isGroupCall" to isGroupCall,
        "groupParticipants" to groupParticipants,
        "groupParticipantNames" to groupParticipantNames,
        "groupParticipantPhotos" to groupParticipantPhotos,
        "note" to note,
        "noteAddedAt" to noteAddedAt,
        "qualityRating" to qualityRating,
        "qualityFeedback" to qualityFeedback,
        "ratedAt" to ratedAt,
        "isMuted" to isMuted,
        "mutedAt" to mutedAt,
        "isCameraOn" to isCameraOn,
        "cameraToggledAt" to cameraToggledAt,
        "isSpeakerOn" to isSpeakerOn,
        "speakerToggledAt" to speakerToggledAt,
        "videoUpgradedAt" to videoUpgradedAt
    )

    companion object {
        fun fromMap(map: Map<String, Any?>, id: String): Call {
            @Suppress("UNCHECKED_CAST")
            val groupParticipants = map["groupParticipants"] as? List<String> ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val groupParticipantNames = map["groupParticipantNames"] as? List<String> ?: emptyList()
            @Suppress("UNCHECKED_CAST")
            val groupParticipantPhotos = map["groupParticipantPhotos"] as? List<String> ?: emptyList()

            return Call(
                id = id,
                conversationId = map["conversationId"] as? String ?: "",
                callerId = map["callerId"] as? String ?: "",
                callerName = map["callerName"] as? String ?: "",
                callerPhotoUrl = map["callerPhotoUrl"] as? String ?: "",
                receiverId = map["receiverId"] as? String ?: "",
                receiverName = map["receiverName"] as? String ?: "",
                receiverPhotoUrl = map["receiverPhotoUrl"] as? String ?: "",
                type = try {
                    CallType.valueOf(map["type"] as? String ?: "VOICE")
                } catch (e: Exception) {
                    CallType.VOICE
                },
                status = try {
                    CallStatus.valueOf(map["status"] as? String ?: "INCOMING")
                } catch (e: Exception) {
                    CallStatus.INCOMING
                },
                startTime = (map["startTime"] as? Long) ?: 0L,
                endTime = (map["endTime"] as? Long) ?: 0L,
                duration = (map["duration"] as? Long) ?: 0L,
                timestamp = (map["timestamp"] as? Long) ?: System.currentTimeMillis(),
                isVideoAccepted = map["isVideoAccepted"] as? Boolean ?: false,
                isCallRecorded = map["isCallRecorded"] as? Boolean ?: false,

                isGroupCall = map["isGroupCall"] as? Boolean ?: false,
                groupParticipants = groupParticipants,
                groupParticipantNames = groupParticipantNames,
                groupParticipantPhotos = groupParticipantPhotos,

                note = map["note"] as? String ?: "",
                noteAddedAt = (map["noteAddedAt"] as? Long) ?: 0L,

                qualityRating = (map["qualityRating"] as? Long)?.toInt() ?: 0,
                qualityFeedback = map["qualityFeedback"] as? String ?: "",
                ratedAt = (map["ratedAt"] as? Long) ?: 0L,

                isMuted = map["isMuted"] as? Boolean ?: false,
                mutedAt = (map["mutedAt"] as? Long) ?: 0L,

                isCameraOn = map["isCameraOn"] as? Boolean ?: false,
                cameraToggledAt = (map["cameraToggledAt"] as? Long) ?: 0L,

                isSpeakerOn = map["isSpeakerOn"] as? Boolean ?: false,
                speakerToggledAt = (map["speakerToggledAt"] as? Long) ?: 0L,

                videoUpgradedAt = (map["videoUpgradedAt"] as? Long) ?: 0L
            )
        }
    }
}
