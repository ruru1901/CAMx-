package com.pausecard.app.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.pausecard.app.data.entity.CardEntity
import com.pausecard.app.data.entity.DislikedCardEntity
import com.pausecard.app.data.entity.InterceptedAppEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        CardEntity::class,
        InterceptedAppEntity::class,
        DislikedCardEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class PauseCardDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao
    abstract fun interceptedAppDao(): InterceptedAppDao
    abstract fun dislikedCardDao(): DislikedCardDao

    companion object {
        @Volatile
        private var INSTANCE: PauseCardDatabase? = null

        fun getInstance(context: Context): PauseCardDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(context: Context): PauseCardDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                PauseCardDatabase::class.java,
                "pausecard.db"
            )
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            getInstance(context).let { database ->
                                SeedData.seedCards(database.cardDao())
                                SeedData.seedApps(database.interceptedAppDao())
                            }
                        }
                    }
                })
                .fallbackToDestructiveMigration()
                .build()
        }
    }
}
