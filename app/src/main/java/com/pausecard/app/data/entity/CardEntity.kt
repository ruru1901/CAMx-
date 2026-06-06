package com.pausecard.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "cards",
    indices = [
        Index(value = ["category"]),
        Index(value = ["last_shown_at"]),
        Index(value = ["is_static"])
    ]
)
data class CardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val body: String,
    val category: String,
    @ColumnInfo(name = "shown_count")
    val shownCount: Int = 0,
    @ColumnInfo(name = "last_shown_at")
    val lastShownAt: Long = 0L,
    @ColumnInfo(name = "is_static")
    val isStatic: Boolean = true
)
