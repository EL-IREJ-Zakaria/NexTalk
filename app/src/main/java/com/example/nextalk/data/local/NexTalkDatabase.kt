package com.example.nextalk.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.example.nextalk.data.local.dao.CallDao
import com.example.nextalk.data.local.dao.ConversationDao
import com.example.nextalk.data.local.dao.MessageDao
import com.example.nextalk.data.local.dao.StatusDao
import com.example.nextalk.data.local.dao.UserDao
import com.example.nextalk.data.model.Call
import com.example.nextalk.data.model.Conversation
import com.example.nextalk.data.model.Message
import com.example.nextalk.data.model.Status
import com.example.nextalk.data.model.User

@Database(
    entities = [User::class, Conversation::class, Message::class, Call::class, Status::class],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class NexTalkDatabase : RoomDatabase() {

    abstract fun userDao(): UserDao
    abstract fun conversationDao(): ConversationDao
    abstract fun messageDao(): MessageDao
    abstract fun callDao(): CallDao
    abstract fun statusDao(): StatusDao

    companion object {
        @Volatile
        private var INSTANCE: NexTalkDatabase? = null

        fun getDatabase(context: Context): NexTalkDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    NexTalkDatabase::class.java,
                    "nextalk_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
