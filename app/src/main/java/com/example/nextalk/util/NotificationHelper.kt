package com.example.nextalk.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.Person
import androidx.core.graphics.drawable.IconCompat
import com.example.nextalk.R
import com.example.nextalk.ui.chat.ChatActivity
import java.net.URL

/**
 * Gestionnaire avancé de notifications pour NexTalk
 */
class NotificationHelper(private val context: Context) {

    companion object {
        private const val TAG = "NotificationHelper"
        
        // Canaux de notification
        const val CHANNEL_MESSAGES = "messages_channel"
        const val CHANNEL_CALLS = "calls_channel"
        const val CHANNEL_STATUSES = "statuses_channel"
        const val CHANNEL_GENERAL = "general_channel"
        
        // IDs de notification
        const val NOTIFICATION_MESSAGE = 1001
        const val NOTIFICATION_CALL = 1002
        const val NOTIFICATION_STATUS = 1003
        
        // Groupes de notification
        const val GROUP_MESSAGES = "messages_group"
    }

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    init {
        createNotificationChannels()
    }

    /**
     * Créer les canaux de notification
     */
    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            // Canal pour les messages
            val messagesChannel = NotificationChannel(
                CHANNEL_MESSAGES,
                "Messages",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications pour les nouveaux messages"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // Canal pour les appels
            val callsChannel = NotificationChannel(
                CHANNEL_CALLS,
                "Appels",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications pour les appels entrants"
                enableLights(true)
                enableVibration(true)
                setShowBadge(true)
            }

            // Canal pour les statuts
            val statusesChannel = NotificationChannel(
                CHANNEL_STATUSES,
                "Statuts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications pour les nouveaux statuts"
                setShowBadge(true)
            }

            // Canal général
            val generalChannel = NotificationChannel(
                CHANNEL_GENERAL,
                "Général",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifications générales"
            }

            notificationManager.createNotificationChannels(
                listOf(messagesChannel, callsChannel, statusesChannel, generalChannel)
            )
        }
    }

    /**
     * Afficher une notification de message simple
     */
    fun showMessageNotification(
        conversationId: String,
        senderId: String,
        senderName: String,
        message: String,
        senderPhotoUrl: String? = null
    ) {
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId)
            putExtra(ChatActivity.EXTRA_OTHER_USER_ID, senderId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(senderName)
            .setContentText(message)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setGroup(GROUP_MESSAGES)
            .build()

        notificationManager.notify(conversationId.hashCode(), notification)
    }

    /**
     * Afficher une notification de message avec style MessagingStyle
     */
    fun showMessagingStyleNotification(
        conversationId: String,
        senderId: String,
        senderName: String,
        messages: List<Pair<String, String>>, // Pair(message, timestamp)
        senderPhotoUrl: String? = null
    ) {
        val intent = Intent(context, ChatActivity::class.java).apply {
            putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId)
            putExtra(ChatActivity.EXTRA_OTHER_USER_ID, senderId)
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            conversationId.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Créer la personne
        val person = Person.Builder()
            .setName(senderName)
            .setKey(senderId)
            .apply {
                senderPhotoUrl?.let { url ->
                    try {
                        val bitmap = BitmapFactory.decodeStream(URL(url).openStream())
                        setIcon(IconCompat.createWithBitmap(bitmap))
                    } catch (e: Exception) {
                        // Ignorer si l'image ne peut pas être chargée
                    }
                }
            }
            .build()

        // Style MessagingStyle
        val messagingStyle = NotificationCompat.MessagingStyle(person)
            .setConversationTitle(senderName)

        messages.forEach { (message, _) ->
            messagingStyle.addMessage(
                NotificationCompat.MessagingStyle.Message(
                    message,
                    System.currentTimeMillis(),
                    person
                )
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setStyle(messagingStyle)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
            .setGroup(GROUP_MESSAGES)
            .build()

        notificationManager.notify(conversationId.hashCode(), notification)
    }

    /**
     * Afficher une notification d'appel entrant
     */
    fun showCallNotification(
        callId: String,
        callerName: String,
        callerPhotoUrl: String? = null,
        isVideoCall: Boolean = false
    ) {
        // Actions pour répondre/refuser
        val answerIntent = Intent(context, ChatActivity::class.java).apply {
            action = "ACTION_ANSWER_CALL"
            putExtra("CALL_ID", callId)
        }
        val answerPendingIntent = PendingIntent.getActivity(
            context,
            0,
            answerIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val declineIntent = Intent(context, ChatActivity::class.java).apply {
            action = "ACTION_DECLINE_CALL"
            putExtra("CALL_ID", callId)
        }
        val declinePendingIntent = PendingIntent.getActivity(
            context,
            1,
            declineIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val callType = if (isVideoCall) "Appel vidéo" else "Appel vocal"

        val notification = NotificationCompat.Builder(context, CHANNEL_CALLS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("$callType entrant")
            .setContentText(callerName)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_CALL)
            .setFullScreenIntent(answerPendingIntent, true)
            .addAction(R.drawable.ic_notification, "Répondre", answerPendingIntent)
            .addAction(R.drawable.ic_notification, "Refuser", declinePendingIntent)
            .setAutoCancel(true)
            .setOngoing(true)
            .build()

        notificationManager.notify(NOTIFICATION_CALL, notification)
    }

    /**
     * Afficher une notification de nouveau statut
     */
    fun showStatusNotification(
        userId: String,
        userName: String,
        statusType: String
    ) {
        val notification = NotificationCompat.Builder(context, CHANNEL_STATUSES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("Nouveau statut")
            .setContentText("$userName a publié un nouveau statut $statusType")
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(userId.hashCode(), notification)
    }

    /**
     * Afficher une notification groupée pour plusieurs messages
     */
    fun showGroupedMessagesNotification(messageCount: Int) {
        val summaryNotification = NotificationCompat.Builder(context, CHANNEL_MESSAGES)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("NexTalk")
            .setContentText("$messageCount nouveaux messages")
            .setGroup(GROUP_MESSAGES)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_MESSAGE, summaryNotification)
    }

    /**
     * Annuler une notification spécifique
     */
    fun cancelNotification(notificationId: Int) {
        notificationManager.cancel(notificationId)
    }

    /**
     * Annuler toutes les notifications
     */
    fun cancelAllNotifications() {
        notificationManager.cancelAll()
    }

    /**
     * Vérifier si les notifications sont activées
     */
    fun areNotificationsEnabled(): Boolean {
        return notificationManager.areNotificationsEnabled()
    }

    /**
     * Obtenir le nombre de notifications actives
     */
    fun getActiveNotificationsCount(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            notificationManager.activeNotifications.size
        } else {
            0
        }
    }
}
