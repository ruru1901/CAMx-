package com.pausecard.app.data.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "disliked_cards",
    indices = [Index(value = ["card_id"])]
)
data class DislikedCardEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "card_id")
    val cardId: Long,
    @ColumnInfo(name = "disliked_at")
    val dislikedAt: Long = System.currentTimeMillis()
)
