package com.pausecard.app.data.repository

import com.pausecard.app.data.db.CardDao
import com.pausecard.app.data.db.DislikedCardDao
import com.pausecard.app.data.entity.CardEntity
import com.pausecard.app.data.entity.DislikedCardEntity
import com.pausecard.app.util.CrashReporting
import java.util.concurrent.ConcurrentLinkedDeque

class CardRepository(
    private val cardDao: CardDao,
    private val dislikedCardDao: DislikedCardDao
) {

    private val recentCardIds = ConcurrentLinkedDeque<Long>()
    private val maxRecentHistory = 3

    suspend fun getNextCard(allowedCategories: List<String>? = null): CardEntity? {
        return try {
            val excludeIds = getExcludedIds()

            val card = if (!allowedCategories.isNullOrEmpty()) {
                cardDao.getWeightedRandomCardByCategory(allowedCategories.random(), excludeIds)
                    ?: cardDao.getWeightedRandomCard(excludeIds)
                    ?: cardDao.getRandomCard()
            } else {
                cardDao.getWeightedRandomCard(excludeIds)
                    ?: cardDao.getRandomCard()
            }

            card?.let {
                trackShownCard(it.id)
                cardDao.incrementShownCount(it.id)
            }

            card
        } catch (e: Exception) {
            CrashReporting.logError(TAG, "Error getting next card", e)
            cardDao.getRandomCard()
        }
    }

    private suspend fun getExcludedIds(): List<Long> {
        val dislikedIds = dislikedCardDao.getDislikedCardIds()
        val recentIds = recentCardIds.toList()
        return (dislikedIds + recentIds).distinct()
    }

    private fun trackShownCard(cardId: Long) {
        recentCardIds.addLast(cardId)
        while (recentCardIds.size > maxRecentHistory) {
            recentCardIds.pollFirst()
        }
    }

    suspend fun markDisliked(cardId: Long) {
        try {
            dislikedCardDao.insertDislike(
                DislikedCardEntity(cardId = cardId)
            )
        } catch (e: Exception) {
            CrashReporting.logError(TAG, "Error marking card as disliked", e)
        }
    }

    suspend fun getCardCount(): Int = cardDao.getCardCount()

    suspend fun getAllCards(): List<CardEntity> = cardDao.getAllCards()

    suspend fun insertCards(cards: List<CardEntity>) {
        cardDao.insertCards(cards)
    }

    companion object {
        private const val TAG = "CardRepository"
    }
}
