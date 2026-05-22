package com.example.stay.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.stay.data.local.dao.ChatDao
import com.example.stay.data.local.dao.ListingDao
import com.example.stay.data.local.dao.UserDao
import com.example.stay.data.local.entity.ChatMessage
import com.example.stay.data.local.entity.Favorite
import com.example.stay.data.local.entity.Listing
import com.example.stay.data.local.entity.User

@Database(entities = [User::class, Listing::class, ChatMessage::class, Favorite::class], version = 21, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userDao(): UserDao
    abstract fun listingDao(): ListingDao
    abstract fun chatDao(): ChatDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "stay_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
