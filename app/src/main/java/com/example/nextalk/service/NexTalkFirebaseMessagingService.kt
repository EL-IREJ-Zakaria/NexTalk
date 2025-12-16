package com.example.nextalk.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.nextalk.R
import com.example.nextalk.data.preferences.PreferencesManager
import com.example.nextalk.data.repository.AuthRepository
import com.example.nextalk.ui.chat.ChatActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class NexTalkFirebaseMessagingService : FirebaseMessagingService() {

    private val authRepository = AuthRepository()

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // Vérifier si les notifications sont activées
        CoroutineScope(Dispatchers.IO).launch {
            val preferencesManager = PreferencesManager(applicationContext)
            val notificationsEnabled = preferencesManager.notificationsEnabled.first()

            if (notificationsEnabled) {
                remoteMessage.data.let { data ->
                    val title = data["title"] ?: getString(R.string.new_message)
                    val body = data["body"] ?: ""
                    val conversationId = data["conversationId"] ?: ""
                    val senderId = data["senderId"] ?: ""

                    // Ne pas afficher la notification si c'est notre propre message
                    if (senderId != authRepository.getCurrentUserId()) {
                        sendNotification(title, body, conversationId, senderId)
                    }
                }
            }
        }
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)

        // Mettre à jour le token FCM dans Firestore
        CoroutineScope(Dispatchers.IO).launch {
            authRepository.updateFcmToken(token)
        }
    }

    private fun sendNotification(
        title: String,
        body: String,
        conversationId: String,
        senderId: String
    ) {
        val intent = Intent(this, ChatActivity::class.java).apply {
            putExtra(ChatActivity.EXTRA_CONVERSATION_ID, conversationId)
            putExtra(ChatActivity.EXTRA_OTHER_USER_ID, senderId)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            intent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val channelId = getString(R.string.notification_channel_name)
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val notificationBuilder = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setSound(defaultSoundUri)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Créer le canal de notification pour Android O+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = getString(R.string.notification_channel_description)
            }
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(System.currentTimeMillis().toInt(), notificationBuilder.build())
    }
}
