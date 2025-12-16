package com.example.nextalk.data.local

import androidx.room.TypeConverter
import com.example.nextalk.data.model.MessageReaction
import com.example.nextalk.data.model.MessageStatus
import com.example.nextalk.data.model.MessageType
import com.example.nextalk.data.model.ReplyInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Converters {

    private val gson = Gson()

    @TypeConverter
    fun fromStringList(value: List<String>): String {
        return value.joinToString(",")
    }

    @TypeConverter
    fun toStringList(value: String): List<String> {
        return if (value.isEmpty()) emptyList() else value.split(",")
    }

    @TypeConverter
    fun fromMessageStatus(status: MessageStatus): String {
        return status.name
    }

    @TypeConverter
    fun toMessageStatus(value: String): MessageStatus {
        return try {
            MessageStatus.valueOf(value)
        } catch (e: Exception) {
            MessageStatus.SENT
        }
    }

    @TypeConverter
    fun fromMessageType(type: MessageType): String {
        return type.name
    }

    @TypeConverter
    fun toMessageType(value: String): MessageType {
        return try {
            MessageType.valueOf(value)
        } catch (e: Exception) {
            MessageType.TEXT
        }
    }

    // Nouveaux converters pour les réactions
    @TypeConverter
    fun fromMessageReactionList(reactions: List<MessageReaction>): String {
        return gson.toJson(reactions)
    }

    @TypeConverter
    fun toMessageReactionList(value: String): List<MessageReaction> {
        return if (value.isEmpty()) {
            emptyList()
        } else {
            try {
                val type = object : TypeToken<List<MessageReaction>>() {}.type
                gson.fromJson(value, type)
            } catch (e: Exception) {
                emptyList()
            }
        }
    }

    // Converter pour ReplyInfo
    @TypeConverter
    fun fromReplyInfo(replyInfo: ReplyInfo?): String? {
        return replyInfo?.let { gson.toJson(it) }
    }

    @TypeConverter
    fun toReplyInfo(value: String?): ReplyInfo? {
        return if (value.isNullOrEmpty()) {
            null
        } else {
            try {
                gson.fromJson(value, ReplyInfo::class.java)
            } catch (e: Exception) {
                null
            }
        }
    }
}
