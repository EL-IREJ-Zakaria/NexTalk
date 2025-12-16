package com.example.nextalk.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.example.nextalk.data.model.Status
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object pour les statuts
 */
@Dao
interface StatusDao {

    @Insert
    suspend fun insertStatus(status: Status)

    @Update
    suspend fun updateStatus(status: Status)

    @Query("SELECT * FROM statuses WHERE id = :statusId")
    suspend fun getStatusById(statusId: String): Status?

    @Query("SELECT * FROM statuses WHERE userId = :userId AND expiresAt > :currentTime ORDER BY createdAt DESC")
    fun getStatusesByUser(userId: String, currentTime: Long = System.currentTimeMillis()): Flow<List<Status>>

    @Query("SELECT DISTINCT userId FROM statuses WHERE expiresAt > :currentTime ORDER BY createdAt DESC")
    fun getAllUsersWithStatuses(currentTime: Long = System.currentTimeMillis()): Flow<List<String>>

    @Query("SELECT * FROM statuses WHERE expiresAt > :currentTime ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentStatuses(limit: Int = 50, currentTime: Long = System.currentTimeMillis()): Flow<List<Status>>

    @Query("DELETE FROM statuses WHERE expiresAt < :currentTime")
    suspend fun deleteExpiredStatuses(currentTime: Long = System.currentTimeMillis())

    @Query("DELETE FROM statuses WHERE id = :statusId")
    suspend fun deleteStatus(statusId: String)

    @Query("DELETE FROM statuses WHERE userId = :userId")
    suspend fun deleteUserStatuses(userId: String)

    @Query("SELECT COUNT(*) FROM statuses WHERE userId = :userId AND expiresAt > :currentTime")
    fun getStatusCountByUser(userId: String, currentTime: Long = System.currentTimeMillis()): Flow<Int>
}
