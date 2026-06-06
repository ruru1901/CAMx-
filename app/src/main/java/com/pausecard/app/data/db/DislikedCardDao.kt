package com.pausecard.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.pausecard.app.data.entity.DislikedCardEntity

@Dao
interface DislikedCardDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDislike(dislike: DislikedCardEntity)

    @Query("SELECT card_id FROM disliked_cards")
    suspend fun getDislikedCardIds(): List<Long>

    @Query("SELECT COUNT(*) FROM disliked_cards WHERE card_id = :cardId")
    suspend fun getDislikeCount(cardId: Long): Int

    @Query("SELECT COUNT(*) FROM disliked_cards")
    suspend fun getDislikeCount(): Int
}
