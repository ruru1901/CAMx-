package com.pausecard.app.groq

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.pausecard.app.data.entity.CardEntity
import com.pausecard.app.util.CrashReporting
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import kotlin.math.min

class GroqCardGenerator(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val prefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                "pausecard_secure_prefs",
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            CrashReporting.logError(TAG, "Failed to create encrypted prefs, falling back", e)
            context.getSharedPreferences("pausecard_secure_prefs_fallback", Context.MODE_PRIVATE)
        }
    }

    fun getGroqKey(): String = prefs.getString(KEY_GROQ_API_KEY, "") ?: ""

    fun setGroqKey(key: String) {
        prefs.edit().putString(KEY_GROQ_API_KEY, key).apply()
    }

    fun getUserInterests(): String = prefs.getString(KEY_USER_INTERESTS, "") ?: ""

    fun setUserInterests(interests: String) {
        prefs.edit().putString(KEY_USER_INTERESTS, interests).apply()
    }

    fun getDailyCallCount(): Int = prefs.getInt(KEY_DAILY_CALL_COUNT, 0)

    fun getLastCallDate(): String = prefs.getString(KEY_LAST_CALL_DATE, "") ?: ""

    private fun incrementDailyCallCount() {
        val today = java.time.LocalDate.now().toString()
        if (getLastCallDate() != today) {
            prefs.edit()
                .putInt(KEY_DAILY_CALL_COUNT, 1)
                .putString(KEY_LAST_CALL_DATE, today)
                .apply()
        } else {
            prefs.edit().putInt(KEY_DAILY_CALL_COUNT, getDailyCallCount() + 1).apply()
        }
    }

    private fun canMakeApiCall(): Boolean {
        val key = getGroqKey()
        if (key.isBlank()) return false
        val today = java.time.LocalDate.now().toString()
        if (getLastCallDate() != today) return true
        return getDailyCallCount() < MAX_DAILY_CALLS
    }

    suspend fun generateCards(userInterests: String? = null): GroqResponse = withContext(Dispatchers.IO) {
        if (!canMakeApiCall()) {
            CrashReporting.logWarning(TAG, "Cannot make API call: rate limited or no key")
            return@withContext GroqResponse(emptyList(), "Rate limited or no API key")
        }

        val interests = userInterests ?: getUserInterests()
        val request = buildRequest(interests)

        var lastException: Exception? = null
        val maxRetries = 3
        var delayMs = 1000L

        repeat(maxRetries) { attempt ->
            try {
                incrementDailyCallCount()
                val response = client.newCall(request).execute()
                val body = response.body?.string() ?: ""

                if (!response.isSuccessful) {
                    CrashReporting.logWarning(TAG, "Groq API error ${response.code}: $body")
                    if (response.code == 429) {
                        Thread.sleep(delayMs)
                        delayMs *= 2
                        return@repeat
                    }
                    return@withContext GroqResponse(emptyList(), "API error: ${response.code}")
                }

                return@withContext parseResponse(body)
            } catch (e: Exception) {
                lastException = e
                CrashReporting.logError(TAG, "Groq API call attempt ${attempt + 1} failed", e)
                if (attempt < maxRetries - 1) {
                    Thread.sleep(delayMs)
                    delayMs *= 2
                }
            }
        }

        GroqResponse(emptyList(), lastException?.message ?: "Unknown error")
    }

    private fun buildRequest(interests: String): Request {
        val systemPrompt = buildSystemPrompt()
        val userPrompt = buildUserPrompt(interests)

        val groqRequest = GroqRequest(
            messages = listOf(
                GroqMessage("system", systemPrompt),
                GroqMessage("user", userPrompt)
            )
        )

        return Request.Builder()
            .url(API_ENDPOINT)
            .addHeader("Authorization", "Bearer ${getGroqKey()}")
            .addHeader("Content-Type", "application/json")
            .post(groqRequest.toJson().toString().toRequestBody("application/json".toMediaType()))
            .build()
    }

    private fun buildSystemPrompt(): String = """You generate micro-learning cards for a mobile app. Each card has:
- title: max 8 words, punchy and specific
- body: exactly 4 sentences
- category: one of ECE, LLM, Android, Automation, DevTools, General, Cybersecurity, Programming, Tamil

Card style rules:
- Write like a senior dev casually explaining to a curious junior
- Never write textbook definitions or Wikipedia intros
- Every card must answer at least one of these three questions:
  1. What is it (in plain words)?
  2. Why does it matter or when would you use it?
  3. How do you use it — one concrete example or command
- Practical over theoretical, always
- Casual tone, no fluff, no hype words like 'powerful' or 'robust'

Return ONLY a JSON array. No markdown, no backticks, no extra text.
Format: [{"title": "", "body": "", "category": ""}]"""

    private fun buildUserPrompt(interests: String): String {
        val profile = if (interests.isNullOrBlank()) {
            DEFAULT_PROFILE
        } else {
            interests
        }

        return """Generate 10 cards based on these interests: $profile.

Return ONLY a JSON array. No markdown, no backticks, no extra text.
Format: [{"title": "", "body": "", "category": ""}]"""
    }

    private fun parseResponse(responseBody: String): GroqResponse {
        val strategies: List<(String) -> List<GroqCard>> = listOf(
            { body -> parseFromMarkdownJson(body) },
            { body -> parseFromCodeBlock(body) },
            { body -> parseFromBrackets(body) },
            { body -> parseDirectly(body) }
        )

        for (strategy in strategies) {
            try {
                val cards = strategy(responseBody)
                if (cards.isNotEmpty()) {
                    val validatedCards = cards.mapNotNull { card ->
                        val result = CardValidator.validate(card.title, card.body, card.category)
                        if (result.isValid) card else null
                    }
                    if (validatedCards.isNotEmpty()) {
                        return GroqResponse(validatedCards)
                    }
                }
            } catch (_: Exception) {}
        }

        CrashReporting.logError(TAG, "All JSON parsing strategies failed for response", null)
        return GroqResponse(emptyList(), "Failed to parse response")
    }

    private fun parseFromMarkdownJson(body: String): List<GroqCard> {
        val regex = Regex("```json\\s*\\n(.*?)\\n\\s*```", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(body) ?: return emptyList()
        return parseJsonArray(match.groupValues[1])
    }

    private fun parseFromCodeBlock(body: String): List<GroqCard> {
        val regex = Regex("```\\s*\\n(.*?)\\n\\s*```", RegexOption.DOT_MATCHES_ALL)
        val match = regex.find(body) ?: return emptyList()
        return parseJsonArray(match.groupValues[1])
    }

    private fun parseFromBrackets(body: String): List<GroqCard> {
        val firstBracket = body.indexOf('[')
        val lastBracket = body.lastIndexOf(']')
        if (firstBracket == -1 || lastBracket == -1 || lastBracket <= firstBracket) return emptyList()
        return parseJsonArray(body.substring(firstBracket, lastBracket + 1))
    }

    private fun parseDirectly(body: String): List<GroqCard> {
        return parseJsonArray(body.trim())
    }

    private fun parseJsonArray(jsonString: String): List<GroqCard> {
        val jsonArray = JSONArray(jsonString.trim())
        return GroqCard.fromJsonArray(jsonArray)
    }

    companion object {
        private const val TAG = "GroqCardGenerator"
        private const val API_ENDPOINT = "https://api.groq.com/openai/v1/chat/completions"
        private const val MAX_DAILY_CALLS = 20
        private const val KEY_GROQ_API_KEY = "groq_api_key"
        private const val KEY_USER_INTERESTS = "user_interests"
        private const val KEY_DAILY_CALL_COUNT = "daily_call_count"
        private const val KEY_LAST_CALL_DATE = "last_call_date"

        private const val DEFAULT_PROFILE = """Techie who wants broad awareness, not deep mastery. Topics:
LLMs and how to use them practically (prompting, APIs, local models),
Android development basics (Jetpack Compose, ADB, permissions),
automation tools and scripts (Tasker, shell, GitHub Actions),
new tools in the dev/AI space (CLI tools, AI coding assistants, dev workflows, what is actually worth knowing),
ECE fundamentals (signals, op-amps, digital logic, communications).
The user does not want to master any of these — they want to know what each thing is, why it exists, and how to use it at a basic level."""
    }
}
