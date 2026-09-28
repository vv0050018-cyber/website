package com.example.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prediction_history",
    foreignKeys = [
        ForeignKey(
            entity = UserEntity::class,
            parentColumns = ["id"],
            childColumns = ["userId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["userId"])]
)
data class PredictionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val userId: Long,
    val scanType: String,
    val imageName: String,
    val imageUri: String,
    val prediction: String,
    val svmPrediction: String,
    val dtPrediction: String,
    val confidence: Float?,
    val modelsAgree: Boolean,
    val meanIntensity: Float,
    val contrast: Float,
    val homogeneity: Float,
    val hyperdenseRatio: Float,
    val hypodenseRatio: Float,
    val modelVersion: String = "v1.0-svm-dt",
    val createdAt: Long = System.currentTimeMillis()
)
