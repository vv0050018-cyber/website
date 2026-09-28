package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface KidneyDao {

    // --- User Authentication ---
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertUser(user: UserEntity): Long

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE id = :userId LIMIT 1")
    suspend fun getUserById(userId: Long): UserEntity?

    @Query("SELECT COUNT(*) FROM users")
    suspend fun getUserCount(): Int

    // --- Predictions & History ---
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrediction(prediction: PredictionEntity): Long

    @Query("SELECT * FROM prediction_history WHERE userId = :userId ORDER BY createdAt DESC")
    fun getPredictionsByUser(userId: Long): Flow<List<PredictionEntity>>

    @Query("SELECT * FROM prediction_history WHERE userId = :userId AND UPPER(prediction) = UPPER(:className) ORDER BY createdAt DESC")
    fun getPredictionsByUserAndClass(userId: Long, className: String): Flow<List<PredictionEntity>>

    @Query("SELECT * FROM prediction_history WHERE id = :id LIMIT 1")
    suspend fun getPredictionById(id: Long): PredictionEntity?

    @Query("DELETE FROM prediction_history WHERE id = :id AND userId = :userId")
    suspend fun deletePrediction(id: Long, userId: Long): Int

    // --- Statistics for Dashboard ---
    @Query("SELECT COUNT(*) FROM prediction_history WHERE userId = :userId")
    fun getTotalScansCount(userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM prediction_history WHERE userId = :userId AND UPPER(prediction) = 'NORMAL'")
    fun getNormalCount(userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM prediction_history WHERE userId = :userId AND UPPER(prediction) = 'CYST'")
    fun getCystCount(userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM prediction_history WHERE userId = :userId AND UPPER(prediction) = 'TUMOR'")
    fun getTumorCount(userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM prediction_history WHERE userId = :userId AND UPPER(prediction) = 'STONE'")
    fun getStoneCount(userId: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM prediction_history WHERE userId = :userId AND modelsAgree = 1")
    fun getConcordantCount(userId: Long): Flow<Int>
}
