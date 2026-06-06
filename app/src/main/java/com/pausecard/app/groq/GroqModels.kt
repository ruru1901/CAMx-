package com.pausecard.app.groq

import org.json.JSONArray
import org.json.JSONObject

data class GroqCard(
    val title: String,
    val body: String,
    val category: String
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("title", title)
        put("body", body)
        put("category", category)
    }

    companion object {
        fun fromJson(json: JSONObject): GroqCard? {
            return try {
                GroqCard(
                    title = json.getString("title"),
                    body = json.getString("body"),
                    category = json.getString("category")
                )
            } catch (e: Exception) {
                null
            }
        }

        fun fromJsonArray(jsonArray: JSONArray): List<GroqCard> {
            val cards = mutableListOf<GroqCard>()
            for (i in 0 until jsonArray.length()) {
                try {
                    val obj = jsonArray.getJSONObject(i)
                    fromJson(obj)?.let { cards.add(it) }
                } catch (_: Exception) {}
            }
            return cards
        }
    }
}

data class GroqRequest(
    val model: String = "llama3-8b-8192",
    val messages: List<GroqMessage>,
    val temperature: Double = 0.7,
    val maxTokens: Int = 2000
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("model", model)
        put("temperature", temperature)
        put("max_tokens", maxTokens)
        val msgsArray = JSONArray()
        messages.forEach { msg ->
            msgsArray.put(JSONObject().apply {
                put("role", msg.role)
                put("content", msg.content)
            })
        }
        put("messages", msgsArray)
    }
}

data class GroqMessage(
    val role: String,
    val content: String
)

data class GroqResponse(
    val cards: List<GroqCard>,
    val error: String? = null
)
