package com.pausecard.app.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.pausecard.app.data.entity.CardEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CardDao {

    @Query("SELECT * FROM cards ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomCard(): CardEntity?

    @Query("SELECT * FROM cards WHERE category = :category ORDER BY RANDOM() LIMIT 1")
    suspend fun getRandomCardByCategory(category: String): CardEntity?

    @Query("""
        SELECT * FROM cards
        WHERE id NOT IN (:excludeIds)
        ORDER BY shown_count ASC, last_shown_at ASC
        LIMIT 1
    """)
    suspend fun getWeightedRandomCard(excludeIds: List<Long>): CardEntity?

    @Query("""
        SELECT * FROM cards
        WHERE category = :category AND id NOT IN (:excludeIds)
        ORDER BY shown_count ASC, last_shown_at ASC
        LIMIT 1
    """)
    suspend fun getWeightedRandomCardByCategory(category: String, excludeIds: List<Long>): CardEntity?

    @Query("SELECT * FROM cards ORDER BY shown_count ASC")
    suspend fun getAllCards(): List<CardEntity>

    @Query("SELECT * FROM cards WHERE id = :id")
    suspend fun getCardById(id: Long): CardEntity?

    @Query("SELECT COUNT(*) FROM cards")
    suspend fun getCardCount(): Int

    @Query("SELECT COUNT(*) FROM cards WHERE category = :category")
    suspend fun getCardCountByCategory(category: String): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCard(card: CardEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCards(cards: List<CardEntity>)

    @Update
    suspend fun updateCard(card: CardEntity)

    @Query("UPDATE cards SET shown_count = shown_count + 1, last_shown_at = :now WHERE id = :id")
    suspend fun incrementShownCount(id: Long, now: Long = System.currentTimeMillis())

    @Query("DELETE FROM cards")
    suspend fun deleteAllCards()

    @Query("SELECT * FROM cards WHERE category IN (:categories) ORDER BY RANDOM() LIMIT :limit")
    suspend fun getCardsByCategories(categories: List<String>, limit: Int): List<CardEntity>
}
