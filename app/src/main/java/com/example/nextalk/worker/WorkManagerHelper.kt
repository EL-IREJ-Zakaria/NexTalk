package com.example.nextalk.worker

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit

private val OneTimeWorkRequest.Companion.MIN_BACKOFF_MILLIS: Long
    get() {
        return WorkRequest.MIN_BACKOFF_MILLIS
    }

object WorkManagerHelper {

    fun scheduleSendPendingMessages(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val sendMessagesRequest = OneTimeWorkRequestBuilder<SendMessageWorker>()
            .setConstraints(constraints)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                OneTimeWorkRequest.MIN_BACKOFF_MILLIS,
                TimeUnit.MILLISECONDS
            )
            .build()

        WorkManager.getInstance(context)
            .enqueueUniqueWork(
                SendMessageWorker.WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                sendMessagesRequest
            )
    }

    fun schedulePeriodicSync(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()

        val periodicRequest = PeriodicWorkRequestBuilder<SendMessageWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(context)
            .enqueueUniquePeriodicWork(
                "periodic_sync",
                ExistingPeriodicWorkPolicy.KEEP,
                periodicRequest
            )
    }
}
