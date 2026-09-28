package com.example.data

import com.example.data.local.KidneyDao
import com.example.data.local.PredictionEntity
import com.example.data.local.UserEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom

class KidneyRepository(private val dao: KidneyDao) {

    // --- Authentication ---
    suspend fun registerUser(name: String, email: String, password: String):Result<Long> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        if (cleanEmail.isEmpty() || !cleanEmail.contains("@")) {
            return@withContext Result.failure(IllegalArgumentException("Invalid email format."))
        }
        if (password.length < 6) {
            return@withContext Result.failure(IllegalArgumentException("Password must be at least 6 characters."))
        }

        val existing = dao.getUserByEmail(cleanEmail)
        if (existing != null) {
            return@withContext Result.failure(IllegalArgumentException("An account with this email already exists."))
        }

        val salt = generateSalt()
        val hash = hashPassword(password, salt)
        val user = UserEntity(
            name = name.trim(),
            email = cleanEmail,
            passwordHash = hash,
            salt = salt,
            isAdmin = false
        )
        val userId = dao.insertUser(user)
        Result.success(userId)
    }

    suspend fun authenticateUser(email: String, password: String): Result<UserEntity> = withContext(Dispatchers.IO) {
        val cleanEmail = email.trim().lowercase()
        val user = dao.getUserByEmail(cleanEmail)
            ?: return@withContext Result.failure(IllegalArgumentException("Invalid email or password."))

        val computedHash = hashPassword(password, user.salt)
        if (computedHash == user.passwordHash) {
            Result.success(user)
        } else {
            Result.failure(IllegalArgumentException("Invalid email or password."))
        }
    }

    suspend fun seedDemoDoctorIfEmpty() = withContext(Dispatchers.IO) {
        val count = dao.getUserCount()
        if (count == 0) {
            val salt = generateSalt()
            val hash = hashPassword("Doctor123!", salt)
            val demoUser = UserEntity(
                name = "Dr. Sarah Jenkins",
                email = "doctor@hospital.org",
                passwordHash = hash,
                salt = salt,
                isAdmin = true
            )
            dao.insertUser(demoUser)
        }
    }

    // --- Predictions ---
    suspend fun savePrediction(prediction: PredictionEntity): Long = withContext(Dispatchers.IO) {
        dao.insertPrediction(prediction)
    }

    fun getHistory(userId: Long, filterClass: String = "ALL"): Flow<List<PredictionEntity>> {
        return if (filterClass == "ALL") {
            dao.getPredictionsByUser(userId)
        } else {
            dao.getPredictionsByUserAndClass(userId, filterClass)
        }
    }

    suspend fun deletePrediction(id: Long, userId: Long): Boolean = withContext(Dispatchers.IO) {
        dao.deletePrediction(id, userId) > 0
    }

    suspend fun getPredictionById(id: Long): PredictionEntity? = withContext(Dispatchers.IO) {
        dao.getPredictionById(id)
    }

    // --- Statistics ---
    fun getTotalScans(userId: Long): Flow<Int> = dao.getTotalScansCount(userId)
    fun getNormalCount(userId: Long): Flow<Int> = dao.getNormalCount(userId)
    fun getCystCount(userId: Long): Flow<Int> = dao.getCystCount(userId)
    fun getTumorCount(userId: Long): Flow<Int> = dao.getTumorCount(userId)
    fun getStoneCount(userId: Long): Flow<Int> = dao.getStoneCount(userId)
    fun getConcordantCount(userId: Long): Flow<Int> = dao.getConcordantCount(userId)

    // --- Cryptographic Utilities ---
    private fun generateSalt(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPassword(password: String, salt: String): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt.toByteArray())
        val digest = md.digest(password.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }
}
