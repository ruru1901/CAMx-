package com.pausecard.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pausecard.app.data.entity.InterceptedAppEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InterceptedAppDao {

    @Query("SELECT * FROM intercepted_apps ORDER BY app_label ASC")
    fun getAllApps(): Flow<List<InterceptedAppEntity>>

    @Query("SELECT * FROM intercepted_apps")
    suspend fun getAllAppsList(): List<InterceptedAppEntity>

    @Query("SELECT * FROM intercepted_apps WHERE is_enabled = 1")
    suspend fun getEnabledApps(): List<InterceptedAppEntity>

    @Query("SELECT * FROM intercepted_apps WHERE package_name = :packageName")
    suspend fun getAppByPackage(packageName: String): InterceptedAppEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: InterceptedAppEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<InterceptedAppEntity>)

    @Update
    suspend fun updateApp(app: InterceptedAppEntity)

    @Query("UPDATE intercepted_apps SET is_enabled = :enabled WHERE package_name = :packageName")
    suspend fun setAppEnabled(packageName: String, enabled: Boolean)

    @Query("SELECT COUNT(*) FROM intercepted_apps")
    suspend fun getAppCount(): Int
}
