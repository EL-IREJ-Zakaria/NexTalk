package com.example.nextalk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.nextalk.data.model.Call
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object pour les appels
 */
@Dao
interface CallDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCall(call: Call)

    @Update
    suspend fun updateCall(call: Call)

    @Query("SELECT * FROM calls WHERE id = :callId")
    suspend fun getCallById(callId: String): Call?

    @Query("SELECT * FROM calls WHERE conversationId = :conversationId ORDER BY timestamp DESC")
    fun getCallsByConversation(conversationId: String): Flow<List<Call>>

    @Query("SELECT * FROM calls WHERE callerId = :userId OR receiverId = :userId ORDER BY timestamp DESC")
    fun getCallsByUser(userId: String): Flow<List<Call>>

    @Query("DELETE FROM calls WHERE id = :callId")
    suspend fun deleteCall(callId: String)

    @Query("DELETE FROM calls WHERE conversationId = :conversationId")
    suspend fun deleteCallsByConversation(conversationId: String)

    @Query("SELECT * FROM calls ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentCalls(limit: Int = 20): Flow<List<Call>>

    @Query("SELECT * FROM calls WHERE status = 'MISSED' ORDER BY timestamp DESC")
    fun getMissedCalls(): Flow<List<Call>>

    @Query("DELETE FROM calls WHERE timestamp < :timestamp")
    suspend fun deleteCallsOlderThan(timestamp: Long)
}
