package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserEntity::class, PredictionEntity::class],
    version = 1,
    exportSchema = false
)
abstract class KidneyDatabase : RoomDatabase() {

    abstract fun kidneyDao(): KidneyDao

    companion object {
        @Volatile
        private var INSTANCE: KidneyDatabase? = null

        fun getDatabase(context: Context): KidneyDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KidneyDatabase::class.java,
                    "kidney_ai_database"
                )
                    .fallbackToDestructiveMigration(false)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
