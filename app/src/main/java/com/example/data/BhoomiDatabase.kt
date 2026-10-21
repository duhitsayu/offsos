package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        MeshMessageEntity::class,
        MeshGroupEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class BhoomiDatabase : RoomDatabase() {
    abstract fun dao(): BhoomiDao

    companion object {
        @Volatile
        private var INSTANCE: BhoomiDatabase? = null

        fun getDatabase(context: Context): BhoomiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    BhoomiDatabase::class.java,
                    "off_sos_mesh.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
