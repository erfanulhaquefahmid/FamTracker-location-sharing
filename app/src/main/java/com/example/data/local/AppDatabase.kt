package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.local.dao.FamilyDao
import com.example.data.local.dao.LocationDao
import com.example.data.local.dao.UserDao
import com.example.data.local.entity.FamilyEntity
import com.example.data.local.entity.FamilyMemberEntity
import com.example.data.local.entity.LocationEventEntity
import com.example.data.local.entity.UserProfileEntity

@Database(
    entities = [
        LocationEventEntity::class,
        FamilyEntity::class,
        FamilyMemberEntity::class,
        UserProfileEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun locationDao(): LocationDao
    abstract fun familyDao(): FamilyDao
    abstract fun userDao(): UserDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "famtrack.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                .also { INSTANCE = it }
            }
        }
    }
}
