package com.pausecard.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "intercepted_apps")
data class InterceptedAppEntity(
    @PrimaryKey
    @ColumnInfo(name = "package_name")
    val packageName: String,
    @ColumnInfo(name = "app_label")
    val appLabel: String,
    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true
)
