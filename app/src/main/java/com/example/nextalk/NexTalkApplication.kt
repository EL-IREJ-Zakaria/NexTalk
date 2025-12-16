package com.example.nextalk

import android.app.Application
import com.example.nextalk.data.local.NexTalkDatabase

class NexTalkApplication : Application() {

    val database: NexTalkDatabase by lazy {
        NexTalkDatabase.getDatabase(this)
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: NexTalkApplication
            private set
    }
}
